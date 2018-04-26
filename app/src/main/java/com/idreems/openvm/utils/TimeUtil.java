package com.idreems.openvm.utils;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ramonqlee on 7/10/16.
 */
public class TimeUtil {
    private static long sTimeOffset;
    private static long sLastTimeInMs;

    public static void setTimeOffsetInMs(long offsetInMs) {
        sTimeOffset = offsetInMs;
    }

    // 获取校对后的时间
    public static long getCheckedCurrentTimeInMills(Context context) {
        return System.currentTimeMillis() + sTimeOffset;
    }

    public static void setLastTimeInMs(long time) {
        sLastTimeInMs = time;
    }

    public static long getLastCheckTimeInMs() {
        if (0 == sLastTimeInMs) {
            sLastTimeInMs = System.currentTimeMillis();
        }
        return sLastTimeInMs;
    }

    /**
     * "HH:mm"
     *
     * @param time
     * @return
     */
    public static long getTimeInMillis(String time) {
        SimpleDateFormat sdr = new SimpleDateFormat("HH:mm");
        try {
            Date date = sdr.parse(time);
            long l = date.getTime();
            return l;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String formatTimeWithHHMM(long timeInMills) {
        try {
            SimpleDateFormat sdr = new SimpleDateFormat("HH:mm");
            return sdr.format(new Date(timeInMills));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String formatFullTime(long timeInMills) {
        try {
            SimpleDateFormat sdr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdr.format(new Date(timeInMills));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
