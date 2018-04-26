package com.idreems.openvm.protocols;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by ramonqlee on 5/15/16.
 */
public class JsonUtils {
    // 缺省返回""
    public static Object getObject(JSONObject object, String key) {
        if (null == object || !object.has(key)) {
            return null;
        }
        try {
            return object.get(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 缺省返回""
    public static JSONArray getJSONArray(JSONObject object, String key) {
        if (null == object || !object.has(key)) {
            return null;
        }
        try {
            return object.getJSONArray(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean getBoolean(JSONObject object, String key) {
        if (null == object) {
            return false;
        }
        try {
            return object.optBoolean(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 缺省返回""
    public static JSONObject getJsonObject(JSONObject object, String key) {
        if (null == object || !object.has(key)) {
            return null;
        }
        try {
            return object.getJSONObject(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 缺省返回""
    public static String getString(JSONObject object, String key) {
        if (null == object || !object.has(key)) {
            return "";
        }
        try {
            return object.optString(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    // 缺省返回-1
    public static long getLong(JSONObject object, String key) {
        if (null == object) {
            return -1L;
        }
        if (!object.has(key)) {
            return -1L;
        }
        try {
            return object.optLong(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1L;
    }
}
