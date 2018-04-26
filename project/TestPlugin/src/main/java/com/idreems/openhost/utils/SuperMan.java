package com.idreems.openhost.utils;

import android.content.Context;
import android.content.Intent;

/**
 * Created by ramonqlee on 19/07/2017.
 */

public class SuperMan {
    public static final String REBOOT_BROADCAST_ACTION = "com.idreems.superman.reboot";

    public static void callReboot(final Context context) {
        if (null == context) {
            return;
        }
        // FIXME 待增加加密信息，保证通信安全
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(REBOOT_BROADCAST_ACTION);
        context.startActivity(intent);
    }
}
