package com.idreems.openvm.protocols;

import android.text.TextUtils;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.utils.LogUtil;

import java.util.ArrayList;

/**
 * Created by ramonqlee on 5/14/16.
 */
public class ProtocolPool {
    private ArrayList<ProtocolBaseHandler> mProtocolHandlerList = new ArrayList<ProtocolBaseHandler>();

    public int size() {
        return mProtocolHandlerList.size();
    }

    public void add(ProtocolBaseHandler handler) {
        if (null == handler) {
            return;
        }

        // 添加协议处理器，如果类名相同，则认为已经添加过了
        for (ProtocolBaseHandler ele : mProtocolHandlerList) {
            if (null == ele) {
                continue;
            }
            if (TextUtils.equals(ele.getClass().getSimpleName(), handler.getClass().getSimpleName())) {
                LogUtil.d(Consts.LOG_TAG_PROTOCOL,ele.getClass().getSimpleName()+" existed,ignore register");
                return;
            }
        }

        mProtocolHandlerList.add(handler);
    }

    public void clear() {
        mProtocolHandlerList.clear();
    }

    public synchronized void run(Object object) {
        // 寻找匹配的协议处理器
        boolean r = false;
        for (ProtocolBaseHandler ele : mProtocolHandlerList) {
            if (null == ele) {
                continue;
            }
            r = ele.handle(object);
            if (r) {
                LogUtil.d(Consts.LOG_TAG_PROTOCOL, "run with " + ele.getClass().getSimpleName());
                return;
            }
        }

        if (!r) {
            LogUtil.d(Consts.LOG_TAG, " ignore this object:" + object);
        }
    }
}
