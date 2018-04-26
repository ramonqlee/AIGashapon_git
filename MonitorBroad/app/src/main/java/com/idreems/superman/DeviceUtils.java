package com.idreems.superman;

import android.content.Context;
import android.os.PowerManager;

/**
 * Created by lizy on 15-7-25.
 */
public class DeviceUtils {

    /**
     * 重启机器
     */
    public static void reboot(Context context) {
        if (null == context) {
            return;
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

}
