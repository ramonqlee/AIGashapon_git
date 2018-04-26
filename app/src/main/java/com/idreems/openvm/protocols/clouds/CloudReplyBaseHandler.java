package com.idreems.openvm.protocols.clouds;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.paho.mqtt.MQTTMgr;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.utils.TimeUtil;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/18/16.
 */
public abstract class CloudReplyBaseHandler extends CloudBaseHandler {
// 状态定义
    /**
     * “state”: 1, //1成功，2收到并处理指令时已超时，3硬件出货失败，4硬件繁忙，5币量不足，6币售空，99状态未知
     */
    public static final int SUCCESS = 1;
    public static final int TIMEOUT = 2;
    public static final int FAIL = 3;
    public static final int BUSY = 4;
    public static final int INSUFFICIENT = 5;
    public static final int EMPTY = 6;

    public static final int ELECTRIC_LOCK_OPEN_FAIL = 11;//电子锁打开失败
    public static final int NOT_ROTATE = 13;//未旋转
    public static final int UNKNOWN = 99;

    private int mState;

    public CloudReplyBaseHandler(Context context) {
        super(context);
    }

    public boolean handle(Object object) {
        boolean r = false;
        if (!(object instanceof JSONObject)) {
            return r;
        }

        try {
            JSONObject jsonObject = (JSONObject) object;
            if (!TextUtils.equals(jsonObject.getString(CloudConsts.TOPIC), name())) {
                return r;
            }
            JSONObject payloadJson = new JSONObject(jsonObject.getString(CloudConsts.PAYLOAD));
            return handleContent(payloadJson);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return r;
    }

    protected boolean handleContent(JSONObject payloadJsons) {
        boolean r = false;
        JSONObject myPayload = new JSONObject();
        JSONObject myContent = new JSONObject();
        try {
            if (null != payloadJsons) {
                myContent = new JSONObject(payloadJsons.toString());
            }

            // FIXME 需要客户端的时间，是否考虑采用本地矫正时间
            myPayload.put(CloudConsts.TIMESTAMP, TimeUtil.getCheckedCurrentTimeInMills(mContext) / 1000);
            myPayload.put(CloudConsts.CONTENT, myContent);
            addExtraPayloadContent(myContent);

            MQTTMgr.sharedInstance(mContext).publishMessage(getTopic(),myPayload.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r;
    }

    protected String getTopic() {
        String nodeId = Config.sharedInstance(mContext).getValue(Config.NODE_ID);
        if (TextUtils.isEmpty(nodeId)) {
            return "";
        }
        return String.format("%s/%s", nodeId, name());
    }

    abstract protected void addExtraPayloadContent(JSONObject contentJson);

    public static int getCoinHopperState(byte state) {

        return UNKNOWN;
    }
}
