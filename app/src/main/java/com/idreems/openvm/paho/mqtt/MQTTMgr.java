package com.idreems.openvm.paho.mqtt;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.MyApplication;
import com.idreems.openvm.R;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.protocols.ProtocolPool;
import com.idreems.openvm.protocols.clouds.CloudConsts;
import com.idreems.openvm.protocols.clouds.DeliverHandler;
import com.idreems.openvm.protocols.clouds.GetLatestSaleLogHandler;
import com.idreems.openvm.protocols.clouds.GetLogHandler;
import com.idreems.openvm.protocols.clouds.GetMachineVariablesHandler;
import com.idreems.openvm.protocols.clouds.GetTimeHandler;
import com.idreems.openvm.protocols.clouds.ReplyTimeHandler;
import com.idreems.openvm.protocols.clouds.SetConfigHandler;
import com.idreems.openvm.protocols.websocket.WebConsts;
import com.idreems.openvm.utils.DeviceUtils;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.TimeUtil;
import com.idreems.openvm.utils.ToastUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ramonqlee on 8/5/16.
 */
public class MQTTMgr {
    private static final int MQTT_KEEP_ALIVE_INTERVAL = 5;//5S
    private static final int MAX_SN_CACHED = 10000;
    private static final int MQTT_RECONNECT_INTERVAL_MS = 60 * 1000;//10S
    private static final int MQTT_REBOOT_AFTER_RECONNECT_COUNT = 10;//10//重试几次后，如果失败，则重启机器
    private static final int MQTT_RECONNECT_SPAN_INTERVAL = MQTT_REBOOT_AFTER_RECONNECT_COUNT * MQTT_RECONNECT_INTERVAL_MS;
    private static final int VC_REBOOT_PER_DAY = Integer.MAX_VALUE;// 工控每天重启的最大次数
    private static final String MQTT_REBOOT_INIT_COUNT = "1";

    private static MQTTMgr sMQTTWrapper = new MQTTMgr();
    private MqttAndroidClient mqttAndroidClient;
    private MqttConnectOptions mMQTTConnectOptions;

    private ProtocolPool mMqttProtocolHandlerPool;
    private ArrayList<String> mSNCachedList = new ArrayList<String>();
    private Context mContext;
    private Timer mMQTTReconnectTimer;
    private Timer mSyncTimeTimer;
    private long mLastRetryTimeInMs;
    private int mRetryCount;

    public static MQTTMgr sharedInstance(Context context) {
        sMQTTWrapper.mContext = context.getApplicationContext();
        return sMQTTWrapper;
    }

    /**
     * 自己设定重连逻辑
     */
    public void startMqttConnectionWithCustomReconnect() {
        if (null != mMQTTReconnectTimer) {
            mMQTTReconnectTimer.cancel();
            mMQTTReconnectTimer = null;
        }

        mMQTTReconnectTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (null != mqttAndroidClient && mqttAndroidClient.isConnected()) {
                    //重置重试变量
                    mLastRetryTimeInMs = 0;
                    mRetryCount = 0;
                    return;
                }

                final Config config = Config.sharedInstance(mContext);
                // 如果没有配置点位id，则不重启
                if (TextUtils.isEmpty(config.getValue(Config.NODE_ID))) {
                    LogUtil.d(Consts.LOG_TAG, "node_id not set");
                    return;
                }

                // 如果连续N次没有连接成功，则重启机器
                // 首先查看上次重试的时间，如果距离当前时间在认定的时间范围内，则认为是连续的一次尝试
                if (0 == mLastRetryTimeInMs || Math.abs(System.currentTimeMillis() - mLastRetryTimeInMs) <= MQTT_RECONNECT_SPAN_INTERVAL) {
                    // 更新重试变量
                    mLastRetryTimeInMs = System.currentTimeMillis();
                    mRetryCount++;

                    // 达到次数了，重启机器吧
                    if (mRetryCount > MQTT_REBOOT_AFTER_RECONNECT_COUNT) {
                        // 每天限定3次，如果超过了，则今天不再重启
                        String date = config.getValue(Config.REBOOT_DATE_KEY);
                        try {
                            // 如果没设置日期或者日期不是同一天，则进行重置
                            long time = System.currentTimeMillis();
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                            String today = format.format(new Date(time));
                            if (TextUtils.isEmpty(date) || !TextUtils.equals(today, date)) {
                                config.saveValue(Config.REBOOT_DATE_KEY, today);
                                config.saveValue(Config.REBOOT_COUNT_KEY, MQTT_REBOOT_INIT_COUNT);
                                date = today;
                            }

                            String rebootCntStr = config.getValue(Config.REBOOT_COUNT_KEY);
                            // 是否同一天,并且达到了上限制
                            int rebootCnt = Integer.valueOf(MQTT_REBOOT_INIT_COUNT);
                            if (!TextUtils.isEmpty(rebootCntStr) && TextUtils.isDigitsOnly(rebootCntStr)) {
                                rebootCnt = Integer.valueOf(rebootCntStr);
                            }

                            // 如果没设置过或者非法，则开始计数;否则，则将重启次数+1
                            if (TextUtils.isEmpty(rebootCntStr) || !TextUtils.isDigitsOnly(rebootCntStr)) {
                                rebootCnt = Integer.valueOf(MQTT_REBOOT_INIT_COUNT);
                                config.saveValue(Config.REBOOT_COUNT_KEY, MQTT_REBOOT_INIT_COUNT);
                            }

                            // 当天是否超过了启动次数
                            LogUtil.d(Consts.LOG_TAG_MQTT, "date = " + date + " rebootCnt = " + rebootCnt);
                            if (rebootCnt <= VC_REBOOT_PER_DAY) {
                                rebootCnt++;
                                config.saveValue(Config.REBOOT_COUNT_KEY, String.valueOf(rebootCnt));

                                LogUtil.d(Consts.LOG_TAG_MQTT, "reboot now after fail to connect");
                                DeviceUtils.reboot(mContext);
                            } else {
                                LogUtil.d(Consts.LOG_TAG_MQTT, "keep reconnecting after fail to connect");
                                mLastRetryTimeInMs = 0;
                                mRetryCount = 0;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    //不是连续的重试，需要重置变量,防止出现间断的重试，判定为连续的重试的情况
                    mLastRetryTimeInMs = 0;
                    mRetryCount = 0;
                }
                startMqttConnection();
            }
        };
        mMQTTReconnectTimer.schedule(task, 0, MQTT_RECONNECT_INTERVAL_MS);
    }

    private void startMqttConnection() {
        Config config = Config.sharedInstance(MyApplication.getContext());
        final String serverUri = Consts.getBrokerUrl();
        String clientId = config.getValue(Config.NODE_ID);
        String password = config.getValue(Config.PASSWORD);
        //是否已经配置
        if (TextUtils.isEmpty(clientId) || TextUtils.isEmpty(password)) {
            addToHistory("client or password is null ");
            return;
        }

        if (null == mqttAndroidClient) {
            mqttAndroidClient = new MqttAndroidClient(mContext, serverUri, clientId);
        }
        MqttCallbackExtended callbackExtended = new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                startSyncServerTimeLooper();
                addToHistory("Connected to: " + serverURI + " reconnect = " + reconnect);
            }

            @Override
            public void connectionLost(Throwable cause) {
                addToHistory("The Connection was lost,cause = " + (null == cause ? "" : cause.toString()));
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                addToHistory("Incoming message: " + topic + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                if (null != token) {
                    try {
                        MqttMessage msg = token.getMessage();
                        addToHistory("deliveryComplete msg = " + msg.toString());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    addToHistory("deliveryComplete with null token");
                }
            }
        };

        mqttAndroidClient.setCallback(callbackExtended);

        if (null == mMQTTConnectOptions) {
            mMQTTConnectOptions = new MqttConnectOptions();
        }

        mMQTTConnectOptions.setAutomaticReconnect(false);
        mMQTTConnectOptions.setCleanSession(false);
        mMQTTConnectOptions.setKeepAliveInterval(MQTT_KEEP_ALIVE_INTERVAL);
        mMQTTConnectOptions.setUserName(clientId);
        mMQTTConnectOptions.setMaxInflight(1024);
        int timeout = MQTT_RECONNECT_INTERVAL_MS / 1000;
        if (timeout <= 0) {
            timeout = 30;
        }
        mMQTTConnectOptions.setConnectionTimeout(timeout);
        mMQTTConnectOptions.setPassword(password.toCharArray());

        try {
            addToHistory("Connecting to " + serverUri);
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(1024);
                    disconnectedBufferOptions.setPersistBuffer(true);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();

                    if (!Consts.PRODUCTION_ON && Consts.TEST_CASE_ENABLED) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                testSyncDeliverHandler();
                            }
                        }, 5 * 1000);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to connect to: " + serverUri + " exception = " + (null == exception ?
                            "" : exception.toString()));
                    ToastUtils.show(R.string.invalid_password);
                }
            };

            mqttAndroidClient.connect(mMQTTConnectOptions, null, listener);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void subscribeToTopic() {
        try {
            Config config = Config.sharedInstance(mContext);
            String nodeId = config.getValue(Config.NODE_ID);
            if (TextUtils.isEmpty(nodeId)) {
                addToHistory("nodeId is null");
                return;
            }

            String subscriptionTopic = String.format("%s/%s", nodeId, "#");
            LogUtil.e(Consts.LOG_TAG_MQTT, "subscribeTopic = " + subscriptionTopic);
            mqttAndroidClient.subscribe(subscriptionTopic, 2, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived,handle it
                    try {
                        String payload = new String(message.getPayload());
                        addToHistory("Message: " + topic + " : " + payload);
                        handlePacket(topic, payload);
                    } catch (Exception ex) {
                        LogUtil.e(Consts.LOG_TAG_MQTT, "messageArrived = " + ex.toString());
                        ex.printStackTrace();
                    }
                }
            });

        } catch (MqttException ex) {
            LogUtil.d("Exception in subscribe");
            ex.printStackTrace();
        }
    }

    public void publishMessage(String publishTopic, String publishMessage) {
        if (null == mqttAndroidClient) {
            addToHistory("mqtt not init yet");
            return;
        }
        if (!mqttAndroidClient.isConnected()) {
            addToHistory("mqtt disconnected,drop publishMessage = " + publishTopic + " " + publishMessage);
            return;
        }
        try {
            MqttMessage message = new MqttMessage();
            message.setQos(2);
            message.setRetained(false);
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(publishTopic, message);
            addToHistory("Message Published topic=" + publishTopic + " message=" + publishMessage);

            if (!mqttAndroidClient.isConnected()) {
                addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            addToHistory("Error Publishing: " + e.getMessage());
            e.printStackTrace();
            LogUtil.d(Consts.HANDLER_TAG, e.getMessage());
        }
    }

    private void addToHistory(String mainText) {
        LogUtil.d(Consts.LOG_TAG_MQTT, "addToHistory = " + mainText);
    }

    private synchronized void handlePacket(String topic, String payload) {
        initProtocolPool();
        // 在此处理协议
        // 提取node_id和topic
        try {
            String[] splits = topic.split("/");
            if (2 != splits.length)//目前仅仅支持2级topic
            {
                // TODO 待添加umeng事件
                Log.e(Consts.LOG_TAG_MQTT, "topic error :" + topic);
                return;
            }

            String nodeID = Config.sharedInstance(mContext).getValue(Config.NODE_ID);
            if (!TextUtils.equals(nodeID, splits[0])) {
                // TODO 待添加umeng事件
                Log.e(Consts.LOG_TAG_MQTT, "nodeid error :" + topic);
                return;
            }

            // 增加sn去重，如果已经收到过，则下一次直接丢弃，防止重复操作(仅仅保留最近10次的)
            JSONObject jsonObject = new JSONObject(payload);
            JSONObject contentJson = JsonUtils.getJsonObject(jsonObject, CloudConsts.CONTENT);
            String sn = JsonUtils.getString(contentJson, CloudConsts.SN);
            if (TextUtils.isEmpty(sn)) {
                LogUtil.d("no sn,ignore");
                return;
            }

            if (mSNCachedList.contains(sn)) {
                LogUtil.d(Consts.LOG_TAG_MQTT, "ignore dup sn =" + sn + " topic= " + topic);
                return;
            }

            if (mSNCachedList.size() >= MAX_SN_CACHED) {
                String removedItem = mSNCachedList.remove(0);
                LogUtil.d(Consts.LOG_TAG_MQTT, "remove check sn =" + removedItem);
            }
            mSNCachedList.add(sn);

            JSONObject object = new JSONObject();
            object.put(CloudConsts.TOPIC, splits[1]);
            object.put(CloudConsts.PAYLOAD, payload);
            if (null != mMqttProtocolHandlerPool) {
                mMqttProtocolHandlerPool.run(object);
            }
        } catch (Exception ex) {
            LogUtil.d(Consts.LOG_TAG_MQTT, "handlePacket exception =" + ex.toString());
            ex.printStackTrace();
        }
    }

    private void startSyncServerTimeLooper() {
        try {
            // 建立连接后，尝试发送同步时间指令，需要定时发送？
            if (null != mSyncTimeTimer) {
                return;
            }
            mSyncTimeTimer = new Timer();
            TimerTask task = new TimerTask() {
                private void sendSyncTimeCmd() {
                    if (null == mqttAndroidClient || !mqttAndroidClient.isConnected()) {
                        return;
                    }
                    try {
                        // test get_time publish
                        JSONObject msg = new JSONObject();
                        Config config = Config.sharedInstance(mContext);
                        String nodeId = config.getValue(Config.NODE_ID);
                        final String topic = String.format("%s/%s", nodeId, GetTimeHandler.MY_TOPIC);
                        msg.put(CloudConsts.TIMESTAMP, TimeUtil.getCheckedCurrentTimeInMills(mContext) / 1000);
                        publishMessage(topic, msg.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void run() {
                    sendSyncTimeCmd();
                }
            };
            mSyncTimeTimer.schedule(task, 0, Consts.SYNC_TIME_INTERVAL);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void testSyncDeliverHandler() {
        // 测试出货并发
        for (int i = 0; i < 5; i++) {
            final int order = 100 + i;
            final int location = i % 3 + 1;
            final int seq = i % 3;
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
            Config config = Config.sharedInstance(mContext);
            String nodeId = config.getValue(Config.NODE_ID);
            String topic = nodeId + "/deliver";
            try {
                JSONObject payloadJson = new JSONObject();
                JSONObject contentJson = new JSONObject();
                payloadJson.put(CloudConsts.CONTENT, contentJson);
                payloadJson.put(CloudConsts.TIMESTAMP, String.valueOf(TimeUtil.getCheckedCurrentTimeInMills(mContext) / 1000));

                contentJson.put(CloudConsts.DEVICE_SEQ, String.valueOf(seq));
                contentJson.put(CloudConsts.LOCATION, String.valueOf(location));//1-3

                contentJson.put(CloudConsts.SN, String.valueOf(order));
                contentJson.put(CloudConsts.AMOUNT, "1");
                contentJson.put(CloudConsts.ONLINE_ORDER_ID, String.valueOf(Math.abs(order)));
                final long expired = System.currentTimeMillis() + 60 * 1000;
                contentJson.put(WebConsts.CONST_EXPIRE, String.valueOf(expired / 1000));

                String payload = payloadJson.toString();
//                        handlePacket(topic, payload);
                publishMessage(topic, payload);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
//                }
//            }).start();
        }
    }

    private DeliverHandler mDeliverHandler;

    private void initProtocolPool() {
        // 在此初始化协议处理池
        if (null == mMqttProtocolHandlerPool) {
            mMqttProtocolHandlerPool = new ProtocolPool();
        }
        if (mMqttProtocolHandlerPool.size() > 0) {
            return;
        }

        // TODO 待添加更多指令处理
        mMqttProtocolHandlerPool.add(new ReplyTimeHandler(mContext));
        mMqttProtocolHandlerPool.add(new SetConfigHandler(mContext));
        if (null == mDeliverHandler) {
            mDeliverHandler = new DeliverHandler(mContext);
        }
        mMqttProtocolHandlerPool.add(mDeliverHandler);
        // 暂时没用，注释掉
//        mMqttProtocolHandlerPool.add(new LightOnHandler(mContext));
//        mMqttProtocolHandlerPool.add(new LightOffHandler(mContext));
        mMqttProtocolHandlerPool.add(new GetLogHandler(mContext));
        mMqttProtocolHandlerPool.add(new GetMachineVariablesHandler(mContext));
        mMqttProtocolHandlerPool.add(new GetLatestSaleLogHandler(mContext));
    }

    // 正在出货的size
    public int getDeliveringSize() {
        return (null == mDeliverHandler) ? 0 : mDeliverHandler.getDeliveringSize();
    }
}
