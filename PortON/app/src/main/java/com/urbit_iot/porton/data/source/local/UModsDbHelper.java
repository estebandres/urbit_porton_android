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

package com.urbit_iot.porton.data.source.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UModsDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "Umods.db";

    private static final String TEXT_TYPE = " TEXT";

    private static final String BOOLEAN_TYPE = " INTEGER";

    private static final String INTEGER_TYPE = " INTEGER";

    private static final String DATE_TYPE = " TEXT";

    private static final String REAL_TYPE = " REAL";

    private static final String COMMA_SEP = ",";
//UUID should be the primary key_urbit_green isn't it?
    //UModsPersistenceContract.UModEntry._ID + TEXT_TYPE + " PRIMARY KEY," +
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + UModsPersistenceContract.UModEntry.UMODS_TABLE_NAME + " (" +
                    UModsPersistenceContract.UModEntry.UUID_CN + TEXT_TYPE + " PRIMARY KEY," +
                    UModsPersistenceContract.UModEntry.ALIAS_CN + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.LAN_OPERATION_ENABLED_CN + BOOLEAN_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.WIFI_SSID_CN + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.MAC_ADDRESS_CN + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.CONNECTION_ADDRESS_CN + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.UMOD_STATE_CN + INTEGER_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.LAST_REPORT_CN + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.APP_USER_STATUS_CN + INTEGER_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.NOTIF_ENABLED_CN + BOOLEAN_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.HW_VERSION_CN + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.SW_VERSION_CN + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.PROD_UUID_CN + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.LATITUDE_CN + REAL_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.LONGITUDE_CN + REAL_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.ADDRESS_TEXT_CN + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.LAST_UPDATE_DATE_CN + DATE_TYPE +
            " )";

    public UModsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Not required as at version 1
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Not required as at version 1
    }
}
