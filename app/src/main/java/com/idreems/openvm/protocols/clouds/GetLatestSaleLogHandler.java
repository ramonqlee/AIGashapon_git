package com.idreems.openvm.protocols.clouds;

import android.content.Context;

import com.idreems.openvm.persistence.Config;
import com.idreems.openvm.protocols.JsonUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ramonqlee on 5/18/16.
 */
public class GetLatestSaleLogHandler extends CloudBaseHandler {
    public static final String MY_TOPIC = "get_last_sale_log_id";

    public GetLatestSaleLogHandler(Context context) {
        super(context);
    }

    @Override
    public String name() {
        return MY_TOPIC;
    }

    protected boolean handleContent(JSONObject content) {
        boolean r = false;
        if (null == content) {
            return r;
        }

        try {
            // 更新状态，并通知云端收到了
            String sn = JsonUtils.getString(content, CloudConsts.SN);
            Config config = Config.sharedInstance(mContext);
            String latestDeliverLogSN = config.getValue(Config.LATEST_DELIVER_LOG);

            Map<String, String> map = new HashMap<String, String>();
            map.put(CloudConsts.SN, sn);
            map.put(CloudConsts.LAST_ID,latestDeliverLogSN);

            MqttReplyHandlerMgr.replyWith(mContext, ReplyLatestSaleLogHandler.MY_TOPIC, map);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
