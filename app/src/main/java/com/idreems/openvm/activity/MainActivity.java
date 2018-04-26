package com.idreems.openvm.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.idreems.openvm.MyApplication;
import com.idreems.openvm.Push.PushDispatcher;
import com.idreems.openvm.PushHandler.PowerOnOffHandler;
import com.idreems.openvm.PushHandler.RebootHandler;
import com.idreems.openvm.PushHandler.ReloadBannerHandler;
import com.idreems.openvm.PushHandler.SnapshotHandler;
import com.idreems.openvm.PushHandler.SwitchAirplaneModeHandler;
import com.idreems.openvm.PushHandler.UpdateAPPHandler;
import com.idreems.openvm.R;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.utils.DeviceUtils;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.TimeUtil;
import com.idreems.openvm.utils.Utils;
import com.idreems.openvm.widget.DigitalClock;
import com.idreems.openvm.widget.TelephonyIcons;
import android_serialport_api.vmc.AIGashaponMachine.utils.AIGashponManager;


public class MainActivity extends Activity {
    private static final long UI_UPDATE_TIMER_INTERVAL = 20 * 1000;
    private BroadcastReceiver mRebootReceiver;
    private long mTimeTickCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView  infoTv = (TextView) findViewById(R.id.server_mode_textview);
        if (null != infoTv) {
            infoTv.setVisibility(Consts.PRODUCTION_ON ? View.GONE : View.VISIBLE);
        }

        delayUpdateUITask();
        PushDispatcher.sharedInstance().addObserver(new SnapshotHandler(this));
        initView();
        initOnce();
        rebootPeriodically();
        test();
        LogUtil.d(Consts.LOG_TAG, MainActivity.class.getSimpleName() + " onCreate");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 不支持返回
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterPushObserver();
        removeUpdateUITask();
        LogUtil.d(Consts.LOG_TAG,"MainActivity onDestroy");
    }

    private void initOnce() {
        try {
            if (!Consts.PRODUCTION_ON && Consts.TEST_CASE_ENABLED) {
                Config config = Config.sharedInstance(getApplicationContext());
                String devicePath = config.getValue(Config.PC_DEVICE);
                String baudRateStr = config.getValue(Config.PC_BAUDE);
                if (TextUtils.isEmpty(devicePath) || TextUtils.isEmpty(baudRateStr) || !TextUtils.isDigitsOnly(baudRateStr)) {
                    LogUtil.d(Consts.LOG_TAG, "illegal parameter for devicePath=" + devicePath + " baudRate=" + baudRateStr);
                    return;
                }
                int baudRate = Integer.decode(baudRateStr);
                AIGashponManager wrapper = AIGashponManager.sharedInstance(devicePath, baudRate);

                for (byte i = 1; i < 4; ++i) {
                    byte address = 0;
                    byte groupNo = i;
                    short timeoutInSec = 2 * 60;
                    wrapper.open(address, groupNo, timeoutInSec);
                    LogUtil.d(Consts.HANDLER_TAG, "test payout address = " + address + " groupNo=" + groupNo + " timeoutInSec = " + timeoutInSec);
                }
            }
            registerPushObserver();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        // 开始轮播图片
        ReloadBannerHandler.startLoopDisplayImagesOnMainThread(Utils.getImagesDir(getApplicationContext()), (ImageView) findViewById(R.id.banner));
        updateUI();
    }

    private void registerPushObserver() {
        // 注册更多的Push处理器
        PushDispatcher dispatcher = PushDispatcher.sharedInstance(Consts.PERIOD_DISPATCHER_ID);
        dispatcher.addObserver(new RebootHandler(getApplicationContext()));
        dispatcher.addObserver(new UpdateAPPHandler(getApplicationContext()));
        dispatcher.addObserver(new ReloadBannerHandler(getApplicationContext(), (ImageView) findViewById(R.id.banner)));
        dispatcher.addObserver(new PowerOnOffHandler(getApplicationContext()));
        dispatcher.addObserver(new SwitchAirplaneModeHandler(getApplicationContext()));
    }

    private void unregisterPushObserver() {
        PushDispatcher dispatcher = PushDispatcher.sharedInstance(Consts.PERIOD_DISPATCHER_ID);
        dispatcher.clearObserver();
    }


    private Handler mUIHandler = new Handler(Looper.getMainLooper());
    private Runnable mUIUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();
            delayUpdateUITask();
        }
    };

    private void removeUpdateUITask() {
        mUIHandler.removeCallbacks(mUIUpdateRunnable);
    }

    private void delayUpdateUITask() {
        removeUpdateUITask();
        mUIHandler.postDelayed(mUIUpdateRunnable, UI_UPDATE_TIMER_INTERVAL);
    }

    private void updateUI() {
        try {
            // TODO 更新点位信息，点位id，信号强度和时间
            final Config config = Config.sharedInstance(getApplicationContext());
            final String nodeId = config.getValue(Config.NODE_ID);
            final String nodeName = config.getValue(Config.NODE_NAME);
            final long currentTimeInMills = TimeUtil.getCheckedCurrentTimeInMills(getApplicationContext());

            TextView nodeIDTV = (TextView) findViewById(R.id.tv_node_id);
            if (null != nodeIDTV) {
                final String displayText = "编号：" + nodeId;
                if (!TextUtils.equals(nodeIDTV.getText(),displayText)) {
                    nodeIDTV.setText(displayText);
                }
            }

            TextView nodeNameTV = (TextView) findViewById(R.id.tv_node_name);
            if (null != nodeNameTV) {
                if (!TextUtils.equals(nodeNameTV.getText(),nodeName)) {
                    nodeNameTV.setText(nodeName);
                }
            }

            DigitalClock timer = (DigitalClock) findViewById(R.id.dg_timer);
            if (null != timer) {
                timer.setTimeOffsetInMillis(currentTimeInMills - System.currentTimeMillis());
            }

            final int[] iconList = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH[0];
            ImageView mSignalImageView = (ImageView) findViewById(R.id.iv_signal);
            mSignalImageView.setBackgroundResource(iconList[TelephonyIcons.getIconLevel(MyApplication.getMyApplication().getAsuLevel())]);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void rebootPeriodically() {
        //reboot test
        if (Consts.REBOOT_ENABLE) {
            if (null != mRebootReceiver) {
                unregisterReceiver(mRebootReceiver);
                mRebootReceiver = null;
            }
            mRebootReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (null == intent || !TextUtils.equals(Intent.ACTION_TIME_TICK, intent.getAction())) {
                        LogUtil.d(Consts.LOG_TAG, "MainActivity broadcast action =" + intent.getAction());
                        return;
                    }

                    if (!DeviceUtils.sDelayRebootApp && ++mTimeTickCount < Consts.REBOOT_INTERVAL_IN_MIN) {
                        return;
                    }

                    LogUtil.d(Consts.TASK_TAG, "restartApp periodically now");
                    DeviceUtils.restartApp(getApplicationContext());
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            registerReceiver(mRebootReceiver, filter);
            LogUtil.e(Consts.LOG_TAG, "reboot app enabled");
        }
    }

    private void test() {
        if (Consts.PRODUCTION_ON) {
            return;
        }

//        LogUtil.d(Consts.TASK_TAG, "reboot ater 1 s");
//        DeviceUtils.openAirplaneModeOn(this,true);
    }

}
