package com.idreems.openvm.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.utils.LogUtil;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by wxliao on 2016/2/22.
 */
public class DigitalClock extends TextView {
    private final static String m12 = "h:mm aa";//h:mm:ss aa
    private final static String m24 = "k:mm";//k:mm:ss
    Calendar mCalendar;
    String mFormat;
    private long mTimeOff;// 系统时间和服务器时间的时间差

    public DigitalClock(Context context) {
        super(context);
        initClock(context);
    }

    public DigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        initClock(context);
    }

    private void initClock(Context context) {
        if (mCalendar == null) {
            mCalendar = Calendar.getInstance();
        }
        // FIXME设定时区(目前设定为北京时间)
        mCalendar.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        setFormat();
        updateTime();
    }

    public void setTimeOffsetInMillis(long milliseconds) {
        // 记录设置的当前时间，和当前系统的时间，计算出当前的时间差，后续显示时间时，使用该时间差,显示当前控件的时间
        mTimeOff = milliseconds;
    }

    private final BroadcastReceiver mTimeTickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent || !TextUtils.equals(Intent.ACTION_TIME_TICK, intent.getAction())) {
                LogUtil.d(Consts.LOG_TAG, "DigitalClock broadcast action =" + intent.getAction());
                return;
            }

            updateTime();
        }
    };


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerTimeTick();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterTimeTick();
    }

    /**
     * Pulls 12/24 mode from system settings
     */
    private boolean get24HourMode() {
        return DateFormat.is24HourFormat(getContext());
    }

    private void setFormat() {
        if (get24HourMode()) {
            mFormat = m24;
        } else {
            mFormat = m12;
        }
    }

    private void registerTimeTick() {
        if (null == mTimeTickReceiver) {
            return;
        }
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            getContext().registerReceiver(mTimeTickReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unregisterTimeTick() {
        if (null == mTimeTickReceiver) {
            return;
        }
        try {
            getContext().unregisterReceiver(mTimeTickReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTime() {
        setFormat();
//        String timeSave = String.valueOf((System.currentTimeMillis() + mTimeOff)/1000);
//        Config config = Config.sharedInstance(getContext());
//        config.saveValue(Config.TIME,timeSave);
        mCalendar.setTimeInMillis(System.currentTimeMillis() + mTimeOff);
        CharSequence time = DateFormat.format(mFormat, mCalendar);
        setText(time);
    }

}
