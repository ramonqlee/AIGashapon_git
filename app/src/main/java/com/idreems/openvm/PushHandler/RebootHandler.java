package com.idreems.openvm.PushHandler;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.Push.PushObserver;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.utils.DeviceUtils;
import com.idreems.openvm.utils.LogUtil;
import com.idreems.openvm.widget.Task;

import org.json.JSONObject;

/**
 * Created by ramonqlee on 8/7/16.
 */
public class RebootHandler implements PushObserver {
    private static final String TAG = RebootHandler.class.getSimpleName();

    private static final String HANDLER_TYPE = "REBOOT";
    private Context mContext;

    public RebootHandler(Context context) {
        mContext = context;
    }

    public boolean onMessage(String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }

        try {
            JSONObject jsonObject = new JSONObject(message);
            String type = JsonUtils.getString(jsonObject, Task.TASK_KEY);
            String nodeId = JsonUtils.getString(jsonObject, Config.NODE_ID);
            if (TextUtils.isEmpty(type) || !TextUtils.equals(type, HANDLER_TYPE)) {
                return false;
            }

            String myNodeId = Config.sharedInstance(mContext).getValue(Config.NODE_ID);
            if (TextUtils.isEmpty(nodeId) || !TextUtils.equals(nodeId, myNodeId)) {
                return false;
            }

            LogUtil.d(Consts.TASK_TAG, "RebootHandler");

            DeviceUtils.reboot(mContext);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
