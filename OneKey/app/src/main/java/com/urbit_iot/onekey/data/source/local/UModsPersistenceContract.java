/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.urbit_iot.onekey.data.source.local;

import android.provider.BaseColumns;

/**
 * The contract used for the db to save the tasks locally.
 */
public final class UModsPersistenceContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private UModsPersistenceContract() {}

    /* Inner class that defines the table contents */
    public static abstract class UModEntry implements BaseColumns {
        public static final String UMODS_TABLE_NAME = "umod";
        public static final String UUID_CN = "uuid";
        public static final String ALIAS_CN = "alias";
        public static final String WIFI_SSID_CN = "wifi_ssid";
        public static final String NOTIF_ENABLED_CN = "notification_enabled";
        public static final String CONNECTION_ADDRESS_CN = "connection_address";
        //public static final String AP_MODE_CN = "ap_mode";
        public static final String APP_USER_STATUS_CN = "app_user_status";
        public static final String UMOD_STATE_CN = "umod_state";
        public static final String LAST_REPORT_CN = "last_report";
        public static final String PROD_UUID_CN = "product_uuid";
        public static final String HW_VERSION_CN = "hw_version";
        public static final String SW_VERSION_CN = "sw_version";
        //public static final String IS_OPEN_CN = "is_open";
        public static final String LAST_UPDATE_DATE_CN = "last_update_date";
        public static final String MAC_ADDRESS_CN = "mac_address";
        public static final String LATITUDE_CN = "latitude";
        public static final String LONGITUDE_CN = "longitude";
        public static final String ADDRESS_TEXT_CN = "address_text";
        public static final String LAN_OPERATION_ENABLED_CN = "lan_operation_enabled";
    }
}
