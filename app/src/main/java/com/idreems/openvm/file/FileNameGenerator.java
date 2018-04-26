package com.idreems.openvm.file;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ramonqlee on 6/21/16.
 */
public class FileNameGenerator {
    public static String generateKey(String imageUri) {
        return String.valueOf(imageUri.hashCode());
    }

    private static final Pattern LEGAL_KEY_PATTERN = Pattern.compile("[a-z0-9_-]{1,64}");

    public static void validateKey(String key) {
        Matcher matcher = LEGAL_KEY_PATTERN.matcher(key);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("keys must match regex [a-z0-9_-]{1,64}: \"" + key + "\"");
        }
    }

    public static String getExtensionName(String url) {
        if ((url != null) && (url.length() > 0)) {
            int dot = url.lastIndexOf('.');
            if ((dot > -1) && (dot < (url.length() - 1))) {
                String substring = url.substring(dot + 1);
                return substring;
            }
        }
        return "cng";
    }

    public static String getName(String url) {
        String suffixes = "avi|mpeg|3gp|mp3|mp4|wav|jpeg|gif|jpg|png|apk|exe|txt|html|zip|java|doc";
        Pattern pat = Pattern.compile("[\\w]+[\\.](" + suffixes + ")");//正则判断
        Matcher mc = pat.matcher(url);//条件匹配
        while (mc.find()) {
            String substring = mc.group();//截取文件名后缀名
            return substring;
        }
        return "cng";
    }

    public static String urlToFileName(String url) {
        String extendsionName = getExtensionName(url);
        String key = generateKey(url);
        return key + "." + extendsionName;
    }
}