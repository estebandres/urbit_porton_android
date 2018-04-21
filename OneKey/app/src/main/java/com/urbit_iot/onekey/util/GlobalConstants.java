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

    //------------COMMON RPC_FIELD_NAMES-----------------
    public static final String RPC_FIELD_NAME__METHOD   = "method";
    public static final String RPC_FIELD_NAME__METHOD_CODE   = "method_code";
    public static final String RPC_FIELD_NAME__REQUESTER   = "requester";
    public static final String RPC_FIELD_NAME__ARGS     = "args";
    public static final String RPC_FIELD_NAME__TAG      = "tag";
    public static final String RPC_FIELD_NAME__ID       = "id";
    public static final String RPC_FIELD_NAME__RESULT = "result";
    public static final String RPC_FIELD_NAME__ERROR = "error";
    public static final String RPC_FIELD_NAME__MSGE = "message";
    public static final String RPC_FIELD_NAME__CODE = "code";

    //------------RPC_METHOD_NAMES-----------------
    public static final String RPC_METHOD_NAME__FACTORY_RESET   = "Admin.FactoryReset";
    public static final String RPC_METHOD_NAME__SET_WIFI        = "Admin.SetWifi";
    public static final String RPC_METHOD_NAME__GET_USERS       = "Admin.GetUsers";
    public static final String RPC_METHOD_NAME___UPDATE_USER    = "Admin.UpdateUser";
    public static final String RPC_METHOD_NAME__DELETE_USER     = "Admin.DeleteUser";
    public static final String RPC_METHOD_NAME__ADMIN_TRIGGER   = "Admin.Trigger";

    public static final String RPC_METHOD_NAME__CREATE_USER     = "Guest.CreateUser";
    public static final String RPC_METHOD_NAME__USER_STATUS     = "Guest.UserStatus";
    public static final String RPC_METHOD_NAME__GET_INFO        = "Sys.GetInfo";

    public static final String RPC_METHOD_NAME__USER_TRIGGER    = "User.Trigger";

    //------------RPC_METHOD_CODES-----------------
    public static final int RPC_METHOD_CODE__FACTORY_RESET  = 100;
    public static final int RPC_METHOD_CODE__SET_WIFI       = 101;
    public static final int RPC_METHOD_CODE__GET_USERS      = 102;
    public static final int RPC_METHOD_CODE__UPDATE_USER    = 103;
    public static final int RPC_METHOD_CODE__DELETE_USER    = 104;
    public static final int RPC_METHOD_CODE__ADMIN_TRIGGER  = 105;

    public static final int RPC_METHOD_CODE__CREATE_USER    = 200;
    public static final int RPC_METHOD_CODE__USER_STATUS    = 201;
    public static final int RPC_METHOD_CODE__GET_INFO       = 203;

    public static final int RPC_METHOD_CODE__USER_TRIGGER   = 305;

    //----------------------------------------------------

    public static final String CREDENTIALS_REALM_SEPARATOR = ":urbit:";

    //UModViewModel
    public static final String TRIGGER_SLIDER_TEXT = "DESLICE PARA ACCIONAR";
    public static final String PENDING_SLIDER_TEXT = "ESPERANDO AUTORIZACIÓN";
    public static final String REQUEST_ACCESS_SLIDER_TEXT = "SOLICITAR ACCESO";

    public static final String ONLINE_LOWER_TEXT = "online";
    public static final String OFFLINE_LOWER_TEXT = "desconectado";

    public interface ACTION {
        String MAIN = "com.urbit-iot.onekey.action.main";
        String INIT = "com.urbit-iot.onekey.action.init";
        String UPDATE_UMODS = "com.urbit-iot.onekey.action.update";
        String ACTION_UMOD = "com.urbit-iot.onekey.action.actuate";
        String NEXT_UMOD = "com.urbit-iot.onekey.action.next";
        String STARTFOREGROUND = "com.urbit-iot.onekey.action.startforeground";
        String STOPFOREGROUND = "com.urbit-iot.onekey.action.stopforeground";
        String BACK_UMOD = "com.urbit-iot.onekey.action.back";
        String UNLOCK = "com.urbit-iot.onekey.action.unlock";
        String TRIGGER = "com.urbit-iot.onekey.action.trigger";
        String REQUEST_ACCESS = "com.urbit-iot.onekey.action.request";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }

}
