package com.idreems.openvm.PushHandler;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.MyApplication;
import com.idreems.openvm.Push.PushObserver;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.file.FileUtil;
import com.idreems.openvm.file.ZipUtil;
import com.idreems.openvm.network.NetworkUtils;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.utils.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Response;

import static com.idreems.openvm.persistence.Config.NODE_ID;

/**
 * Created by ramonqlee on 8/7/16.
 */
public class PostLogHandler implements PushObserver {
    private static final String TAG = PostLogHandler.class.getSimpleName();

    private static final String FORMAL_BASE_URL = "http://ecs3.fshd.com/";//"http://fshd.fensihudong.com/"; // 正式环境服务器地址

    private static String getAPIUrl() {
        return FORMAL_BASE_URL.concat("index.php");
    }

    private static final String HANDLER_TYPE = "postlog";

    private Context mContext;

    private static final long MIN_POST_TIME_INTERVAL = 2 * 60 * 1000;//最小上传间隔:10分钟
    private static long sLastPostTime;

    public PostLogHandler(Context context) {
        mContext = context;
    }

    public boolean onMessage(String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }

        // 是否太过频繁了
        if (sLastPostTime > 0) {
            if (Math.abs(sLastPostTime - System.currentTimeMillis()) < MIN_POST_TIME_INTERVAL) {
                LogUtil.d(Consts.PUSH_TAG, "post log too freq,ignore");
                return true;
            }
        }

        try {
            JSONObject jsonObject = new JSONObject(message);
            String type = JsonUtils.getString(jsonObject, HANDLER_TYPE);
            String pushNodeId = JsonUtils.getString(jsonObject, NODE_ID);
            Config config = Config.sharedInstance(mContext);
            final String localNodeId = config.getValue(NODE_ID);
            Log.d(TAG, "nodeid = " + pushNodeId + " localNodeId=" + localNodeId + " pushType=" + type);
            if (TextUtils.isEmpty(localNodeId) || TextUtils.isEmpty(pushNodeId)) {
                return false;
            }

            if (!TextUtils.equals(localNodeId, pushNodeId)) {
                return false;
            }

            if (TextUtils.isEmpty(type)) {
                return false;
            }
            postLog(mContext, localNodeId);
            if (0 == sLastPostTime) {
                sLastPostTime = System.currentTimeMillis();
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static void postLog(final Context context, final String fileName) {
        LogUtil.d(TAG, "prepare to post log file");
        // 上传日志文件message
        Map<String, String> parameters = new HashMap<>();
        parameters.put("m", "wechat");
        parameters.put("c", "push");
        parameters.put("a", "uploadFile");

        String url = NetworkUtils.getSignedUrl(getAPIUrl(), parameters);
        final String logFilePath = zipLogFiles(context, fileName);
        if (TextUtils.isEmpty(logFilePath)) {
            return;
        }

        final MediaType MEDIA_TYPE_OCTET
                = MediaType.parse("application/octet-stream");
        NetworkUtils.postFile(logFilePath, MEDIA_TYPE_OCTET, url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                FileUtil.deleteFile(logFilePath);
                LogUtil.d(TAG, "post log file fail" + ((null != e) ? e.getMessage() : ""));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                FileUtil.deleteFile(logFilePath);
                LogUtil.d(TAG, "post log file done");
            }
        });
    }

    // 将日志文件打包，返回打包后的路径名
    public static String zipLogFiles(Context context, String fileNamePrefix) {
        if (null == context) {
            return "";
        }
        try {
            String logDir = Utils.getLogCacheDir(MyApplication.getContext());
            File zippedFile = context.getFilesDir().createTempFile(fileNamePrefix, ".zip");
            ZipUtil.zip(logDir, zippedFile.getAbsolutePath());
            return zippedFile.getAbsolutePath();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }
}
