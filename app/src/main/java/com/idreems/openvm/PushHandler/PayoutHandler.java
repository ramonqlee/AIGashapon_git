package com.idreems.openvm.PushHandler;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.idreems.openvm.Push.PushObserver;
import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.utils.LogUtil;

import org.json.JSONObject;

import android_serialport_api.vmc.AIGashaponMachine.utils.AIGashponManager;

import static com.idreems.openvm.persistence.Config.NODE_ID;

/**
 * Created by ramonqlee on 8/7/16.
 */
public class PayoutHandler implements PushObserver {
    private static final String TAG = PayoutHandler.class.getSimpleName();

    private static final String HANDLER_TYPE = "payout";

    private Context mContext;

    public PayoutHandler(Context context) {
        mContext = context;
    }

    public boolean onMessage(String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }

        try {
            JSONObject jsonObject = new JSONObject(message);
            String type = JsonUtils.getString(jsonObject, HANDLER_TYPE);
            String pushNodeId = JsonUtils.getString(jsonObject, Config.NODE_ID);
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

            payout();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private synchronized void payout() {
        Config config = Config.sharedInstance(mContext);
        String devicePath = config.getValue(Config.PC_DEVICE);
        String baudRateStr = config.getValue(Config.PC_BAUDE);
        if (TextUtils.isEmpty(devicePath) || TextUtils.isEmpty(baudRateStr) || !TextUtils.isDigitsOnly(baudRateStr)) {
            LogUtil.e(Consts.HANDLER_TAG, "illegal parameter for devicePath=" + devicePath + " baudRate=" + baudRateStr);
            return;
        }
        int baudRate = Integer.decode(baudRateStr);
        final AIGashponManager wrapper = AIGashponManager.sharedInstance(devicePath, baudRate);
        final byte address = 0;
        final byte groupNo = 1;

        final short timeout = 60;
        wrapper.open(address, groupNo, timeout);
    }
}
