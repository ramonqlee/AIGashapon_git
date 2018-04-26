package com.idreems.openvm.protocols.clouds;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.protocols.ProtocolBaseHandler;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/17/16.
 */
public abstract class CloudBaseHandler implements ProtocolBaseHandler {
    protected Context mContext;
    protected long mTimestampInSec;

    public CloudBaseHandler(Context context) {
        mContext = context;
    }

    public boolean handle(Object object) {
        boolean r = false;
        if (!(object instanceof JSONObject)) {
            return r;
        }
        String payload = CloudBaseHandler.match(name(), object);
        if (TextUtils.isEmpty(payload)) {
            return r;
        }

        try {
            JSONObject payloadJson = new JSONObject(payload);
            //TODO 待根据 timestamp，做些事情
            mTimestampInSec = JsonUtils.getLong(payloadJson, CloudConsts.TIMESTAMP);
            mTimestampInSec = (mTimestampInSec < 0) ? 0 : mTimestampInSec;
            if (name().equalsIgnoreCase(ReplyTimeHandler.MY_TOPIC)) {
                return handleContent(mTimestampInSec, JsonUtils.getJsonObject(payloadJson, CloudConsts.CONTENT));
            }
            return handleContent(JsonUtils.getJsonObject(payloadJson, CloudConsts.CONTENT));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return r;
    }

    protected boolean handleContent(JSONObject contentJson) {
        return false;
    }

    protected boolean handleContent(long timestamp, JSONObject contentJson) {
        return false;
    }

    // 根据inst，进行匹配，然后返回content
    public static String match(String topic, Object object) {
        if (TextUtils.isEmpty(topic) || !(object instanceof JSONObject)) {
            return "";
        }
        String r = JsonUtils.getString((JSONObject) object, CloudConsts.TOPIC);
        if (!TextUtils.equals(topic, r)) {
            return "";
        }
        try {
            return JsonUtils.getString((JSONObject) object, CloudConsts.PAYLOAD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
