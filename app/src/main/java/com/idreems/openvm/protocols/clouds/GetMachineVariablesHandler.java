package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import com.idreems.openvm.protocols.JsonUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class GetMachineVariablesHandler extends CloudBaseHandler {
    private static final String TAG = GetMachineVariablesHandler.class.getSimpleName();
    public static final String MY_TOPIC = "get_machine_variables";

    public GetMachineVariablesHandler(Context context) {
        super(context);
    }

    @Override
    public String name() {
        return MY_TOPIC;
    }

    protected boolean handleContent(JSONObject content) {
        if (null == content) {
            return false;
        }

        String sn = JsonUtils.getString(content, CloudConsts.SN);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(CloudConsts.SN, sn);

        // 直接返回最近一次的状态,如果碰到异常的情况，则主动发起查询，返回后主动上报，更新状态
        // 注册专用的handler，然后用这个处理器组装上传的数据
        ReplyGashaponMachineVariablesVariablesHandler handler = new ReplyGashaponMachineVariablesVariablesHandler(mContext);
//        final CoinHopperWrapper wrapper = CoinHopperWrapper.sharedInstance(mContext);
//        byte latestStatus = wrapper.getLastestStatus();
//        handler.setState(latestStatus);
        MqttReplyHandlerMgr.registerHandler(handler);
        MqttReplyHandlerMgr.replyWith(mContext, ReplyGashaponMachineVariablesVariablesHandler.MY_TOPIC, map);
//
//        //  状态正常
//        if (Status.isSuccessful(latestStatus)) {
//            return true;
//        }
//
//        Log.e(TAG, "abnormal cp state,reconfirm it with inquiry");
//        // 状态异常，确认后，上报上传底层设备状态变化事件
//        Ack.AckCallback ackCallback = new Ack.AckCallback() {
//            @Override
//            public void onStatus(byte sn, byte status, byte address, byte lowDispense, byte highDispense) {
//                if (Status.isBusy(status)) {
//                    return;
//                }
//                // 移除回调
//                wrapper.removeCallback(this);
//
//                // 注册专用的handler，然后用这个处理器组装上传的数据
//                UploadCoinHopperVMCStateHandler handler = new UploadCoinHopperVMCStateHandler(mContext);
//                handler.setState(status);
//                MqttReplyHandlerMgr.registerHandler(handler);
//                MqttReplyHandlerMgr.replyWith(mContext, UploadCoinHopperVMCStateHandler.MY_TOPIC, new HashMap<String, String>());
//                LogUtil.d(TAG, "UploadCoinHopperVMCStateHandler replyWith in GetMachineVariablesHandler");
//            }
//        };
//        wrapper.addCallback(ackCallback);
//        wrapper.inquireStateAsync();
        return true;
    }
}
