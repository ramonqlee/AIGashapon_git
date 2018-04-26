package com.idreems.openvm.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.idreems.openvm.constant.Consts;

/**
 * Created by ramonqlee_macpro on 19/10/2017.
 */

public class ANRManager {
    private final static int mTimeoutInterval = 2000;
    private static Thread mANRMonitorThread;
    private static Handler mUIHandler = new Handler(Looper.getMainLooper());

    private static Context mContext;
    private static int mTick;
    private static final Runnable tickerRunnable = new Runnable() {
        @Override
        public void run() {
            mTick = (mTick + 1) % 10;
        }
    };

    final static Runnable mThreadRunnable = new Runnable() {
        @Override
        public void run() {
            int lastTick;
            while (!mANRMonitorThread.isInterrupted()) {
                lastTick = mTick;
                mUIHandler.post(tickerRunnable);
                try {
                    Thread.sleep(mTimeoutInterval);
                } catch (InterruptedException e) {
                    return;
                }

                // If the main thread has not handled _ticker, it is blocked. ANR.
                if (mTick == lastTick) {
                    LogUtil.d(Consts.LOG_TAG, "anr now");
                    DeviceUtils.reboot(mContext);
                    return;
                } else {
                    LogUtil.d(Consts.LOG_TAG, "anr monitor,lastTick=" + lastTick);
                }
            }
        }
    };

    public static void startANRMonitor(Context context) {
        mContext = context;
        if (null == mANRMonitorThread || mANRMonitorThread.isInterrupted()) {
            mANRMonitorThread = new Thread(mThreadRunnable);
        }
        mANRMonitorThread.start();
    }
}
