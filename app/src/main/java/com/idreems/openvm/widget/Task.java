package com.idreems.openvm.widget;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.Push.PushDispatcher;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.network.NetworkUtils;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.TimeUtil;
import com.idreems.openvm.utils.Utils;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by ramonqlee on 06/07/2017.
 */

public class Task {
    private static final String NONE_TASK = "NONE";

    private static final String BODY_KEY = "body";
    public static final String TASK_KEY = "task";
    public static final String TOKEN_KEY = "token";

    private static String sToken;

    public static String getToken() {
        return sToken;
    }

    public static void startTaskChecker(final Context context, final String nodeId, final String password) {
        final Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtil.d(Consts.TASK_TAG, "startTaskChecker onFailure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (null == response) {
                    return;
                }
                try {
                    String body = response.body().string();
                    LogUtil.d(Consts.TASK_TAG, "startTaskChecker onResponse: " + body);
                    if (TextUtils.isEmpty(body)) {
                        return;
                    }

                    JSONObject jsonObject = new JSONObject(body);
                    JSONObject myBody = JsonUtils.getJsonObject(jsonObject, BODY_KEY);
                    if (null == myBody) {
                        return;
                    }

                    // TODO 是否会返回task？
                    String task = JsonUtils.getString(myBody, TASK_KEY);
                    if (TextUtils.isEmpty(task) || TextUtils.equals(NONE_TASK, task)) {
                        return;
                    }

                    // TODO 还是先查看token
                    sToken = JsonUtils.getString(myBody, TOKEN_KEY);
                    if (TextUtils.isEmpty(sToken)) {
                        return;
                    }

                    // 现在开始分发任务
                    PushDispatcher dispatcher = PushDispatcher.sharedInstance(Consts.PERIOD_DISPATCHER_ID);
                    dispatcher.dispatch(myBody.toString());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }
            }
        };
        // TODO 需要签名
        final long timeInSec = TimeUtil.getCheckedCurrentTimeInMills(context) / 1000;
        final String nonce = Utils.getSha1(String.valueOf(timeInSec));
        final String timestamp = String.valueOf(timeInSec);
//        sign = sha1(node_id + sha1(password) + nonce + timestamp)
        final String sign = Utils.getSha1(nodeId + Utils.getSha1(password) + nonce + timestamp);
        final String url = String.format("%s?node_id=%s&nonce=%s&timestamp=%s&sign=%s", Consts.getTaskUrl(), nodeId, nonce, timestamp, sign);
        NetworkUtils.getAndResponseOnThread(url, callback);
        LogUtil.d(Consts.TASK_TAG, "startTaskChecker url = " + url);
    }
}
