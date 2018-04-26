package com.idreems.openvm.protocols.clouds;

import android.content.Context;
import android.text.TextUtils;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;
import com.idreems.openvm.utils.LogUtil;

import org.json.JSONObject;

import android_serialport_api.vmc.AIGashaponMachine.Location;
import android_serialport_api.vmc.AIGashaponMachine.utils.AIGashponManager;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class LightOffHandler extends CloudBaseHandler {
    public static final String MY_TOPIC = "put_out";

    public LightOffHandler(Context context) {
        super(context);
    }

    @Override
    public String name() {
        return MY_TOPIC;
    }

    protected synchronized boolean handleContent(JSONObject content) {
        if (null == content) {
            return false;
        }

        Config config = Config.sharedInstance(mContext);
        String devicePath = config.getValue(Config.PC_DEVICE);
        String baudRateStr = config.getValue(Config.PC_BAUDE);
        if (TextUtils.isEmpty(devicePath) || TextUtils.isEmpty(baudRateStr) || !TextUtils.isDigitsOnly(baudRateStr)) {
            LogUtil.e(Consts.HANDLER_TAG, "illegal parameter for devicePath=" + devicePath + " baudRate=" + baudRateStr);
            return false;
        }
        int baudRate = Integer.decode(baudRateStr);
        final AIGashponManager wrapper = AIGashponManager.sharedInstance(devicePath, baudRate);

        final String device_seq = JsonUtils.getString(content, CloudConsts.DEVICE_SEQ);
        final String location = JsonUtils.getString(content, CloudConsts.LOCATION);
        if (TextUtils.isEmpty(device_seq) || TextUtils.isEmpty(location) || !TextUtils.isDigitsOnly(device_seq) || !TextUtils.isDigitsOnly(location)) {
            LogUtil.e(Consts.HANDLER_TAG, "device_seq or location not valid");
            return false;
        }

        byte addressOnline = (byte) (Byte.valueOf(device_seq) - Location.BUS_ADDRESS_OFFSET);// 地址被统一了
        addressOnline = (addressOnline > Location.MAX_BUS_ADDRESS) ? Location.MAX_BUS_ADDRESS : addressOnline;
        addressOnline = (addressOnline < Location.MIN_BUS_ADDRESS) ? Location.MIN_BUS_ADDRESS : addressOnline;
        final byte address = addressOnline;
        final byte groupNo = Byte.valueOf(location);

        // 如果正在出币中，直接返回出货结果；否则，尝试出币，通知收到出币指令，初步完成后，上传出币结果
        if (wrapper.isCheckingOut(address, groupNo)) {
            return true;
        }

        wrapper.close(address, groupNo);
        LogUtil.d(Consts.HANDLER_TAG, LightOffHandler.class.getSimpleName() + " address=" + address + " groupNo=" + groupNo);
        return true;
    }
}
