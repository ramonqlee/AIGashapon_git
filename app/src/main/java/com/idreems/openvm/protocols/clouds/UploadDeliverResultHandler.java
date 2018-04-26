package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class UploadDeliverResultHandler extends  CloudReplyBaseHandler{
    public static final String MY_TOPIC = "upload_deliver_result";

    public UploadDeliverResultHandler(Context context)
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
