package com.idreems.openhost.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Created by lizy on 15-12-3.
 */
public class ToastUtils {

    private static Handler myHandler = new Handler(Looper.getMainLooper());

    public static void show(Context context,int msgId) {
        show(context,context.getString(msgId));
    }

    public static void show(final Context context,final String msg) {
        if (myHandler.getLooper() == Looper.myLooper()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        } else {
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
