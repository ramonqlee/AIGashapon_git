package com.idreems.openhost.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Created by ramonqlee on 4/19/16.
 */
public class Config {
    public static final String SERVER_ENV_KEY = "server_env";// 后续用于识别本地环境
    public static final String WIFI_MAC_KEY = "wifi_mac";
    public static final String ETHERNET_MAC_KEY = "ethernet_mac";
    public static final String MAC_TYPE_KEY = "mac_type";// 用于记录mac地址的类型，取值分别为WIFI_MAC_KEY，和ETHERNET_MAC_KEY
    public static final String CURRENT_APK_PATH = "current_apk_path";//最新的apk的位置

    private static final String PREF_FILE_NAME = "sharedPref";

    private SharedPreferences mPref;
    private static Config sConfig;

    public static  Config sharedInstance(Context context)
    {
        if (null != sConfig)
        {
            return sConfig;
        }

        if (null == context)
        {
            return null;
        }
        sConfig = new Config(context);
        return sConfig;
    }

    private Config(Context context)
    {
        mPref = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public String getValue(String key) {
        if (TextUtils.isEmpty(key))
        {
            return "";
        }
        return mPref.getString(key, "");
    }

    public void saveValue(String key,String value) {
        if (TextUtils.isEmpty(key))
        {
            return;
        }
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(key, value);
        editor.apply();
    }
}
