package com.idreems.openvm.PushHandler;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.idreems.openvm.MyApplication;
import com.idreems.openvm.Push.PushObserver;
import com.idreems.openvm.file.FileUtil;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.utils.TimeUtil;
import com.idreems.openvm.utils.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.idreems.openvm.persistence.Config.NODE_ID;

/**
 * Created by ramonqlee on 8/7/16.
 */
public class SnapshotHandler implements PushObserver {
    private static final String TAG = SnapshotHandler.class.getSimpleName();

    private static final String HANDLER_TYPE = "snapshot";

    private Activity mActivity;

    public SnapshotHandler(Activity activity) {
        mActivity = activity;
    }

    public boolean onMessage(String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }

        try {
            JSONObject jsonObject = new JSONObject(message);
            String type = JsonUtils.getString(jsonObject, HANDLER_TYPE);
            String pushNodeId = JsonUtils.getString(jsonObject, NODE_ID);
            Config config = Config.sharedInstance(mActivity);
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    screenshot();
                }
            }).start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private String screenshot() {
        if (null == mActivity) {
            return "";
        }

        View dView = mActivity.getWindow().getDecorView();
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();
        Bitmap bmp = dView.getDrawingCache();
        if (bmp != null) {
            try {
                //按照天进行归集,文件名采用天进行命名
                Context context = MyApplication.getMyApplication();
                String logDir = Utils.getLogCacheDir(context);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                String fileName = format.format(new Date(TimeUtil.getCheckedCurrentTimeInMills(context)));
                String filePath = String.format("%s%s%s.png", logDir, File.separator, fileName);

                FileUtil.deleteFile(filePath);
                File file = new File(filePath);
                FileOutputStream os = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 10, os);
                os.flush();
                os.close();

                bmp.recycle();
                bmp = null;

                return filePath;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dView.destroyDrawingCache();
        return "";
    }
}
