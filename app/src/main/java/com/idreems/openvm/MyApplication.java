package com.idreems.openvm;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Process;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.Crash.CrashHandler;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.file.DirUtil;
import com.idreems.openvm.network.SignalStrengthUtil;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.service.BackgroundService;
import com.idreems.openvm.utils.ANRManager;
import com.idreems.openvm.utils.AppMonitor;
import com.idreems.openvm.utils.ConfigImporter;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.NetworkProber;
import com.idreems.openvm.utils.TimeUtil;
import com.idreems.openvm.utils.Utils;
import com.idreems.openvm.widget.SerialPortPreferences;
import com.liulishuo.filedownloader.FileDownloader;
import com.squareup.leakcanary.LeakCanary;

import android_serialport_api.vmc.AIGashaponMachine.AIUtils;

/**
 */
public class MyApplication extends Application {
    private static final String TAG = MyApplication.class.getSimpleName();
    private static Application sApplication;
    private int mSignalStrength;
    private int mAsuLevel;

    private static final String THIRD_SERVICE_IDENTIFIER[] = {"pushservice"};

    // 是否主程序，可能是后台服务单独占用的程序
    public static boolean isMainApp(Context context) {
        final String processName = Utils.getProcessName(context, Process.myPid());
        if (TextUtils.isEmpty(processName)) {
            return true;
        }

        for (String val : THIRD_SERVICE_IDENTIFIER) {
            if (TextUtils.isEmpty(val)) {
                continue;
            }
            if (processName.toUpperCase().contains(val.toUpperCase())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // FIXME TMEP CODE
        byte[] r = new byte[8];
//        00 00 99 93 00 03 00 00
        int pos = 0;
        r[pos++] = 0x00;
        r[pos++] = 0x00;
        r[pos++] = (byte) 0x99;
        r[pos++] = (byte) 0x93;
        r[pos++] = 0x00;
        r[pos++] = (byte) 0x03;
        r[pos++] = 0;
        r[pos++] = 0;
        short checkSum = AIUtils.checkSum(r, r.length);
        Log.d("TAG","checkSum = "+checkSum);

        sApplication = this;
        DirUtil.init(this);
        // 异常处理，不需要处理时注释掉这两句即可！
        CrashHandler crashHandler = CrashHandler.getInstance();
        // 注册crashHandler
        crashHandler.init(getApplicationContext());
        FileDownloader.setup(this);
        AppMonitor.sharedInstance(this).start(Consts.PACKAGE_NAME);

        final String processName = Utils.getProcessName(this, Process.myPid());
        LogUtil.d(Consts.LOG_TAG, ".......................startApp " + processName + ".......................");
        if (!isMainApp(getApplicationContext())) {
            LogUtil.d(Consts.LOG_TAG, ".......................startApp return here......................." + processName);
            return;
        }

        //log app info
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (null != packageInfo) {
                LogUtil.d(Consts.LOG_TAG, "app_version =" + packageInfo.versionCode + " app_version_name=" + packageInfo.versionName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
//        callTest();

        try {
            init();
            Intent intent = new Intent(this, BackgroundService.class);
            startService(intent);
            ANRManager.startANRMonitor(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public static Context getContext() {
        return sApplication;
    }

    public static MyApplication getMyApplication() {
        return (MyApplication) sApplication;
    }

    // 重置兑币器
    public void resetCoinHopper() {
    }

    public void startConfigImporter() {
        ConfigImporter.startConfig(this);
    }

    public void startLocalWebsocketServerWithCustomReconnect() {
//        LocalWebsocketServerWrapper.shareInstance(this).start();
    }

    /**
     * Retrieve an abstract level value for the overall signal strength.
     *
     * @return a single integer from 0 to 4 representing the general signal quality.
     * This may take into account many different radio technology inputs.
     * 0 represents very poor signal strength
     * while 4 represents a very strong signal strength.
     */
    public int getSignalStrenth() {
        if (NetworkProber.isWifi(getApplicationContext())) {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            int numberOfLevels = 5;
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);
            Log.d(TAG, "wifiSignalLevel = " + level);
            return level;
        }
        return mSignalStrength;
    }

    public int getAsuLevel() {
        if (NetworkProber.isWifi(getApplicationContext())) {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            Log.d(TAG, "wifiSignalLevel = " + wifiInfo.getRssi());
            return wifiInfo.getRssi();
        }
        return mAsuLevel;
    }

    public void monitorSignalStrength() {
        TelephonyManager Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Tel.listen(new PhoneStateListener() {
            private static final int INVALID = Integer.MAX_VALUE;

            private int getSignalStrengthByName(SignalStrength signalStrength, String methodName) {
                try {
                    Class classFromName = Class.forName(SignalStrength.class.getName());
                    java.lang.reflect.Method method = classFromName.getDeclaredMethod(methodName);
                    Object object = method.invoke(signalStrength);
                    return (int) object;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return INVALID;
            }

            /* 从得到的信号强度,每个tiome供应商有更新 */
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {

                super.onSignalStrengthsChanged(signalStrength);
//                if (Build.VERSION.SDK_INT >= 23) {
//                    mSignalStrength = signalStrength.getAsuLevel();
//                } else
                {
                    mAsuLevel = getSignalStrengthByName(signalStrength, "getAsuLevel");
                    mSignalStrength = SignalStrengthUtil.getLevel(mAsuLevel);
//                    LogUtil.d("signalStrength = " + mSignalStrength + " asuLevel = " + mAsuLevel);
                }
            }
        }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    private void init() {
        startConfigImporter();
        startLocalWebsocketServerWithCustomReconnect();
        resetCoinHopper();

        startSDcardListen();
        initConfig();
        monitorSignalStrength();

        if (!Consts.PRODUCTION_ON && !LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            LeakCanary.install(this);
        }
    }

    private void initConfig() {
        try {
            Config config = Config.sharedInstance(getApplicationContext());
            long time = TimeUtil.getCheckedCurrentTimeInMills(getApplicationContext());
            long elapsedTime = SystemClock.elapsedRealtime();
            LogUtil.d("elapsedTime = " + elapsedTime);
            time -= elapsedTime;

            config.saveValue(Config.LAST_REBOOT, String.valueOf(time / 1000));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void startSDcardListen() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.setPriority(1000);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SHARED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_NOFS);
        intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intentFilter.addDataScheme("file");
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.d(Consts.LOG_TAG, "MyApplication action = " + action);
            if (TextUtils.isEmpty(action)) {
                return;
            }
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                // TODO 检查u盘中包含按照格式命名的配置文件，并且文件内容合法
                openConfigUI(context);
                //todo
            }
        }
    };

    private void callTest() {
        if (Consts.PRODUCTION_ON) {
            return;
        }

//        Config config = Config.sharedInstance(this);
//        config.saveValue(Config.REBOOT_SCHEDULE, "6:30");
//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    while (true) {
//                        Thread.sleep(1000);
//                    }
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }, 10000);
    }

    public static void openConfigUI(Context context) {
        Intent mainIntent = new Intent(context, SerialPortPreferences.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(mainIntent);
    }

}
