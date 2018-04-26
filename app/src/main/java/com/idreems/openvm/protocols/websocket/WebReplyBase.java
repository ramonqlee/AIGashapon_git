package com.idreems.openvm.protocols.websocket;

import android.content.Context;

/**
 * Created by ramonqlee on 5/18/16.
 */
public abstract class WebReplyBase extends WebBaseHandler {

    public WebReplyBase(Context context)
    {
        super(context);
    }

    // 同步
    protected String getResponse(){return "";}

    public boolean handle(Object object) {

        return true;
    }
}
