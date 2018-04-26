package com.idreems.openvm.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.Push.PushDispatcher;
import com.idreems.openvm.PushHandler.PayoutHandler;
import com.idreems.openvm.PushHandler.PostLogHandler;
import com.idreems.openvm.PushHandler.PowerOnOffHandler;
import com.idreems.openvm.PushHandler.RebootHandler;
import com.idreems.openvm.PushHandler.SwitchAirplaneModeHandler;
import com.idreems.openvm.R;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.file.FileUtil;
import com.idreems.openvm.paho.mqtt.MQTTMgr;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.utils.DeviceUtils;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.TimeUtil;
import com.idreems.openvm.utils.ToastUtils;
import com.idreems.openvm.widget.Task;
import com.igexin.sdk.PushManager;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ramonqlee on 7/23/16.
 */
public class BackgroundService extends Service {
    private static final String TAG = BackgroundService.class.getSimpleName();
    private static final long PUSH_TIMER_INTERVAL = 30 * 1000;
    private static final long TASK_TIMER_INTERVAL = 5 * 60 * 1000;

    private Timer mTaskTimer;
    private Timer mPushTimer;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // TODO 需要修改为定时清理
        FileUtil.startClean(this);
        LogUtil.cleanJunkLogFiles();
        MQTTMgr.sharedInstance(this).startMqttConnectionWithCustomReconnect();
        startPush();
        startPushMonitor();
        startRebootMonitor();
        registerPushObserver();
        startTaskMonitor();
        return START_STICKY;
    }


    class TimeTickerChangeReceiver extends BroadcastReceiver {
        private static final long MIN_REBOOT_INTERVAL = 30 * 60 * 1000;//机器最小的重启间隔
        private static final long ALLOW_TIME_INTEVAL = 5 * 60 * 1000;//超过时间，就不再重启了

        private void handleReceive(final Context context) {
            // 获取当前时间，如果超过了系统设定的时间，并且在一定的时间间隔内，则重启机器
            // 1. 时间进入到允许重启的时间了
            // 2. 检查是否满足最小的重启时间间隔内
            final Config config = Config.sharedInstance(context);
            final String rebootScheduleStr = config.getValue(Config.REBOOT_SCHEDULE);
            if (TextUtils.isEmpty(rebootScheduleStr)) {
                return;
            }

            final long currentTime = TimeUtil.getCheckedCurrentTimeInMills(context);

            // 追加上日期
            String[] split = rebootScheduleStr.split(":");
            int hour = Integer.valueOf(split[0]);
            int minutes = Integer.valueOf(split[1]);
            Date date = new Date(currentTime);
            date.setHours(hour);
            date.setMinutes(minutes);
            date.setSeconds(0);
            final long rebootTimeInMillis = date.getTime();

            Log.d(TAG, "rebootTimeInMillis = " + TimeUtil.formatFullTime(rebootTimeInMillis) + " currentTime = " + TimeUtil.formatFullTime(currentTime));
            // 1. 时间进入到允许重启的时间了
            if (rebootTimeInMillis < currentTime || rebootTimeInMillis - currentTime > ALLOW_TIME_INTEVAL) {
                return;
            }
            LogUtil.d("time to reboot");

            String lastRebootStr = config.getValue(Config.LAST_REBOOT);
            if (TextUtils.isEmpty(lastRebootStr)) {
                return;
            }

            try {
                long lastRebootInMills = Long.valueOf(lastRebootStr) * 1000;
                // 2. 检查是否满足最小的重启时间间隔内
                if (Math.abs(lastRebootInMills - currentTime) < MIN_REBOOT_INTERVAL) {
                    LogUtil.d("reboot recently");
                    return;
                }
                ToastUtils.show(R.string.reboot_tip);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LogUtil.d("try to reboot");
                        DeviceUtils.reboot(getApplicationContext());
                    }
                }, 3000);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent || !TextUtils.equals(Intent.ACTION_TIME_TICK, intent.getAction())) {
                LogUtil.d(Consts.LOG_TAG, "BackgroundService broadcast action =" + intent.getAction());
                return;
            }
            handleReceive(context);
        }
    }

    private TimeTickerChangeReceiver mTimeTickerReceiver;

    /**
     * 启动重启检测
     */
    private void startRebootMonitor() {
        if (null != mTimeTickerReceiver) {
            return;
        }
        try {
            mTimeTickerReceiver = new TimeTickerChangeReceiver();
            registerReceiver(mTimeTickerReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void registerPushObserver() {
        // 注册更多的Push处理器
        PushDispatcher.sharedInstance().addObserver(new PowerOnOffHandler(getApplicationContext()));
        PushDispatcher.sharedInstance().addObserver(new RebootHandler(getApplicationContext()));
        PushDispatcher.sharedInstance().addObserver(new PostLogHandler(getApplicationContext()));
        PushDispatcher.sharedInstance().addObserver(new PayoutHandler(getApplicationContext()));
        PushDispatcher.sharedInstance().addObserver(new SwitchAirplaneModeHandler(getApplicationContext()));
    }


    private void startPush() {
        PushManager.getInstance().initialize(getApplicationContext(), MyGetuiPushService.class);
    }


    private void startPushMonitor() {
        stopPushMonitor();
        if (null == mPushTimer) {
            mPushTimer = new Timer();
        }

        mPushTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                startPush();
            }
        }, PUSH_TIMER_INTERVAL, PUSH_TIMER_INTERVAL);
    }

    private void stopPushMonitor() {
        if (null == mPushTimer) {
            return;
        }
        mPushTimer.cancel();
        mPushTimer = null;
    }


    private void startTask() {
        // TODO 联网请求远程任务，然后分发
        Config config = Config.sharedInstance(getApplicationContext());
        String nodeId = config.getValue(Config.NODE_ID);
        String password = config.getValue(Config.PASSWORD);
        if (TextUtils.isEmpty(nodeId) || TextUtils.isEmpty(password)) {
            return;
        }
        Task.startTaskChecker(getApplicationContext(), nodeId, password);
    }

    private void startTaskMonitor() {
        stopTaskMonitor();
        if (null == mTaskTimer) {
            mTaskTimer = new Timer();
        }

        mTaskTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                startTask();
            }
        }, 0, TASK_TIMER_INTERVAL);
    }

    private void stopTaskMonitor() {
        if (null == mTaskTimer) {
            return;
        }
        mTaskTimer.cancel();
        mTaskTimer = null;
    }

}
