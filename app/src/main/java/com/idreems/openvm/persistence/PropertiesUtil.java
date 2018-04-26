package com.idreems.openvm.persistence;

import android.content.Context;

import com.idreems.openvm.utils.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Created by ramonqlee on 6/21/16.
 */
public class PropertiesUtil {

    public static Properties loadConfig(Context context, String file) {
        Properties properties = new Properties();
        try {
            if(Utils.isFileExist(file)) {
                FileInputStream s = new FileInputStream(file);
                properties.load(s);
                s.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static void saveConfig(Context context, String file, Properties properties) {
        try {
            FileOutputStream s = new FileOutputStream(file, false);
            properties.store(s, "");
            s.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
