package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import com.idreems.openvm.protocols.ProtocolBaseHandler;
import com.idreems.openvm.utils.LogUtil;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ramonqlee on 5/17/16.
 */
public class MqttReplyHandlerMgr {
    private static final String TAG = MqttReplyHandlerMgr.class.getSimpleName();

    static Map<String, ProtocolBaseHandler> sInst2Handler = new HashMap<String, ProtocolBaseHandler>();

    private static void makesureInit(Context context) {
        //  注册更多handler
        registerHandler(new ReplyConfigHandler(context));
        registerHandler(new ReplyDeliverHandler(context));
        registerHandler(new ReplyLatestSaleLogHandler(context));
    }

    /**
     * 注册处理器，如果已经注册过，直接覆盖
     *
     * @param handler
     */
    public static void registerHandler(ProtocolBaseHandler handler) {
        if (null == handler) {
            return;
        }
        sInst2Handler.put(handler.name(), handler);
    }

    public static boolean replyWith(Context context, String topic, Map<String, String> payload) {
        makesureInit(context);
        if (null == sInst2Handler) {
            return false;
        }

        Object object = sInst2Handler.get(topic);
        if (!(object instanceof ProtocolBaseHandler)) {
            return false;
        }

        try {
            JSONObject inObject = new JSONObject();
            inObject.put(CloudConsts.TOPIC, topic);

            // 增加payload
            if (null != payload) {
                JSONObject temp = new JSONObject();
                inObject.put(CloudConsts.PAYLOAD, temp);

                for (Map.Entry<String, String> entry : payload.entrySet()) {
                    temp.put(entry.getKey(), entry.getValue());
                }
            }

            return ((ProtocolBaseHandler) object).handle(inObject);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d(TAG,e.getMessage());
        }
        return false;
    }
}
