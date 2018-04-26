package com.idreems.openvm.protocols.websocket;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/17/16.
 */
public class ReplyVMState extends WebReplyBase {
    // 售货机状态
    public static final String STATE_INIT = "INIT";
    public static final String STATE_TEST = "TEST";
    public static final String STATE_ON = "ON";
    public static final String STATE_PAUSE = "PAUSE";

    public static final String MY_INSTRUCTION = "reply_vm_state";

    public ReplyVMState(Context context) {
        super(context);
    }

    public String name() {
        return MY_INSTRUCTION;
    }

    protected String getResponse() {
/*
{
“instruction”: “reply_vm_state”, “content”:{
“state”: “TEST”, //INIT, TEST, ON, PAUSE
} }
 */
        Config config = Config.sharedInstance(mContext);
        String r = config.getValue(Config.VM_SATE);
        if (TextUtils.isEmpty(r)) {
            r = "";
            Log.w(Consts.LOG_TAG_WEB,"vm state is not set");
        }

        try {
            JSONObject obj = new JSONObject();
            obj.put(WebConsts.CONST_INSTRUCTION, MY_INSTRUCTION);

            JSONObject content = new JSONObject();
            obj.put(WebConsts.CONST_CONTENT, content);
            content.put(WebConsts.CONST_STATE, r);
            return obj.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
