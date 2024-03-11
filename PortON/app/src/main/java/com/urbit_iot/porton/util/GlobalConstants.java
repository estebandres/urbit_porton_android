package com.urbit_iot.porton.util;

/**
 * Created by andresteve07 on 12/7/17.
 */

public final class GlobalConstants {
    public static final String URBIT_PREFIX = "urbit-";
    public static final String DEVICE_UUID_REGEX = "([0-9A-Fa-f]{6})";

    public static final String LOGGLY_TOKEN = "cc7be772-d317-4820-9a71-02fd2a60eeb8";

    public static final String SP_KEY__APPUSER = "serialized_app_user";
    public static final String SP_KEY__APP_UUID = "app_uuid";
    public static final String SP_KEY__UUID_HASH_KEY = "app_uuid_hash";
    public static final String SP_KEY__UNSENT_LOGS = "serialized_unsent_logs";


    public static final String LAN_DEFAULT_URL = "http://default.lan.url/rpc/";

    public static final String AP_DEFAULT_IP_ADDRESS = "192.168.4.1";
    //public static final String AP_DEFAULT_IP_ADDRESS = "mocked.apmode.module";
    public static final Integer UMOD__TCP_ECHO_PORT = 7777;

    public static final String FIRMWARE_SERVER__IP_ADDRESS = "192.168.0.3";
    public static final String FIRMWARE_SERVER__PORT = "3289";
    public static final String MQTT_BROKER__IP_ADDRESS = "devbroker.urbit-iot.tk";
    public static final Integer MQTT_BROKER__PORT = 1883;

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
    public static final String RPC_METHOD_NAME___SET_GATE_STATUS      = "Admin.SetGateStatus";
    public static final String RPC_METHOD_NAME___ADMIN_CREATE_USER    = "Admin.CreateUser";
    public static final String RPC_METHOD_NAME__ENABLE_UPDATE   = "Admin.EnableUpdate";
    public static final String RPC_METHOD_NAME__ADMIN_GET_GATE_STATUS = "Admin.GetGateStatus";

    public static final String RPC_METHOD_NAME__CREATE_USER     = "Guest.CreateUser";
    public static final String RPC_METHOD_NAME__USER_STATUS     = "Guest.UserStatus";
    public static final String RPC_METHOD_NAME__GET_INFO        = "Sys.GetInfo";

    public static final String RPC_METHOD_NAME__USER_TRIGGER    = "User.Trigger";
    public static final String RPC_METHOD_NAME__USER_GET_GATE_STATUS = "User.GetGateStatus";

    //------------RPC_METHOD_CODES-----------------
    public static final int RPC_METHOD_CODE__FACTORY_RESET      = 100;
    public static final int RPC_METHOD_CODE__SET_WIFI           = 101;
    public static final int RPC_METHOD_CODE__GET_USERS          = 102;
    public static final int RPC_METHOD_CODE__UPDATE_USER        = 103;
    public static final int RPC_METHOD_CODE__DELETE_USER        = 104;
    public static final int RPC_METHOD_CODE__ADMIN_TRIGGER      = 105;
    public static final int RPC_METHOD_CODE__ADMIN_CREATE_USER  = 106;
    public static final int RPC_METHOD_CODE__ENABLE_UPDATE      = 107;
    public static final int RPC_METHOD_CODE__SET_GATE_STATUS    = 108;
    public static final int RPC_METHOD_CODE__ADMIN_GET_GATE_STATUS = 109;

    public static final int RPC_METHOD_CODE__CREATE_USER        = 200;
    public static final int RPC_METHOD_CODE__USER_STATUS        = 201;
    public static final int RPC_METHOD_CODE__GET_INFO           = 203;

    public static final int RPC_METHOD_CODE__USER_TRIGGER       = 300;
    public static final int RPC_METHOD_CODE__USER_GET_GATE_STATUS = 301;

    //----------------------------------------------------

    public static final String CREDENTIALS_REALM_SEPARATOR = ":urbit:";

    //UModViewModel
    public static final String TRIGGER_SLIDER_TEXT = "DESLICE PARA ACCIONAR";
    public static final String PENDING_SLIDER_TEXT = "ESPERANDO AUTORIZACIÓN";
    public static final String REQUEST_ACCESS_SLIDER_TEXT = "SOLICITAR ACCESO";

    public static final String ONLINE_TAG__TEXT = "online";
    public static final String STORED_LOWER_TEXT = "recordado";
    public static final String ONGOING_NOTIFICATION_STATE_KEY = "app_settings__ongoing_notification_state";
    public static final String NOTIFICATIONS_CHANNEL_ID = "urbit_porton_app_notif_channel_id";
    public static final String OPEN_GATE__TAG_TEXT = "ABIERTO";
    public static final String OPENING_GATE__TAG_TEXT = "ABRIENDO";
    public static final String CLOSED_GATE__TAG_TEXT = "CERRADO";
    public static final String CLOSING_GATE__TAG_TEXT = "CERRANDO";
    public static final String UNKNOWN_GATE_STATUS__TAG_TEXT = "DESCONOCIDO";
    public static final String OFFLINE_TAG__TEXT = "OFFLINE";


    public interface ACTION {
        String MAIN = "com.urbit-iot.porton.action.main";
        String INIT = "com.urbit-iot.porton.action.init";
        String REFRESH_UMODS = "com.urbit-iot.porton.action.refresh";
        String ACTION_UMOD = "com.urbit-iot.porton.action.actuate";
        String NEXT_UMOD = "com.urbit-iot.porton.action.next";
        String STARTFOREGROUND = "com.urbit-iot.porton.action.startforeground";
        String STOPFOREGROUND = "com.urbit-iot.porton.action.stopforeground";
        String BACK_UMOD = "com.urbit-iot.porton.action.back";
        String UNLOCK = "com.urbit-iot.porton.action.unlock";
        String TRIGGER = "com.urbit-iot.porton.action.trigger";
        String REQUEST_ACCESS = "com.urbit-iot.porton.action.request";
        String WIFI_CONNECTED = "com.urbit-iot.porton.action.wifienabled";
        String WIFI_UNUSABLE = "com.urbit-iot.porton.action.wifiunusable";
        String LAUNCH_WIFI_SETTINGS = "com.urbit-iot.porton.action.launchwifisettings";
        String SHUTDOWN_SERVICE = "com.urbit-iot.porton.action.shutdownservice";
        String REFRESH_ON_CACHED = "com.urbit-iot.porton.action.refreshoncached";
    }

    public static int NOTIFICATION_ID = 101;

}
