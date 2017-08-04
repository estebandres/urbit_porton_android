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
        public static final String TABLE_NAME = "umod";
        public static final String COLUMN_NAME_UUID = "uuid";
        public static final String COLUMN_NAME_ALIAS = "alias";
        public static final String COLUMN_NAME_NOTIF_EN = "notification_enabled";
        public static final String COLUMN_NAME_LAN_IP_ADDRESS = "lan_ip_address";
        public static final String COLUMN_NAME_AP_MODE = "ap_mode";
        public static final String COLUMN_NAME_APP_USER_STATUS = "app_user_status";
        public static final String COLUMN_NAME_UMOD_STATE = "umod_state";
        public static final String COLUMN_NAME_UMOD_STATUS = "umod_status";
        public static final String COLUMN_NAME_PROD_UUID = "product_uuid";
        public static final String COLUMN_NAME_HW_VERSION = "hw_version";
        public static final String COLUMN_NAME_SW_VERSION = "sw_version";
        public static final String COLUMN_NAME_IS_OPEN = "is_open";
        public static final String COLUMN_NAME_LAST_UPDATE_DATE = "last_update_date";
    }
}
