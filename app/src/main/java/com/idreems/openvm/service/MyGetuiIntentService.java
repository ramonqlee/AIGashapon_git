package com.idreems.openvm.service;

import android.content.Context;
import android.util.Log;

import com.idreems.openvm.Push.PushDispatcher;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.utils.LogUtil;
import com.igexin.sdk.GTIntentService;
import com.igexin.sdk.PushConsts;
import com.igexin.sdk.PushManager;
import com.igexin.sdk.message.FeedbackCmdMessage;
import com.igexin.sdk.message.GTCmdMessage;
import com.igexin.sdk.message.GTTransmitMessage;
import com.igexin.sdk.message.SetTagCmdMessage;

/**
 * 继承 GTIntentService 接收来自个推的消息, 所有消息在线程中回调, 如果注册了该服务, 则务必要在 AndroidManifest中声明, 否则无法接受消息<br>
 * onReceiveMessageData 处理透传消息<br>
 * onReceiveClientId 接收 cid <br>
 * onReceiveOnlineState cid 离线上线通知 <br>
 * onReceiveCommandResult 各种事件处理回执 <br>
 */
public class MyGetuiIntentService extends GTIntentService {

    private static final String TAG = "GetuiSdkDemo";

    /**
     * 为了观察透传数据变化.
     */
    private static int cnt;

    public MyGetuiIntentService() {

    }

    @Override
    public void onReceiveServicePid(Context context, int pid) {
        Log.d(TAG, "onReceiveServicePid -> " + pid);
    }

    @Override
    public void onReceiveMessageData(Context context, GTTransmitMessage msg) {
        String appid = msg.getAppid();
        String taskid = msg.getTaskId();
        String messageid = msg.getMessageId();
        byte[] payload = msg.getPayload();
        String pkg = msg.getPkgName();
        String cid = msg.getClientId();

        // 第三方回执调用接口，actionid范围为90000-90999，可根据业务场景执行
        boolean result = PushManager.getInstance().sendFeedbackMessage(context, taskid, messageid, 90001);
        LogUtil.d(TAG, "call sendFeedbackMessage = " + (result ? "success" : "failed"));

        LogUtil.d(TAG, "onReceiveMessageData -> " + "appid = " + appid + "\ntaskid = " + taskid + "\nmessageid = " + messageid + "\npkg = " + pkg
                + "\ncid = " + cid);

        if (payload == null) {
            LogUtil.d(TAG, "receiver payload = null");
        } else {
            String data = new String(payload);
            LogUtil.d(TAG, "receiver payload = " + data);
            sendMessage(data, 0);
        }
    }

    @Override
    public void onReceiveClientId(Context context, String clientid) {
//        LogUtil.d(TAG, "onReceiveClientId -> " + "clientid = " + clientid);
//        sendMessage(clientid, 1);
    }

    @Override
    public void onReceiveOnlineState(Context context, boolean online) {
        LogUtil.d(TAG, "onReceiveOnlineState -> " + (online ? "online" : "offline"));
    }

    @Override
    public void onReceiveCommandResult(Context context, GTCmdMessage cmdMessage) {
        LogUtil.d(TAG, "onReceiveCommandResult -> " + cmdMessage);

        int action = cmdMessage.getAction();

        if (action == PushConsts.SET_TAG_RESULT) {
            setTagResult((SetTagCmdMessage) cmdMessage);
        } else if ((action == PushConsts.THIRDPART_FEEDBACK)) {
            feedbackResult((FeedbackCmdMessage) cmdMessage);
        }
    }

    private void setTagResult(SetTagCmdMessage setTagCmdMsg) {
//        String sn = setTagCmdMsg.getSn();
//        String code = setTagCmdMsg.getCode();

//        int text = R.string.add_tag_unknown_exception;
//        switch (Integer.valueOf(code)) {
//            case PushConsts.SETTAG_SUCCESS:
//                text = R.string.add_tag_success;
//                break;
//
//            case PushConsts.SETTAG_ERROR_COUNT:
//                text = R.string.add_tag_error_count;
//                break;
//
//            case PushConsts.SETTAG_ERROR_FREQUENCY:
//                text = R.string.add_tag_error_frequency;
//                break;
//
//            case PushConsts.SETTAG_ERROR_REPEAT:
//                text = R.string.add_tag_error_repeat;
//                break;
//
//            case PushConsts.SETTAG_ERROR_UNBIND:
//                text = R.string.add_tag_error_unbind;
//                break;
//
//            case PushConsts.SETTAG_ERROR_EXCEPTION:
//                text = R.string.add_tag_unknown_exception;
//                break;
//
//            case PushConsts.SETTAG_ERROR_NULL:
//                text = R.string.add_tag_error_null;
//                break;
//
//            case PushConsts.SETTAG_NOTONLINE:
//                text = R.string.add_tag_error_not_online;
//                break;
//
//            case PushConsts.SETTAG_IN_BLACKLIST:
//                text = R.string.add_tag_error_black_list;
//                break;
//
//            case PushConsts.SETTAG_NUM_EXCEED:
//                text = R.string.add_tag_error_exceed;
//                break;
//
//            default:
//                break;
//        }
//
//        Log.d(TAG, "settag result sn = " + sn + ", code = " + code + ", text = " + getResources().getString(text));
    }

    private void feedbackResult(FeedbackCmdMessage feedbackCmdMsg) {
        String appid = feedbackCmdMsg.getAppid();
        String taskid = feedbackCmdMsg.getTaskId();
        String actionid = feedbackCmdMsg.getActionId();
        String result = feedbackCmdMsg.getResult();
        long timestamp = feedbackCmdMsg.getTimeStamp();
        String cid = feedbackCmdMsg.getClientId();

        Log.d(TAG, "onReceiveCommandResult -> " + "appid = " + appid + "\ntaskid = " + taskid + "\nactionid = " + actionid + "\nresult = " + result
                + "\ncid = " + cid + "\ntimestamp = " + timestamp);
    }

    private void sendMessage(String data, int what) {
        LogUtil.d(Consts.PUSH_TAG, MyGetuiIntentService.class.getSimpleName()+" payload : " + data);
        PushDispatcher.sharedInstance().dispatch(data);
    }
}
