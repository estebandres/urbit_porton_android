package com.urbit_iot.onekey.util;

/**
 * Created by andresteve07 on 12/7/17.
 */

public final class GlobalConstants {
    public static final String SP_SERIALIZED_APPUSER_KEY = "serialized_app_user";
    public static final String SP_APP_UUID_KEY = "app_uuid";
    public static final String SP_APP_UUID_HASH_KEY = "app_uuid_hash";


    public static final String LAN_DEFAULT_URL = "http://default.lan.url/rpc/";

    public static final String AP_DEFAULT_IP_ADDRESS = "192.168.4.1";

    public static final String RPC_REQ_METHOD_ATTR_NAME = "method";
    public static final String RPC_REQ_ARGS_ATTR_NAME = "args";
    public static final String RPC_REQ_TAG_ATTR_NAME = "tag";
    public static final String RPC_REQ_ID_ATTR_NAME = "id";

    public static final String RPC_SUCC_RESP_RESULT_ATTR_NAME = "result";
    public static final String RPC_SUCC_RESP_TAG_ATTR_NAME = "tag";

    public static final String RPC_FAIL_RESP_CODE_ATTR_NAME = "result";
    public static final String RPC_FAIL_RESP_TAG_ATTR_NAME = "tag";

    public static final String CREDENTIALS_REALM_SEPARATOR = ":urbit:";

}
