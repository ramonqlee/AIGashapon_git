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
public class PowerOnOffHandler implements PushObserver {
    private static final String TAG = PowerOnOffHandler.class.getSimpleName();

    private static final String HANDLER_TYPE = "POWER_ON_OFF";
    private static final String POWER_TIME = "power_time";
    private static final String SHUT_DOWN = "shut_down";

    private Context mContext;

    public PowerOnOffHandler(Context context) {
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

            String powerTime = JsonUtils.getString(jsonObject, POWER_TIME);
            String shutDownTime = JsonUtils.getString(jsonObject, SHUT_DOWN);

            LogUtil.d(Consts.TASK_TAG, "PowerOnOffHandler powerTime = " + powerTime + " shutDownTime = " + shutDownTime);
            setUpTimeReboot(mContext, powerTime, shutDownTime);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void setUpTimeReboot(Context context, String powerTime, String shutDownTime) {
        if (null == context) {
            return;
        }

        try {
            Intent intent = new Intent("com.ubox.auto_power_shut");
            if (!TextUtils.isEmpty(powerTime)) {
                intent.putExtra("power_time", powerTime); // intent.putExtra("power_time", "15:37"); // 自动开机时间
            }

            if (!TextUtils.isEmpty(shutDownTime)) {
                intent.putExtra("shut_time", shutDownTime);//intent.putExtra("shut_time", "15:32"); // 自动关机时间
            }

            boolean effective = (!TextUtils.isEmpty(powerTime) || !TextUtils.isEmpty(shutDownTime));
            intent.putExtra("effective", effective); // true为启动此功能，false为关闭此功能
            context.sendBroadcast(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
