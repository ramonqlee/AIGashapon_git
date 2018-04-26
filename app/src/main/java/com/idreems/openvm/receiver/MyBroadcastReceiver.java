package com.idreems.openvm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.utils.LogUtil;

import static com.idreems.openvm.MyApplication.openConfigUI;


/**
 * Created by ramonqlee on 11/07/2017.
 */

public class MyBroadcastReceiver extends BroadcastReceiver {
    private static final String ENG_MODE_SWITCH = "android.intent.action.ENG_MODE_SWITCH";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtil.d(Consts.LOG_TAG, MyBroadcastReceiver.class.getSimpleName() + "action = " + action);
        if (TextUtils.isEmpty(action)) {
            return;
        }
        if (TextUtils.equals(ENG_MODE_SWITCH, action)) {
            if (1 == intent.getIntExtra("state", 1)) {
                //维护模式为打开状态
                openConfigUI(context);
            }
        }

    }
}
