package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import com.idreems.openvm.paho.mqtt.MQTTMgr;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.TimeUtil;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by ramonqlee on 7/15/16.
 */
public class UploadGashaponDeliverResultHandler extends UploadDeliverResultHandler {
//    private int mState;
    private Map<String, String> mPayload;

    public UploadGashaponDeliverResultHandler(Context context) {
        super(context);

    }

    public void setMap(Map<String, String> payload) {
        mPayload = payload;
    }

//    public void setState(int state) {
//        mState = state;
//    }

    protected void addExtraPayloadContent(JSONObject contentJson) {
//        if (null == contentJson) {
//            return;
//        }
//        try {
//            contentJson.put(CloudConsts.STATE, mState);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }

    public void send(String status) {
        try {
            JSONObject myPayload = new JSONObject();
            JSONObject myContent = new JSONObject();
            for (Map.Entry<String, String> entry : mPayload.entrySet()) {
                myContent.put(entry.getKey(), entry.getValue());
            }

            myContent.put(CloudConsts.STATE, status);
            myPayload.put(CloudConsts.TIMESTAMP, TimeUtil.getCheckedCurrentTimeInMills(mContext) / 1000);
            myPayload.put(CloudConsts.CONTENT, myContent);

            MQTTMgr.sharedInstance(mContext).publishMessage(getTopic(),myPayload.toString());
        } catch (Exception ex) {
            LogUtil.d("DeliverTest", "exception = " + ex.getMessage());
            ex.printStackTrace();
        }

    }
}
