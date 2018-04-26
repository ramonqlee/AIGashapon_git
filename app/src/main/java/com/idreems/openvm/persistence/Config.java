package com.idreems.openvm.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Created by ramonqlee on 4/19/16.
 */
public class Config {
    public static final String WIFI_MAC_ID_KEY = "WIFI_MAC_ID_KEY";
    public static final String ETHERNET_MAC_ID_KEY = "ETHERNET_MAC_ID_KEY";

    public static final String SETTING_FILE_NAME = "openvm.txt";// 售货机初始化配置文件
    public static final String SERVER_ENV_KEY = "server_env";// 后续用于识别本地环境
    public static final String APP_VERSION = "appVersionCode";// 应用版本号
    public static final String APP_VERSION_NAME = "appVersionName";// 应用版本
    public static final String NODE_ID_DEPRECATED = "note_id";// 点位id，以前错误的id
    public static final String NODE_ID = "node_id";// 点位id
    public static final String NODE_NAME = "node_name";
    public static final String NODE_PRICE = "price";
    public static final String REBOOT_SCHEDULE = "reboot_schedule";
    public static final String LAST_REBOOT = "last_reboot";

    public static final String PASSWORD = "passcode";// 密码
    public static final String VM_SATE = "vm_stare";// 售货机状态
    public static final String AD_CONTENT = "posters";// vm收到的广告
    public static final String CLIENT_2_SERVER_TIME_OFFSET = "timeOffset";// 客户端和服务器端的时间差（client-server）
    public static final String LAST_CHECK_TIME = "last_check_time";
    public static final String LATEST_DELIVER_LOG = "latest_deliver_log";

    public static final String JS_VERSION = "js_version";
    public static final String JS_INDEX_PATH = "js_index_path";

    // coin hopper相关
    public static final String COIN_HOPPER_OFF_REASON = "COIN_HOPPER_OFF_REASON";
    public static final String COIN_HOPPER_SN = "COIN_HOPPER_SN";
    public static final String COIN_HOPPER_COM = "COIN_HOPPER_COM";
    public static final String PC_DEVICE = "DEVICE";
    public static final String PC_BAUDE = "BAUDRATE";

    public static final String REBOOT_COUNT_KEY = "REBOOT_COUNT_KEY";
    public static final String REBOOT_DATE_KEY = "REBOOT_DATE_KEY";

    public static final String SCREEN_ZIP_VERSION_ID = "SCREEN_ZIP_VERSION_ID";// 首页轮播图压缩包的版本
    public static final String DOWNLOADED_APP_VERSION = "DOWNLOADED_APP_VERSION";

    private static final String PREF_FILE_NAME = "sharedPref";

    private SharedPreferences mPref;
    private static Config sConfig;

    public static Config sharedInstance(Context context) {
        if (null != sConfig) {
            return sConfig;
        }

        if (null == context) {
            return null;
        }
        sConfig = new Config(context);
        return sConfig;
    }

    private Config(Context context) {
        mPref = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public String getValue(String key) {
        if (TextUtils.isEmpty(key)) {
            return "";
        }
        return mPref.getString(key, "");
    }

    public void saveValue(String key, String value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(key, value);
        editor.apply();
    }
}
