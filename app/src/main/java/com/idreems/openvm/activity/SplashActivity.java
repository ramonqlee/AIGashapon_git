package com.idreems.openvm.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import com.idreems.openvm.R;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.Utils;

import static com.idreems.openvm.MyApplication.isMainApp;

public class SplashActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final String processName = Utils.getProcessName(this, Process.myPid());
        LogUtil.d(Consts.LOG_TAG, ".......................startApp SplashActivity start in " + processName + ".......................");
        if (!isMainApp(getApplicationContext())) {
            LogUtil.d(Consts.LOG_TAG, ".......................startApp SplashActivity quit here......................."+processName);
            finish();
            return;
        }

        if (Consts.REBOOT_ENABLE) {
            enterMainActivity();
        } else {
            delayEnterMainActivity();
        }
        if (!Consts.PRODUCTION_ON) {
            testCase();
        }
    }

    private void delayEnterMainActivity() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                enterMainActivity();
            }
        }, 5000);
    }

    private void enterMainActivity() {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), MainActivity.class);
        startActivity(intent);

        finish();
    }

    private void testCase() {
    }

}
