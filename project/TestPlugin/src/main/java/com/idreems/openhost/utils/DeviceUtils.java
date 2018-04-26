package com.idreems.openhost.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.idreems.openhost.utils.DirUtil.mContext;

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

    /**
     * 重启机器
     */
    public static void reboot(Context context) {
        if (null == context) {
            return;
        }

        // superman exist,call it
        if (Utils.isAppAvailable(mContext, AssetInstallApp.ASSET_APK_PACKAGE_NAME)) {
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
    }

    public static boolean checkDeviceHasNavigationBar(Context activity) {

        //通过判断设备是否有返回键、菜单键(不是虚拟键,是手机屏幕外的按键)来确定是否有navigation bar
        boolean hasMenuKey = ViewConfiguration.get(activity)
                .hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap
                .deviceHasKey(KeyEvent.KEYCODE_BACK);

        if (!hasMenuKey && !hasBackKey) {
            // 做任何你需要做的,这个设备有一个导航栏
            return true;
        }
        return false;
    }
}
