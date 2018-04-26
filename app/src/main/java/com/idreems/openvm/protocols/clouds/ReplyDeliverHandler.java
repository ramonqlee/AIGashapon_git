package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class ReplyDeliverHandler extends  CloudReplyBaseHandler{
    public static final String MY_TOPIC = "reply_deliver";

    public ReplyDeliverHandler(Context context)
    {
        super(context);
    }

    @Override
    public String name() {
        return MY_TOPIC;
    }

    protected void addExtraPayloadContent(JSONObject contentJson)
    {
    }
}
