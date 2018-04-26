package com.idreems.openhost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class PluginReceiver extends BroadcastReceiver {
    public static final String TAG = PluginReceiver.class.getSimpleName();
    private static final String HEARTBEAT_ACTION = "com.idreems.openvm.action.heartbeat";
    private static final long MAX_APP_ALIVE_INTERVAL = 10 * 60 * 1000;//超过这个时间，就认为app 挂了
    private static long sLastHeartbeatTimeInMs;

    public static boolean isPluginAppAlive() {
        Log.d(TAG, "isPluginAppAlive sLastHeartbeatTimeInMs =" + sLastHeartbeatTimeInMs + " System.currentTimeMillis=" + System.currentTimeMillis());
        return Math.abs(sLastHeartbeatTimeInMs - System.currentTimeMillis()) > MAX_APP_ALIVE_INTERVAL;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // 收到app 心跳
        if (null == intent || TextUtils.isEmpty(intent.getAction())) {
            return;
        }

        Log.d(TAG, "receive broadcast action =" + intent.getAction());
        if (!TextUtils.equals(intent.getAction(), HEARTBEAT_ACTION)) {
            return;
        }

        sLastHeartbeatTimeInMs = System.currentTimeMillis();
        Log.d(TAG, "receive heartbeat sLastHeartbeatTimeInMs =" + sLastHeartbeatTimeInMs);
    }
}
