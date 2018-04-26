package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import com.idreems.openvm.paho.mqtt.MQTTMgr;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.TimeUtil;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class UploadSaleLogHandler extends CloudReplyBaseHandler {
    public static final String MY_TOPIC = "upload_sale_log";

//    private int mState;
    private Map<String, String> mPayload;

    public void setMap(Map<String, String> payload) {
        mPayload = payload;
    }

    public UploadSaleLogHandler(Context context) {
        super(context);
    }

    @Override
    public String name() {
        return MY_TOPIC;
    }

//    public void setState(int state) {
//        mState = state;
//    }

    protected void addExtraPayloadContent(JSONObject contentJson) {
        // TODO 待增加状态字段
//        if (null == contentJson) {
//            return;
//        }
//
//        try {
//            contentJson.put(CloudConsts.STATE, mState);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }


    public void send(String state) {
        try {
            JSONObject myPayload = new JSONObject();
            JSONObject myContent = new JSONObject();
            for (Map.Entry<String, String> entry : mPayload.entrySet()) {
                myContent.put(entry.getKey(), entry.getValue());
            }

            myContent.put(CloudConsts.STATE, state);
            myPayload.put(CloudConsts.TIMESTAMP, TimeUtil.getCheckedCurrentTimeInMills(mContext) / 1000);
            myPayload.put(CloudConsts.CONTENT, myContent);

            MQTTMgr.sharedInstance(mContext).publishMessage(getTopic(), myPayload.toString());
        } catch (Exception ex) {
            LogUtil.d("DeliverTest", "exception = " + ex.getMessage());
            ex.printStackTrace();
        }

    }
}
