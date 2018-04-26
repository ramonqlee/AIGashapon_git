package com.idreems.openvm.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.idreems.openvm.MyApplication;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.paho.mqtt.MQTTMgr;
import com.idreems.openvm.persistence.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lizy on 15-7-25.
 */
public class DeviceUtils {
    private static final String ZERO_MAC = "00:00:00:00:00:00";

    //
    public static boolean isLegalMac(String mac) {
        if (TextUtils.isEmpty(mac)) {
            return false;
        }

        if (TextUtils.equals(ZERO_MAC, mac)) {
            return false;
        }

        if (ZERO_MAC.length() != mac.length()) {
            return false;
        }
        return true;
    }

    // 获取ethernet的mac地址（一次获取，便保存到本地文件中）
    public static String getEthernetMac() {
        Config config = Config.sharedInstance(MyApplication.getMyApplication());
        String ethernetMac = config.getValue(Config.ETHERNET_MAC_ID_KEY);
        if (!TextUtils.isEmpty(ethernetMac)) {
//            Const.logD("getEthernetMac = " + ethernetMac);
            return ethernetMac;
        }

        String mac = "";
        try {
            Process p = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address");
            InputStream is = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bf = new BufferedReader(isr);
            String line = null;
            if ((line = bf.readLine()) != null) {
                mac = line;
            }
            bf.close();
            isr.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mac = TextUtils.isEmpty(mac) ? mac : mac.toUpperCase();
//        Const.logD("getEthernetMac = " + mac);
        config.saveValue(Config.ETHERNET_MAC_ID_KEY, mac);
        return mac;
    }

    // 获取wifi的mac地址（一次获取，便保存到本地文件中）
    private static String getMacAddress() {
        Config config = Config.sharedInstance(MyApplication.getMyApplication());
        String wifiMac = config.getValue(Config.WIFI_MAC_ID_KEY);
        if (!TextUtils.isEmpty(wifiMac)) {
            return wifiMac;
        }

        WifiManager wifiMng = (WifiManager) MyApplication.getMyApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfor = wifiMng.getConnectionInfo();
        String mac = wifiInfor.getMacAddress();

        mac = TextUtils.isEmpty(mac) ? mac : mac.toUpperCase();
//        Const.logD("wifiMac = " + mac);
        config.saveValue(Config.WIFI_MAC_ID_KEY, mac);

        return mac;
    }

    public static String getWifiMac() {
        return getMacAddress();
    }


    // 获取mac地址，优先获取有线mac地址，其次获取wifi mac地址
    public static String getPreferedMac() {
        String ethernetMac = getEthernetMac();
        if (isLegalMac(ethernetMac)) {
            return ethernetMac;
        }
        return getWifiMac();
    }

    /**
     * 获取手机型号
     *
     * @return
     */
    public static String model() {
        return Build.MODEL;
    }

    public static String osSDK() {
        return Build.VERSION.SDK;
    }

    public static boolean isEmulator() {
        // FIXME 此种方法判断，待改进（Build.FINGERPRINT unknown经验证，不准确）
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static String getIMEI(Context context) {
        if (null == context) {
            return "";
        }
        return ((TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE)).getDeviceId();
    }

    public static Display getScreenMetrics(Context context) {
        if (null == context) {
            return null;
        }
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay();
//        int width = wm.getDefaultDisplay().getWidth();//屏幕宽度
//        int height = wm.getDefaultDisplay().getHeight();//屏幕高度
    }

    public static void takeSnapShot(String filePath, Activity activity) {
        if (TextUtils.isEmpty(filePath) || null == activity) {
            return;
        }

        View v = activity.getWindow().getDecorView().getRootView();
        v.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false);

        OutputStream out = null;
        File imageFile = new File(filePath);

        try {
            out = new FileOutputStream(imageFile);
            // choose JPEG format
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
        } catch (FileNotFoundException e) {
            // manage exception
        } catch (IOException e) {
            // manage exception
        } finally {

            try {
                if (out != null) {
                    out.close();
                }

            } catch (Exception exc) {
            }

        }
    }

    private static Timer sDelayRebootTimer;

    private static void delayReboot(final Context context) {
        if (null != sDelayRebootTimer) {
            sDelayRebootTimer.cancel();
        }
        sDelayRebootTimer = new Timer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                LogUtil.d(Consts.LOG_TAG, "delayReboot now");
                reboot(context);
            }
        };
        sDelayRebootTimer.schedule(task, 60 * 1000);
    }

    public static boolean sDelayRebootApp;

    public static void restartApp(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                restartAppImp(context);
            }
        }).start();
    }

    private static void restartAppImp(Context context) {
        LogUtil.d(Consts.LOG_TAG, "prepare to restartApp");
        if (null == context) {
            return;
        }

        // 如果正在出货，则不重启
        if (MQTTMgr.sharedInstance(context).getDeliveringSize() > 0) {
            LogUtil.d(Consts.LOG_TAG, "deliveringSize not empty,delay rebootApp");
            sDelayRebootApp = true;
            return;
        }
        System.exit(0);
    }

    /**
     * 重启机器
     */
    public static void reboot(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                rebootImp(context);
            }
        }).start();
    }

    private static void rebootImp(final Context context) {
        if (!Consts.PRODUCTION_ON) {
            LogUtil.d(Consts.LOG_TAG, "reboot now in test mode,but no effect");
            return;
        }
        LogUtil.d(Consts.LOG_TAG, "prepare to reboot");
        if (null == context) {
            return;
        }

        // 如果正在出货，则不重启
        if (MQTTMgr.sharedInstance(context).getDeliveringSize() > 0) {
            LogUtil.d(Consts.LOG_TAG, "deliveringSize not empty,delay reboot");
            delayReboot(context);
            return;
        }

        // superman exist,call it
        if (Utils.isAppAvailable(context, AssetInstallApp.ASSET_APK_PACKAGE_NAME)) {
            SuperMan.callReboot(context);
//            return;
        }

        String cmd = "su -c reboot";
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 尝试另外的方式
        try {
            PowerManager pManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            pManager.reboot(null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 失败了，通过重启app方式
        restartApp(context);
    }

    public static void openAirplaneModeOn(Context context, boolean enabling) {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, enabling ? 1 : 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        context.sendBroadcast(intent);

    }
}
