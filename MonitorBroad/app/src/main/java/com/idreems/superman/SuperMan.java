package com.idreems.superman;

import android.content.Context;
import android.content.Intent;

/**
 * Created by ramonqlee on 19/07/2017.
 */

public class SuperMan {
    public static final String REBOOT_BROADCAST_ACTION = "com.idreems.superman.reboot";
    public static final String INSTALLAPP_BROADCAST_ACTION = "com.idreems.superman.install";
    public static final String FILE_PATH = "filePath";
    public static final String PACKAGE_NAME = "packageName";

    public static void callReboot(final Context context) {
        if (null == context) {
            return;
        }
        // FIXME 待增加加密信息，保证通信安全
        context.sendBroadcast(new Intent(REBOOT_BROADCAST_ACTION));
    }
}
