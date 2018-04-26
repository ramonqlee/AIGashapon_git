package com.idreems.openvm.Push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.utils.LogUtil;
import com.igexin.sdk.PushConsts;

/**
 * Created by ramonqlee on 6/16/16.
 */
public class PushReceiver extends BroadcastReceiver {
    /**
     * 应用未启动, 个推 service已经被唤醒,保存在该时间段内离线消息(此时 GetuiSdkDemoActivity.tLogView == null)
     */
//    public static StringBuilder payloadData = new StringBuilder();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (null == intent) {
            LogUtil.d(Consts.PUSH_TAG, "PushReceiver onReceive = null");
            return;
        }
        Bundle bundle = intent.getExtras();
        if (null == bundle) {
            LogUtil.e(Consts.PUSH_TAG, "PushReceiver bundler = null");
            return;
        }

        final int actionCmd = bundle.getInt(PushConsts.CMD_ACTION);
//        LogUtil.d(Consts.PUSH_TAG,"push actionCmd = "+actionCmd);
        switch (actionCmd) {
            case PushConsts.GET_MSG_DATA:
                // 获取透传数据
                byte[] payload = bundle.getByteArray("payload");

                // 消息分发
                if (null != payload) {
                    String data = new String(payload);
                    LogUtil.d(Consts.PUSH_TAG, "payload : " + data);
                    PushDispatcher.sharedInstance().dispatch(data);
                }
                break;

            case PushConsts.GET_CLIENTID:
                // 获取ClientID(CID)
                // 第三方应用需要将CID上传到第三方服务器，并且将当前用户帐号和CID进行关联，以便日后通过用户帐号查找CID进行消息推送
                break;

            case PushConsts.GET_SDKONLINESTATE:
                // 获取SDK在线状态
                boolean isOnline = bundle.getBoolean("onlineState");
                Log.d(Consts.PUSH_TAG, "isPushOnline = " + isOnline);
                if (isOnline) {
                    return;
                }
                break;

            default:
                break;
        }
    }
}
