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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsDataSource;
import com.urbit_iot.onekey.data.source.local.UModsPersistenceContract.UModEntry;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.util.List;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Concrete implementation of a data source as a db.
 */
public class UModsLocalDBDataSource implements UModsDataSource {

    @NonNull
    private final BriteDatabase mDatabaseHelper;

    @NonNull
    private Func1<Cursor, UMod> mTaskMapperFunction;

    public UModsLocalDBDataSource(@NonNull Context context,
                                  @NonNull BaseSchedulerProvider schedulerProvider) {
        checkNotNull(context, "context cannot be null");
        checkNotNull(schedulerProvider, "scheduleProvider cannot be null");
        UModsDbHelper dbHelper = new UModsDbHelper(context);
        SqlBrite sqlBrite = SqlBrite.create();
        mDatabaseHelper = sqlBrite.wrapDatabaseHelper(dbHelper, schedulerProvider.io());
        mTaskMapperFunction = new Func1<Cursor, UMod>() {
            @Override
            public UMod call(Cursor c) {
                String uuid = c.getString(c.getColumnIndexOrThrow(UModEntry.COLUMN_NAME_UUID));
                String ipAddress = c.getString(c.getColumnIndexOrThrow(UModEntry.COLUMN_NAME_LAN_IP_ADDRESS));
                boolean isOpen = c.getInt(c.getColumnIndexOrThrow(UModEntry.COLUMN_NAME_IS_OPEN))>0;
                return new UMod(uuid, ipAddress, isOpen);
            }
        };
    }

    @Override
    public Observable<List<UMod>> getUMods() {
        String[] projection = {
                UModEntry.COLUMN_NAME_UUID,
                UModEntry.COLUMN_NAME_ALIAS,
                UModEntry.COLUMN_NAME_LAN_IP_ADDRESS,
                UModEntry.COLUMN_NAME_NOTIF_EN,
        };
        String sql = String.format("SELECT %s FROM %s", TextUtils.join(",", projection), UModEntry.TABLE_NAME);
        return mDatabaseHelper.createQuery(UModEntry.TABLE_NAME, sql)
                .mapToList(mTaskMapperFunction);
    }

    @Override
    public Observable<UMod> getUModsOneByOne() {
        String[] projection = {
                UModEntry.COLUMN_NAME_UUID,
                UModEntry.COLUMN_NAME_ALIAS,
                UModEntry.COLUMN_NAME_LAN_IP_ADDRESS,
                UModEntry.COLUMN_NAME_NOTIF_EN,
        };
        String sql = String.format("SELECT %s FROM %s", TextUtils.join(",", projection), UModEntry.TABLE_NAME);
        return mDatabaseHelper.createQuery(UModEntry.TABLE_NAME, sql)
                .mapToOne(mTaskMapperFunction);
    }

    @Override
    public Observable<UMod> getUMod(@NonNull String uModUUID) {
        String[] projection = {
                UModEntry.COLUMN_NAME_UUID,
                UModEntry.COLUMN_NAME_ALIAS,
                UModEntry.COLUMN_NAME_LAN_IP_ADDRESS,
                UModEntry.COLUMN_NAME_NOTIF_EN,
        };
        String sql = String.format("SELECT %s FROM %s WHERE %s LIKE ?",
                TextUtils.join(",", projection), UModEntry.TABLE_NAME, UModEntry.COLUMN_NAME_UUID);
        return mDatabaseHelper.createQuery(UModEntry.TABLE_NAME, sql, uModUUID)
                .mapToOneOrDefault(mTaskMapperFunction, null);
    }

    @Override
    public void saveUMod(@NonNull UMod uMod) {
        checkNotNull(uMod);
        ContentValues values = new ContentValues();
        values.put(UModEntry.COLUMN_NAME_UUID, uMod.getUUID());
        values.put(UModEntry.COLUMN_NAME_ALIAS, uMod.getAlias());
        values.put(UModEntry.COLUMN_NAME_LAN_IP_ADDRESS, uMod.getLANIPAddress());
        values.put(UModEntry.COLUMN_NAME_NOTIF_EN, uMod.isNotificationEnabled());
        mDatabaseHelper.insert(UModEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public void enableUModNotification(@NonNull UMod uMod) {
        enableUModNotification(uMod.getUUID());
    }

    @Override
    public void enableUModNotification(@NonNull String uModUUID) {
        ContentValues values = new ContentValues();
        values.put(UModEntry.COLUMN_NAME_NOTIF_EN, true);

        String selection = UModEntry.COLUMN_NAME_UUID + " LIKE ?";
        String[] selectionArgs = {uModUUID};
        mDatabaseHelper.update(UModEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public void disableUModNotification(@NonNull UMod uMod) {
        disableUModNotification(uMod.getUUID());
    }

    @Override
    public void disableUModNotification(@NonNull String uModUUID) {
        ContentValues values = new ContentValues();
        values.put(UModEntry.COLUMN_NAME_NOTIF_EN, false);

        String selection = UModEntry.COLUMN_NAME_UUID + " LIKE ?";
        String[] selectionArgs = {uModUUID};
        mDatabaseHelper.update(UModEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public void clearAlienUMods() {
        String selection = UModEntry.COLUMN_NAME_APP_USER_STATUS + " LIKE ?";
        String[] selectionArgs = {UModUser.UModUserStatus.UNAUTHORIZED.getStatusID().toString()};
        mDatabaseHelper.delete(UModEntry.TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public void refreshUMods() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    @Override
    public void deleteAllUMods() {
        mDatabaseHelper.delete(UModEntry.TABLE_NAME, null);
    }

    @Override
    public void deleteUMod(@NonNull String uModUUID) {
        String selection = UModEntry.COLUMN_NAME_UUID + " LIKE ?";
        String[] selectionArgs = {uModUUID};
        mDatabaseHelper.delete(UModEntry.TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public Observable<GetMyUserLevelRPC.SuccessResponse> getUserLevel(@NonNull UMod uMod, @NonNull GetMyUserLevelRPC.Request request) {
        return null;
    }

    @Override
    public Observable<TriggerRPC.SuccessResponse> triggerUMod(@NonNull UMod uMod, @NonNull TriggerRPC.Request request) {
        return null;
    }

    @Override
    public Observable<UpdateUserRPC.SuccessResponse> updateUModUser(@NonNull UMod uMod, @NonNull UpdateUserRPC.Request request) {
        return null;
    }

    @Override
    public Observable<DeleteUserRPC.SuccessResponse> deleteUModUser(@NonNull UMod uMod, @NonNull DeleteUserRPC.Request request) {
        return null;
    }

    @Override
    public Observable<List<UModUser>> getUModUsers(@NonNull String uModUUID) {
        return null;
    }

    @Override
    public Observable<SysGetInfoRPC.SuccessResponse> getSystemInfo(@NonNull UMod uMod, @NonNull SysGetInfoRPC.Request request) {
        return null;
    }
}
