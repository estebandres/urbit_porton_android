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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UModsDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "Umods.db";

    private static final String TEXT_TYPE = " TEXT";

    private static final String BOOLEAN_TYPE = " INTEGER";

    private static final String INTEGER_TYPE = " INTEGER";

    private static final String DATE_TYPE = " INTEGER";

    private static final String COMMA_SEP = ",";
//UUID should be the primary key isn't it?
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + UModsPersistenceContract.UModEntry.TABLE_NAME + " (" +
                    UModsPersistenceContract.UModEntry._ID + TEXT_TYPE + " PRIMARY KEY," +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_UUID + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_ALIAS + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_AP_MODE + BOOLEAN_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_LAN_IP_ADDRESS + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_UMOD_STATE + INTEGER_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_UMOD_STATUS + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_APP_USER_STATUS + INTEGER_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_IS_OPEN + BOOLEAN_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_NOTIF_EN + BOOLEAN_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_HW_VERSION + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_SW_VERSION + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_PROD_UUID + TEXT_TYPE + COMMA_SEP +
                    UModsPersistenceContract.UModEntry.COLUMN_NAME_LAST_UPDATE_DATE + DATE_TYPE +
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
