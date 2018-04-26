package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class ReplyMachineVariablesHandler extends  UploadVMCStateHandler{
    public static final String MY_TOPIC = "reply_machine_variables";
    public ReplyMachineVariablesHandler(Context context)
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
