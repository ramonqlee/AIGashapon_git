package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import com.idreems.openvm.paho.mqtt.MQTTMgr;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.utils.TimeUtil;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class GetTimeHandler extends CloudBaseHandler {
    public static final String MY_TOPIC = "get_time";

    public GetTimeHandler(Context context) {
        super(context);
    }

    @Override
    public String name() {
        return MY_TOPIC;
    }

    public boolean handle(Object object) {
        if (!(object instanceof JSONObject)) {
            return false;
        }

//        mConnection = (CallbackConnection) JsonUtils.getObject((JSONObject) object, CloudConsts.MQTT_CLIENT_CONNECTION);
        sendGetTime();

        return true;
    }

    public void sendGetTime() {
        try {
            // test get_time publish
            JSONObject msg = new JSONObject();
            Config config = Config.sharedInstance(mContext);
            String nodeId = config.getValue(Config.NODE_ID);
            final String topic = String.format("%s/%s",nodeId,name());
            msg.put(CloudConsts.TIMESTAMP, TimeUtil.getCheckedCurrentTimeInMills(mContext) / 1000);
            MQTTMgr.sharedInstance(mContext).publishMessage(topic,msg.toString());
//            if (null != connection) {
//                connection.publish(topic, msg.toString().getBytes(), QoS.AT_MOST_ONCE, false, new Callback<Void>() {
//                    @Override
//                    public void onSuccess(Void value) {
//                        Log.d(Consts.LOG_TAG_MQTT, "publish succeed,topic=" + topic);
//                    }
//
//                    @Override
//                    public void onFailure(Throwable value) {
//                        Log.d(Consts.LOG_TAG_MQTT, "publish fail,topic=" + topic);
//                    }
//                });
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
