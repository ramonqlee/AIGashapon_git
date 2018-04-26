package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import com.idreems.openvm.utils.LogUtil;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class ReplyConfigHandler extends  CloudReplyBaseHandler{
    public static final String MY_TOPIC = "reply_config";

    public ReplyConfigHandler(Context context)
    {
        super(context);
    }

    @Override
    public String name() {
        return MY_TOPIC;
    }

    protected void addExtraPayloadContent(JSONObject contentJson)
    {
        LogUtil.d("ReplyConfigHandler");
    }
}
