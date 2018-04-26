package com.idreems.openvm.protocols.clouds;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.protocols.websocket.WebConsts;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.TimeUtil;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android_serialport_api.vmc.AIGashaponMachine.Location;
import android_serialport_api.vmc.AIGashaponMachine.LockerReport;
import android_serialport_api.vmc.AIGashaponMachine.StatusReport;
import android_serialport_api.vmc.AIGashaponMachine.utils.AIGashponManager;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class DeliverHandler extends CloudBaseHandler {
    private static final String TAG = "DeliverHandler";
    public static final String MY_TOPIC = "deliver";

    /**
     * 支付方式
     */
    private static final String PAY_ONLINE = "online";
    private static final String PAY_CASH = "cash";
    private static final String PAY_CARD = "card";

    private static final String TIMEOUT_COUNT_KEY = "timeout_count";//超时的次数，如果此次数为0，则说明超时了
    private static final short MIN_TIMEOUT_4_LOCK_IN_SEC = 1 * 30;//锁最小打开的时间
    private static final short MAX_TIMEOUT_4_LOCK_IN_SEC = 2 * 60;//缺省锁打开的时间

    // 对于超时的订单，超时后，上传数据给服务器
    private static final long PAYOUT_TIMEOUT_CHECK_INTERVAL_IN_MILLS = 5 * 1000;
    private static final long MACHINE_COMMUNICATION_TIMEOUT_INTERVAL_IN_MILLS = 2 * 60 * 1000;//检查超时的时间，防止出现检查状态时，没返回，误认为没扭动的情况
    private Timer mTimoutTimer;

    // FIXME 需要考虑持久化的问题
    private static Vector<Object> mOrderVectors = new Vector<>();
    private static Map<String, String> mBusyMap = new HashMap<>();//是否在占用的记录

    public DeliverHandler(Context context) {
        super(context);
    }

    // 正在出货的size
    public int getDeliveringSize() {
        return mOrderVectors.size();
    }

    @Override
    public String name() {
        return MY_TOPIC;
    }

    protected synchronized boolean handleContent(JSONObject content) {
        LogUtil.d(Consts.HANDLER_TAG, "DeliverHandler enter handleContent = " + (null == content ? "" : content.toString()));
        if (null == content) {
            return false;
        }
        try {
            startTimeoutLoop();

            Config config = Config.sharedInstance(mContext);
            String devicePath = config.getValue(Config.PC_DEVICE);
            String baudRateStr = config.getValue(Config.PC_BAUDE);
            if (TextUtils.isEmpty(devicePath) || TextUtils.isEmpty(baudRateStr) || !TextUtils.isDigitsOnly(baudRateStr)) {
                LogUtil.e(Consts.HANDLER_TAG, "illegal parameter for devicePath=" + devicePath + " baudRate=" + baudRateStr);
                return false;
            }

            int baudRate = Integer.decode(baudRateStr);
            final AIGashponManager wrapper = AIGashponManager.sharedInstance(devicePath, baudRate);

            final String device_seq = JsonUtils.getString(content, CloudConsts.DEVICE_SEQ);
            final String location = JsonUtils.getString(content, CloudConsts.LOCATION);
            if (TextUtils.isEmpty(device_seq) || TextUtils.isEmpty(location) || !TextUtils.isDigitsOnly(device_seq) || !TextUtils.isDigitsOnly(location)) {
                LogUtil.e(Consts.HANDLER_TAG, "device_seq or location not valid");
                return false;
            }

            byte addressOnline = (byte) (Byte.valueOf(device_seq) - Location.BUS_ADDRESS_OFFSET);// 地址被统一了
            addressOnline = (addressOnline > Location.MAX_BUS_ADDRESS) ? Location.MAX_BUS_ADDRESS : addressOnline;
            addressOnline = (addressOnline < Location.MIN_BUS_ADDRESS) ? Location.MIN_BUS_ADDRESS : addressOnline;
            final byte address = addressOnline;
            final byte groupNo = Byte.valueOf(location);

            // 如果正在出币中，直接返回出货结果；否则，尝试出币，通知收到出币指令，初步完成后，上传出币结果
            // 自己保留一个状态，不用机器的，因为机器可能出故障或者某种认为原因，导致不返回结果
            if (mBusyMap.containsKey(ADDRESS_GROUP_UID(device_seq, location))) {
                handleBusyOperation(content);
                return true;
            }

            String mqttSN = JsonUtils.getString(content, CloudConsts.SN);
            // 更新最新一次出货日志记录
            config.saveValue(Config.LATEST_DELIVER_LOG, mqttSN);
            // 待解析其他出货参数，并通知VM出货
            // TODO同时，向售货机发送出货指令，并在完成后，返回云端出货结果(需要支持队列操作)
            final String orderId = JsonUtils.getString(content, CloudConsts.ONLINE_ORDER_ID);
            final Map<String, String> deliverMap = new HashMap<String, String>();
            deliverMap.put(CloudConsts.SN, mqttSN);

            final Map<String, String> saleLogMap = new HashMap<String, String>();
            saleLogMap.put(CloudConsts.SN, mqttSN);
            saleLogMap.put(CloudConsts.DEVICE_SEQ, device_seq);
            saleLogMap.put(CloudConsts.VM_ORDER_ID, orderId);
            saleLogMap.put(CloudConsts.ONLINE_ORDER_ID, orderId);
            saleLogMap.put(CloudConsts.DEVICE_ORDER_ID, orderId);

            //FIXME待添加
            saleLogMap.put(CloudConsts.SP_ID, "");
            saleLogMap.put(CloudConsts.LOCATION, location);
            saleLogMap.put(CloudConsts.PAYER, PAY_ONLINE);
            saleLogMap.put(CloudConsts.PAID_AMOUNT, String.valueOf(0));

            long currentTimeInSec = TimeUtil.getCheckedCurrentTimeInMills(mContext) / 1000;
            saleLogMap.put(CloudConsts.CTS, String.valueOf(currentTimeInSec));
            // 超时时间
            final String expired = JsonUtils.getString(content, WebConsts.CONST_EXPIRE);
            long expiredInSec = 0;
            try {
                expiredInSec = Long.valueOf(expired);
            } catch (Exception ex) {
                expiredInSec = mTimestampInSec + MAX_TIMEOUT_4_LOCK_IN_SEC * 1000 / 2;
                ex.printStackTrace();
            }

            short timeoutInSec = (short) Math.abs(expiredInSec - mTimestampInSec);
            // 限制最大超时和最小超时
            timeoutInSec = (timeoutInSec > MAX_TIMEOUT_4_LOCK_IN_SEC) ? MAX_TIMEOUT_4_LOCK_IN_SEC : timeoutInSec;
            timeoutInSec = (timeoutInSec < MIN_TIMEOUT_4_LOCK_IN_SEC) ? MIN_TIMEOUT_4_LOCK_IN_SEC : timeoutInSec;

            saleLogMap.put(WebConsts.CONST_EXPIRE, String.valueOf(currentTimeInSec + timeoutInSec));
            saleLogMap.put(CloudConsts.LAST_CHECK_TIME, String.valueOf(TimeUtil.getLastCheckTimeInMs() / 1000));

            // 设置超时的次数，每次计数器减一，直到0就超时了:系统开锁后等待的时间+机器返回数据的最大超时时间
            final long cnt = (timeoutInSec * 1000 + MACHINE_COMMUNICATION_TIMEOUT_INTERVAL_IN_MILLS) / PAYOUT_TIMEOUT_CHECK_INTERVAL_IN_MILLS;
            saleLogMap.put(TIMEOUT_COUNT_KEY, String.valueOf(cnt));
            saleLogMap.put(CloudConsts.STATE,String.valueOf(CloudReplyBaseHandler.ELECTRIC_LOCK_OPEN_FAIL));//初始设置为锁没打开

            LogUtil.d(Consts.HANDLER_TAG, "orderId = " + orderId + " expiredInSec = " + expiredInSec + " mTimestampInSec = " + mTimestampInSec + " TIMEOUT_COUNT_KEY = " + cnt);

            // 指令如果超时了，则直接返回指令到达；上传出货日志
            final long currentTime = TimeUtil.getCheckedCurrentTimeInMills(mContext) / 1000;
            if (expiredInSec > 0 && currentTime > expiredInSec) {
                LogUtil.d("DeliverTest", "expired when payout comes");
                MqttReplyHandlerMgr.replyWith(mContext, ReplyDeliverHandler.MY_TOPIC, deliverMap);

                LogUtil.d(TAG, "UploadTimeoutSale " + "sn = " + saleLogMap.get(CloudConsts.SN) + " orderId = " + orderId);
                final UploadGashaponDeliverResultHandler uploadGashaponDeliverResultHandler = new UploadGashaponDeliverResultHandler(mContext);
                uploadGashaponDeliverResultHandler.setMap(deliverMap);
                uploadGashaponDeliverResultHandler.send(String.valueOf(CloudReplyBaseHandler.TIMEOUT));
                LogUtil.d(Consts.HANDLER_TAG, "UploadGashaponDeliverResultHandler replyWith");

                // 上传销售日志
                final UploadSaleLogHandler saleLogHandler = new UploadSaleLogHandler(mContext);
                saleLogHandler.setMap(saleLogMap);
                saleLogHandler.send(String.valueOf(CloudReplyBaseHandler.TIMEOUT));
                return true;
            }
            // 时间早的在后面
            addPayoutMap(saleLogMap);
            wrapper.addLockListener(new AIGashponManager.LockerReportListener() {
                @Override
                public void onCall(LockerReport report) {
                    if (null == report) {
                        return;
                    }

                    if (LockerReport.OPEN == report.getType()) {
                        // TODO 开锁成功了
                        LogUtil.d(Consts.HANDLER_TAG, "开锁成功");
                        saleLogMap.put(CloudConsts.STATE,String.valueOf(CloudReplyBaseHandler.SUCCESS));//设置为锁打开
                    }
                }
            });
            wrapper.addStatusListener(new AIGashponManager.StatusReportListener() {
                @Override
                public void onCall(StatusReport report) {
                    // 是否同一个扭蛋机
                    if (null == report) {
                        return;
                    }

                    boolean isLockOpen = report.isLockOpen(groupNo);
                    boolean isRotated = report.isRotated(groupNo);
                    //  增加是否有障碍物状态
                    boolean isCheckOn = report.isCheckingOn(groupNo);//关闭是有障碍物，打开是无障碍物
                    LogUtil.d(Consts.HANDLER_TAG, "ReplyDeliverHandler address =" + address + " statusReport address = " + report.getBusAddress() + " groupNo =" + groupNo + " isCheckOn =" + isCheckOn + " isLockOpen=" + isLockOpen + " isRotated = " + isRotated);

                    // 更新是否有障碍物的状态
                    if (address == report.getBusAddress()) {
                        saleLogMap.put(CloudConsts.VM_S2STATE, isCheckOn ? "1" : "0");
                    }

                    if (address != report.getBusAddress() || isLockOpen || !isRotated) {
                        return;
                    }
                    wrapper.removeStatusListener(this);

                    String lockOpenState = saleLogMap.get(CloudConsts.STATE);//锁是否打开成功

                    // 上传出货日志
                    deliverMap.put(CloudConsts.AMOUNT, String.valueOf(1));
                    final UploadGashaponDeliverResultHandler uploadGashaponDeliverResultHandler = new UploadGashaponDeliverResultHandler(mContext);
                    uploadGashaponDeliverResultHandler.setMap(deliverMap);
                    uploadGashaponDeliverResultHandler.send(lockOpenState);
                    LogUtil.d(Consts.HANDLER_TAG, "UploadGashaponDeliverResultHandler replyWith");

                    // 上传销售日志
                    final UploadSaleLogHandler saleLogHandler = new UploadSaleLogHandler(mContext);
                    saleLogHandler.setMap(saleLogMap);
                    saleLogHandler.send(lockOpenState);

                    LogUtil.d(Consts.HANDLER_TAG, "UploadSaleLogHandler replyWith orderId = " + saleLogMap.get(CloudConsts.VM_ORDER_ID) + " status= " + lockOpenState);
                    // 从超时队列中移除
                    removePayoutMap(orderId);
                }
            });
            wrapper.open(address, groupNo, timeoutInSec);
            LogUtil.d(Consts.HANDLER_TAG, "payout sn = " + mqttSN + " address = " + address + " groupNo=" + groupNo + " timeoutInSec = " + timeoutInSec);

            //出货指令到达通知
            LogUtil.d(Consts.HANDLER_TAG, "ReplyDeliverHandler " + " mqttSN = " + mqttSN + " orderId=" + orderId);
            MqttReplyHandlerMgr.replyWith(mContext, ReplyDeliverHandler.MY_TOPIC, deliverMap);
            LogUtil.d(Consts.HANDLER_TAG, "DeliverHandler leave handleContent");
        } catch (Exception ex) {
            LogUtil.d(Consts.HANDLER_TAG, "exception in DeliverHandler = " + ex.toString());
        }
        return true;
    }

    /**
     * 出币中的处理
     *
     * @param content
     */
    private void handleBusyOperation(JSONObject content) {
        try {
            if (null == content) {
                return;
            }
            String mqttSN = JsonUtils.getString(content, CloudConsts.SN);
            LogUtil.d(Consts.HANDLER_TAG, "busy with sn = " + mqttSN);
            UploadGashaponDeliverResultHandler anotherUpload = new UploadGashaponDeliverResultHandler(mContext);

            Map<String, String> map = new HashMap<String, String>();
            map.put(CloudConsts.SN, mqttSN);
            anotherUpload.setMap(map);
            anotherUpload.send(String.valueOf(CloudReplyBaseHandler.UNKNOWN));
        } catch (Exception ex) {
            LogUtil.d(Consts.HANDLER_TAG, "handleBusyOperation exception");
            ex.printStackTrace();
        }
    }

    // TODO 超时的处理,失败
    private void startTimeoutLoop() {
        try {
            if (null != mTimoutTimer) {
                LogUtil.d(Consts.HANDLER_TAG, "startTimeoutLoop is running");
                return;
            }
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    runTimeoutLoop();
                }
            };
            mTimoutTimer = new Timer();
            mTimoutTimer.schedule(task, PAYOUT_TIMEOUT_CHECK_INTERVAL_IN_MILLS, PAYOUT_TIMEOUT_CHECK_INTERVAL_IN_MILLS);
            LogUtil.d(Consts.HANDLER_TAG, "startTimeoutLoop");
        } catch (Exception ex) {
            LogUtil.d(Consts.HANDLER_TAG, "startTimeoutLoop exception");
            ex.printStackTrace();
        }
    }

    private void runTimeoutLoop() {
        //TODO 检查其中的数据，如果已经超时了，则上传出货日期
        if (mOrderVectors.isEmpty()) {
            return;
        }
        Map<String, String> saleLogMap = null;
        String timeoutCntStr = "";
        try {
            UploadSaleLogHandler saleLogHandler = new UploadSaleLogHandler(mContext);

            synchronized (this) {
                for (int i = mOrderVectors.size() - 1; i >= 0; i--) {
                    saleLogMap = (Map<String, String>) mOrderVectors.get(i);
                    if (null == saleLogMap || saleLogMap.isEmpty()) {
                        continue;
                    }

                    // TODO 每次首先检查超时计数器，如果不大于0了，则认为超时了；否则将超时的计数器减一
                    String orderId = saleLogMap.get(CloudConsts.VM_ORDER_ID);
                    timeoutCntStr = saleLogMap.get(TIMEOUT_COUNT_KEY);
                    if (TextUtils.isEmpty(timeoutCntStr) || !TextUtils.isDigitsOnly(timeoutCntStr)) {
                        timeoutCntStr = "0";
                    }

                    int cnt = Integer.valueOf(timeoutCntStr);
//                            LogUtil.d(Consts.HANDLER_TAG, "orderId = " + orderId + " order timeoutCnt = " + timeoutCntStr + " tick period = " + PAYOUT_TIMEOUT_CHECK_INTERVAL_IN_MILLS);
                    if (cnt > 0) {
                        saleLogMap.put(TIMEOUT_COUNT_KEY, String.valueOf(cnt - 1));
                        continue;
                    }

                    // 上传超时的销售日志
                    LogUtil.d(Consts.HANDLER_TAG, "Order timeout,upload sale log,orderId = " + orderId);
                    saleLogMap.remove(TIMEOUT_COUNT_KEY);

                    // 测试用
                    if (!Consts.PRODUCTION_ON) {
                        saleLogMap.put(CloudConsts.VM_S2STATE, "0");
                    }

                    saleLogHandler.setMap(saleLogMap);
                    saleLogHandler.send(String.valueOf(CloudReplyBaseHandler.NOT_ROTATE));

                    //从队列中移除
                    removePayoutMap(i);
                }
            }
        } catch (Exception ex) {
            LogUtil.d(Consts.HANDLER_TAG, "runTimeoutLoop exception");
            //reset count key
            if (TextUtils.isEmpty(timeoutCntStr) || !TextUtils.isDigitsOnly(timeoutCntStr)) {
                saleLogMap.put(TIMEOUT_COUNT_KEY, "0");
            }
            ex.printStackTrace();
        }
    }


    private void addPayoutMap(Map<String, String> saleLogMap) {
        if (null == saleLogMap || saleLogMap.isEmpty()) {
            return;
        }
        String address = saleLogMap.get(CloudConsts.DEVICE_SEQ);
        String location = saleLogMap.get(CloudConsts.LOCATION);
        if (!TextUtils.isEmpty(address) && !TextUtils.isEmpty(location)) {
            mBusyMap.put(ADDRESS_GROUP_UID(address, location), "1");
        }
        synchronized (this) {
            mOrderVectors.add(saleLogMap);
        }
        LogUtil.d(Consts.HANDLER_TAG, "add to timeout vector orderId =" + saleLogMap.get(CloudConsts.VM_ORDER_ID));
    }

    private void removePayoutMap(String orderId) {
        if (mOrderVectors.isEmpty()) {
            return;
        }

        synchronized (this) {
            try {
                for (int i = mOrderVectors.size() - 1; i >= 0; i--) {
                    Map<String, String> saleLogMap = (Map<String, String>) mOrderVectors.get(i);
                    if (null == saleLogMap || saleLogMap.isEmpty()) {
                        continue;
                    }

                    String mapOrderID = saleLogMap.get(CloudConsts.VM_ORDER_ID);
                    if (TextUtils.isEmpty(mapOrderID)) {
                        continue;
                    }

                    if (TextUtils.equals(mapOrderID, orderId)) {
                        removePayoutMap(i);
                        return;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void removePayoutMap(int pos) {
        if (pos < 0 || pos >= mOrderVectors.size()) {
            return;
        }
        Map<String, String> saleLogMap = (Map<String, String>) mOrderVectors.get(pos);

        try {
            String mapOrderID = saleLogMap.get(CloudConsts.VM_ORDER_ID);

            LogUtil.d(Consts.HANDLER_TAG, "remove from timeout vector orderId =" + mapOrderID);
            String address = saleLogMap.get(CloudConsts.DEVICE_SEQ);
            String location = saleLogMap.get(CloudConsts.LOCATION);
            if (!TextUtils.isEmpty(address) && !TextUtils.isEmpty(location)) {
                mBusyMap.remove(ADDRESS_GROUP_UID(address, location));
            }

            int size = mOrderVectors.size();
            mOrderVectors.remove(pos);
            LogUtil.d(Consts.HANDLER_TAG, "map size change from " + size + " to " + mOrderVectors.size());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String ADDRESS_GROUP_UID(String address, String group) {
        return String.format("%s_%s", address, group);
    }
}
