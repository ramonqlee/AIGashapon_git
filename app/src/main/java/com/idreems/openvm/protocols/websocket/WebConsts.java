package com.idreems.openvm.protocols.websocket;

/**
 * Created by ramonqlee on 5/16/16.
 */
public interface WebConsts {
    // parameter passing level
    public static final String CONST_MESSAGE = "message";
    public static final String CONST_SERVER_WEBSOCKET = "server_websocket";

    //protocol level
    public static final String CONST_INSTRUCTION = "instruction";
    public static final String CONST_CONTENT = "content";

    //content level
    public static final String CONST_NODE_ID = "node_id";
    public static final String CONST_NODE_NAME = "node_name";
    public static final String CONST_NODE_PRICE = "price";
    public static final String CONST_STATE = "state";
    public static final String SIGNAL_STRENGTH = "signal_strength";

    public static final String CONST_POSTERS   = "posters";

    // 写本地日志
    public static final String CONST_DIR    = "dir";
    public static final String CONST_FILE   = "file";
    public static final String CONST_TEXT   = "text";

    //请求本地文件
    public static final String CONST_URL      = "url";
    public static final String CONST_EXPIRE   = "expires";
    public static final String CONST_DURATION   = "duration";
}
