package com.idreems.openvm.protocols.clouds;

import com.idreems.openvm.protocols.websocket.WebConsts;

/**
 * Created by ramonqlee on 5/16/16.
 */
public interface CloudConsts {
    public static final String TOPIC    = "topic";
    public static final String PAYLOAD  = "payload";
    public static final String MQTT_CLIENT_CONNECTION = "websoket_client_connection";

    //content level
    public static final String TIMESTAMP = "timestamp";
    public static final String CONTENT   = "content";
    public static final String STATE     = "state";
    public static final String SN        = "sn";
    public static final String NODE_NAME         = "node_name";
    public static final String NODE_PRICE        = "price";
    public static final String AMOUNT            = "amount";
    public static final String DISPENSE          = "dispense";
    public static final String REBOOT_SCHEDULE   = "reboot_schedule";
    public static final String DEVICE_SEQ = "device_seq";
    public static final String VM_ORDER_ID = "vm_order_id";
    public static final String ONLINE_ORDER_ID = "online_order_id";
    public static final String DEVICE_ORDER_ID = "device_order_id";
    public static final String DEVICE_CATEGORY = "device_category";
    public static final String SP_ID = "sp_id";
    public static final String LOCATION = "location";
    public static final String PAYER = "payer";
    public static final String PAID_AMOUNT = "paid_amount";
    public static final String CTS = "cts";
    public static final String LAST_CHECK_TIME = "last_check_time";
    public static final String LAST_ID = "last_id";
    public static final String VM_S2STATE = "s2state";

    //上传js日志
    public static final String DIR       = "dir";
    public static final String DATE      = "date";
    public static final String URL       = "url";
    public static final String TOKEN     = "token";

    public static final String CONST_POSTERS   = WebConsts.CONST_POSTERS;
}
