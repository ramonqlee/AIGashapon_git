package com.idreems.openvm.PushHandler;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.idreems.openvm.Push.PushObserver;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.widget.Task;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 8/7/16.
 */
public class SwitchAirplaneModeHandler implements PushObserver {
    private static final String TAG = SwitchAirplaneModeHandler.class.getSimpleName();

    private static final String HANDLER_TYPE = "SWITCH_AIRPLANE_MODE";
    private static final String ENABLE = "ENABLE";

    private Context mContext;

    public SwitchAirplaneModeHandler(Context context) {
        mContext = context;
    }

    public boolean onMessage(String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }

        try {
            JSONObject jsonObject = new JSONObject(message);
            String type = JsonUtils.getString(jsonObject, Task.TASK_KEY);
            if (TextUtils.isEmpty(type) || !TextUtils.equals(type, HANDLER_TYPE)) {
                return false;
            }

            final String nodeId = JsonUtils.getString(jsonObject, Config.NODE_ID);
            final String myNodeId = Config.sharedInstance(mContext).getValue(Config.NODE_ID);
            if (!TextUtils.isEmpty(nodeId) && !TextUtils.equals(nodeId, myNodeId)) {
                return false;
            }

            boolean enable = JsonUtils.getBoolean(jsonObject, ENABLE);

            LogUtil.d(Consts.TASK_TAG, "SwitchAirplaneModeHandler enable = " + enable);
            switchAirplaneMode(mContext, enable);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    // 切换飞行模式
    public static void switchAirplaneMode(Context context, boolean enable) {
        if (null == context) {
            return;
        }

        try {
            Intent intent = new Intent("com.android.action.SWITCH_AIRPLANE_MODE");
            intent.putExtra("enable", enable); // true为启动此功能，false为关闭此功能
            context.sendBroadcast(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
