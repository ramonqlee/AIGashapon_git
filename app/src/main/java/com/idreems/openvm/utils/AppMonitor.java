package com.idreems.openvm.utils;

import android.content.Context;

import com.idreems.openvm.constant.Consts;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ramonqlee_macpro on 21/10/2017.
 */

public class AppMonitor {
    private static final long MONITOR_INTERVAL = 1 * 60 * 1000;
    private static AppMonitor sAppMonitor = new AppMonitor();
    private Context mContext;
    private Timer mTimer;

    public static AppMonitor sharedInstance(final Context context) {
        sAppMonitor.mContext = context.getApplicationContext();
        return sAppMonitor;
    }

    public void start(final String processName) {
        if (null != mTimer) {
            mTimer.cancel();
        }
        mTimer = new Timer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (Utils.isAppRunnnig(mContext, processName)) {
                    LogUtil.d(Consts.LOG_TAG, processName + " running");
                    return;
                }
                DeviceUtils.restartApp(mContext);
            }
        };
        mTimer.schedule(task, MONITOR_INTERVAL, MONITOR_INTERVAL);
        LogUtil.d(Consts.LOG_TAG, "start monitor app =" + processName);
    }

}
