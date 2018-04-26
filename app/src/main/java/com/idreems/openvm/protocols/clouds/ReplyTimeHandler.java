package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import com.idreems.openvm.utils.TimeUtil;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/14/16.
 */
public class ReplyTimeHandler extends CloudBaseHandler {

    /**
     * Topic：node_id/reply_time。
     * {
     * “timestamp”: 1400000008, //指令发出的服务器unix时间戳，秒；
     * content: {
     * “cts”: 1400000000, //发送指令时，VM本地时间戳
     * }
     * }
     */

    public static final String MY_TOPIC = "reply_time";
    private static final long TIME_OUT_IN_MILLS = 10 * 1000;//暂定10s

    private long mServerTimestampInMills;
    public ReplyTimeHandler(Context context) {
        super(context);
    }


    @Override
    public String name() {
        return MY_TOPIC;
    }

    protected boolean handleContent(long timestampInSec, JSONObject content) {
        boolean r = false;
        if (null == content || timestampInSec <= 0) {
            return r;
        }

        mServerTimestampInMills = timestampInSec * 1000;

        // 根据服务器时间，校准客户端时间
        // 校对方法，记录本地和服务器时间的时间差，每次使用时，加上这个时间差
        TimeUtil.setTimeOffsetInMs(mServerTimestampInMills-System.currentTimeMillis());
        TimeUtil.setLastTimeInMs(TimeUtil.getCheckedCurrentTimeInMills(mContext));
        return true;
    }
}
