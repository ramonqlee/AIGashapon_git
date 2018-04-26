package com.idreems.openhost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.idreems.openhost.utils.DirUtil;
import com.igexin.sdk.PushManager;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private static Activity sActivity;
    private static final long PUSH_TIMER_INTERVAL = 10 * 1000;
    private Timer mPushTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DirUtil.init(getApplicationContext());
        // setContentView(R.layout.splash);
        sActivity = this;
        startPush();
        startPushMonitor();
    }

    // 不允许返回退出
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK /*&& event.getRepeatCount() == 0*/) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(this, HostService.class));
    }

    public static Activity getActivity() {
        return sActivity;
    }

    private void startPush() {
        Log.d("PushTag", "startPush");
        PushManager.getInstance().initialize(MainActivity.this, MyGetuiPushService.class);
        PushManager.getInstance().registerPushIntentService(this.getApplicationContext(), MyGetuiIntentService.class);
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

}
