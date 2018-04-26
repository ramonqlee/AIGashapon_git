package com.idreems.openvm.protocols.clouds;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class SetConfigHandler extends CloudBaseHandler {
    public static final String MY_TOPIC = "set_config";

    public SetConfigHandler(Context context) {
        super(context);
    }

    @Override
    public String name() {
        return MY_TOPIC;
    }

    protected boolean handleContent(JSONObject content) {
        boolean r = false;
        if (null == content) {
            return r;
        }

        // 更新状态，并通知云端收到了
        String state = JsonUtils.getString(content, CloudConsts.STATE);
        String sn = JsonUtils.getString(content, CloudConsts.SN);
        String nodeName = JsonUtils.getString(content, CloudConsts.NODE_NAME);
        String price = JsonUtils.getString(content, CloudConsts.NODE_PRICE);
        String rebootSchedule = JsonUtils.getString(content, CloudConsts.REBOOT_SCHEDULE);
        if (TextUtils.isEmpty(state) || TextUtils.isEmpty(sn)) {
            Log.e(Consts.LOG_TAG_PROTOCOL, "state or sn is empty");
            return false;
        }

        Config config = Config.sharedInstance(mContext);
        config.saveValue(Config.VM_SATE, state);
        config.saveValue(Config.NODE_NAME, nodeName);
        config.saveValue(Config.NODE_PRICE, price);
        config.saveValue(Config.REBOOT_SCHEDULE, rebootSchedule);

        Map<String, String> map = new HashMap<String, String>();
        map.put(CloudConsts.SN, sn);
        map.put(CloudConsts.STATE, state);
        map.put(CloudConsts.NODE_NAME, nodeName);
        map.put(CloudConsts.NODE_PRICE, price);
        map.put(CloudConsts.REBOOT_SCHEDULE, rebootSchedule);

        MqttReplyHandlerMgr.replyWith(mContext, ReplyConfigHandler.MY_TOPIC, map);

        // 通知js
//        WebSocket ws = LocalWebsocketServerWrapper.shareInstance(mContext).getWebSocket();
//        WsReplyHandlerMgr.replyWith(mContext, ReplyNodeInfo.MY_INSTRUCTION, ws);

        // 稍后刷新下页面
//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                MainActivity.sendRefreshJSLocalBroadcast(mContext);
//            }
//        },2000);

        return true;
    }
}
