package com.idreems.openvm.protocols.clouds;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;

import com.idreems.openvm.MyApplication;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.utils.DeviceUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import android_serialport_api.vmc.AIGashaponMachine.Location;

/**
 * Created by ramonqlee on 7/12/16.
 */
public class ReplyGashaponMachineVariablesVariablesHandler extends ReplyMachineVariablesHandler {
    private static final String DEFAULT_JS_VERSION = "1";
    private byte mState;

    public ReplyGashaponMachineVariablesVariablesHandler(Context context) {
        super(context);

    }

    public void setState(byte state) {
        mState = state;
    }

    protected void addExtraPayloadContent(JSONObject contentJson) {
        if (null == contentJson) {
            return;
        }
        try {
            Config config = Config.sharedInstance(mContext);
            contentJson.put("mac", DeviceUtils.getPreferedMac());
            contentJson.put("last_reboot", config.getValue(Config.LAST_REBOOT));
            contentJson.put("signal_strength", MyApplication.getMyApplication().getAsuLevel());
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            if (null != packageInfo) {
                contentJson.put("app_version", String.valueOf(packageInfo.versionCode));
                contentJson.put("app_version_name", packageInfo.versionName);
            }
            String zipVersion = config.getValue(Config.SCREEN_ZIP_VERSION_ID);
            if (TextUtils.isEmpty(zipVersion)) {
                zipVersion = DEFAULT_JS_VERSION;
            }
            contentJson.put("js_version", zipVersion);

            JSONArray devicesJson = new JSONArray();
            contentJson.put("devices", devicesJson);
            final String CATEGORY = "sem";
            // 固定32组机器
            // FIXME 后续待检测出实际连接的机器
            for (int i = Location.MIN_BUS_ADDRESS; i <= Location.MAX_BUS_ADDRESS; i++) {
                JSONObject device = new JSONObject();
                devicesJson.put(device);

                device.put("category", CATEGORY);
                device.put("seq", String.valueOf(i + Location.BUS_ADDRESS_OFFSET));

                // FIXME 待确认这块的格式
                device.put("variables", new JSONArray());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
