package com.idreems.openvm.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.idreems.openvm.MyApplication;

/**
 * Created by ramonqlee on 7/31/16.
 */
public class ToastUtils {

    private static Handler myHandler = new Handler(Looper.getMainLooper());

    public static void show(int msgId) {
        show(MyApplication.getContext().getString(msgId));
    }

    public static void show(final String msg) {
        if (myHandler.getLooper() == Looper.myLooper()) {
            Toast.makeText(MyApplication.getContext(), msg, Toast.LENGTH_LONG).show();
        } else {
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MyApplication.getContext(), msg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
