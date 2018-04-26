package com.idreems.openvm.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;

import com.idreems.openvm.constant.Consts;
import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.websocket.ReplyVMState;

/**
 * Created by ramonqlee on 7/10/16.
 */
public class ConfigImporter {
    // 解析外部的配置文件，导入到内部使用的数据中
    public static void startConfig(final Context context) {
        if (null == context) {
            return;
        }

        Config config = Config.sharedInstance(context);
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (null != packageInfo) {
                config.saveValue(Config.APP_VERSION, String.valueOf(packageInfo.versionCode));
                config.saveValue(Config.APP_VERSION_NAME, packageInfo.versionName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 迁移老数据
        final String oldNodeId = config.getValue(Config.NODE_ID_DEPRECATED);
        String nodeId = config.getValue(Config.NODE_ID);
        if (TextUtils.isEmpty(nodeId) && !TextUtils.isEmpty(oldNodeId)) {
            config.saveValue(Config.NODE_ID, oldNodeId);
            nodeId = oldNodeId;
        }

        // 点位id，用户名，密码，设备已经配置
        String password = config.getValue(Config.PASSWORD);
        String device = config.getValue(Config.PC_DEVICE);
        if (TextUtils.isEmpty(device)) {
            config.saveValue(Config.PC_DEVICE, Consts.DEFAULT_COM_DEVICE);
        }

        final String nodeName = config.getValue(Config.NODE_NAME);
        if (!Consts.PRODUCTION_ON) {
            if (TextUtils.isEmpty(nodeName)) {
                config.saveValue(Config.VM_SATE, ReplyVMState.STATE_ON);
                config.saveValue(Config.NODE_NAME, "华联商厦");
                config.saveValue(Config.NODE_PRICE, String.valueOf(1000));
                config.saveValue(Config.REBOOT_SCHEDULE, "05:00");
            }

            if (TextUtils.isEmpty(nodeId)) {
                config.saveValue(Config.NODE_ID, Consts.getNodeId());
            }
            if (TextUtils.isEmpty(password)) {
                config.saveValue(Config.PASSWORD, Consts.getPassword());
            }
        }

        // 设置缺省的参数
        config.saveValue(Config.PC_BAUDE, Consts.DEFAULT_BAUD_RATE);
    }
}
