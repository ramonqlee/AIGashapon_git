package com.idreems.openvm.protocols.websocket;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.protocols.ProtocolBaseHandler;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/17/16.
 */
public abstract  class WebBaseHandler implements ProtocolBaseHandler {
    protected Context mContext;

    public WebBaseHandler(Context context)
    {
        mContext = context;
    }

    // 根据inst，进行匹配，然后返回content
    public static boolean match(String inst,Object object)
    {
        if(TextUtils.isEmpty(inst) || !(object instanceof JSONObject))
        {
            return false;
        }
        String message = JsonUtils.getString((JSONObject) object,WebConsts.CONST_MESSAGE);
        if (TextUtils.isEmpty(message))
        {
            return false;
        }
        try
        {
            JSONObject json = new JSONObject(message);
            String r = JsonUtils.getString(json,WebConsts.CONST_INSTRUCTION);
            return TextUtils.equals(r,inst);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static JSONObject getContent(Object object)
    {
        if(!(object instanceof JSONObject))
        {
            return null;
        }
        String message = JsonUtils.getString((JSONObject) object,WebConsts.CONST_MESSAGE);
        if (TextUtils.isEmpty(message))
        {
            return null;
        }
        try
        {
            JSONObject json = new JSONObject(message);
            String r = JsonUtils.getString(json,WebConsts.CONST_CONTENT);
            return new JSONObject(r);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
