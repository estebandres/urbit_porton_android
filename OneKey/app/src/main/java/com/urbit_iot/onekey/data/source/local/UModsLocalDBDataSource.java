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
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.GetUsersRPC;
import com.urbit_iot.onekey.data.rpc.OTACommitRPC;
import com.urbit_iot.onekey.data.rpc.SetWiFiRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsDataSource;
import com.urbit_iot.onekey.data.source.local.UModsPersistenceContract.UModEntry;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Response;
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
    private Func1<Cursor, UMod> mUModMapperFunction;

    @NonNull
    private Observable.Transformer<UMod,UMod> uModLocalDBBrander;

    static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public UModsLocalDBDataSource(@NonNull Context context,
                                  @NonNull BaseSchedulerProvider schedulerProvider) {
        checkNotNull(context, "context cannot be null");
        checkNotNull(schedulerProvider, "scheduleProvider cannot be null");
        UModsDbHelper dbHelper = new UModsDbHelper(context);
        SqlBrite sqlBrite = SqlBrite.create();
        mDatabaseHelper = sqlBrite.wrapDatabaseHelper(dbHelper, schedulerProvider.io());
        mUModMapperFunction = new Func1<Cursor, UMod>() {
            @Override
            public UMod call(Cursor c) {
                String uuid = c.getString(c.getColumnIndexOrThrow(UModEntry.UUID_CN));
                String alias = c.getString(c.getColumnIndexOrThrow(UModEntry.ALIAS_CN));
                String wifiSSID = c.getString(c.getColumnIndexOrThrow(UModEntry.WIFI_SSID_CN));
                String macAddress = c.getString(c.getColumnIndexOrThrow(UModEntry.MAC_ADDRESS_CN));
                boolean notifEnabled = c.getInt(c.getColumnIndexOrThrow(UModEntry.NOTIF_ENABLED_CN))>0;
                String connectionAddress = c.getString(c.getColumnIndexOrThrow(UModEntry.CONNECTION_ADDRESS_CN));
                UMod.State state = UMod.State.from(c.getInt(c.getColumnIndexOrThrow(UModEntry.UMOD_STATE_CN)));
                UModUser.Level userStatus = UModUser.Level.from(c.getInt(c.getColumnIndexOrThrow(UModEntry.APP_USER_STATUS_CN)));
                String lastReport = c.getString(c.getColumnIndexOrThrow(UModEntry.LAST_REPORT_CN));
                String productUUID = c.getString(c.getColumnIndexOrThrow(UModEntry.PROD_UUID_CN));
                String hwVersion = c.getString(c.getColumnIndexOrThrow(UModEntry.HW_VERSION_CN));
                String swVersion = c.getString(c.getColumnIndexOrThrow(UModEntry.SW_VERSION_CN));
                return new UMod(uuid, alias, wifiSSID, connectionAddress, state, userStatus, notifEnabled,
                        macAddress, lastReport, productUUID, hwVersion, swVersion);
            }
        };
        this.uModLocalDBBrander = new Observable.Transformer<UMod, UMod>() {
            @Override
            public Observable<UMod> call(Observable<UMod> uModObservable) {
                return uModObservable
                        .map(new Func1<UMod, UMod>() {
                            @Override
                            public UMod call(UMod uMod) {
                                if(uMod != null){
                                    uMod.setuModSource(UMod.UModSource.LOCAL_DB);
                                }
                                return uMod;
                            }
                        });
            }
        };
    }

    @Override
    public Observable<List<UMod>> getUMods() {
        String[] projection = {
                UModEntry.UUID_CN,
                UModEntry.ALIAS_CN,
                UModEntry.CONNECTION_ADDRESS_CN,
                UModEntry.NOTIF_ENABLED_CN,
        };
        String sql = String.format("SELECT %s FROM %s", TextUtils.join(",", projection), UModEntry.UMODS_TABLE_NAME);
        return mDatabaseHelper.createQuery(UModEntry.UMODS_TABLE_NAME, sql)
                .mapToList(mUModMapperFunction)
                .flatMap(new Func1<List<UMod>, Observable<UMod>>() {
                    @Override
                    public Observable<UMod> call(List<UMod> uMods) {
                        return Observable.from(uMods);
                    }
                })
                .compose(this.uModLocalDBBrander)
                .toList();
    }

    @Override
    //@RxLogObservable
    public Observable<UMod> getUModsOneByOne() {
        String[] projection = {
                UModEntry.UUID_CN,
                UModEntry.ALIAS_CN,
                UModEntry.WIFI_SSID_CN,
                UModEntry.MAC_ADDRESS_CN,
                UModEntry.CONNECTION_ADDRESS_CN,
                UModEntry.NOTIF_ENABLED_CN,
                UModEntry.APP_USER_STATUS_CN,
                UModEntry.UMOD_STATE_CN,
                UModEntry.LAST_REPORT_CN,
                UModEntry.PROD_UUID_CN,
                UModEntry.HW_VERSION_CN,
                UModEntry.SW_VERSION_CN,
                UModEntry.LAST_UPDATE_DATE_CN,
        };
        String sql = String.format("SELECT %s FROM %s", TextUtils.join(",", projection), UModEntry.UMODS_TABLE_NAME);
        return mDatabaseHelper.createQuery(UModEntry.UMODS_TABLE_NAME, sql)
                .mapToList(mUModMapperFunction)
                .flatMap(new Func1<List<UMod>, Observable<UMod>>() {
                    @Override
                    public Observable<UMod> call(List<UMod> uMods) {
                        return Observable.from(uMods);
                    }
                })
                //TODO make the stream complete on a better way.
                .take(1200L, TimeUnit.MILLISECONDS)
                .compose(this.uModLocalDBBrander);
    }

    @Override
    //@RxLogObservable
    public Observable<UMod> getUMod(@NonNull String uModUUID) {
        String[] projection = {
                UModEntry.UUID_CN,
                UModEntry.ALIAS_CN,
                UModEntry.MAC_ADDRESS_CN,
                UModEntry.WIFI_SSID_CN,
                UModEntry.CONNECTION_ADDRESS_CN,
                UModEntry.NOTIF_ENABLED_CN,
                UModEntry.APP_USER_STATUS_CN,
                UModEntry.UMOD_STATE_CN,
                UModEntry.LAST_REPORT_CN,
                UModEntry.PROD_UUID_CN,
                UModEntry.HW_VERSION_CN,
                UModEntry.SW_VERSION_CN,
                UModEntry.LAST_UPDATE_DATE_CN,
        };
        String sql = String.format("SELECT %s FROM %s WHERE %s LIKE ?",
                TextUtils.join(",", projection), UModEntry.UMODS_TABLE_NAME, UModEntry.UUID_CN);
        return mDatabaseHelper.createQuery(UModEntry.UMODS_TABLE_NAME, sql, uModUUID)
                //.mapToOne(mUModMapperFunction)
                .mapToOneOrDefault(mUModMapperFunction, null)
                .first()
                .compose(this.uModLocalDBBrander);
    }

    @Override
    public void saveUMod(@NonNull UMod uMod) {
        checkNotNull(uMod);
        ContentValues values = new ContentValues();
        values.put(UModEntry.UUID_CN, uMod.getUUID());
        values.put(UModEntry.ALIAS_CN, uMod.getAlias());
        values.put(UModEntry.WIFI_SSID_CN, uMod.getWifiSSID());
        values.put(UModEntry.MAC_ADDRESS_CN, uMod.getMacAddress());
        values.put(UModEntry.CONNECTION_ADDRESS_CN, uMod.getConnectionAddress());
        values.put(UModEntry.NOTIF_ENABLED_CN, uMod.isOngoingNotificationEnabled());
        values.put(UModEntry.APP_USER_STATUS_CN,uMod.getAppUserLevel().getStatusID());
        values.put(UModEntry.UMOD_STATE_CN,uMod.getState().getStateID());
        values.put(UModEntry.LAST_REPORT_CN, uMod.getuModLastReport());
        values.put(UModEntry.PROD_UUID_CN, uMod.getProductUUID());
        values.put(UModEntry.HW_VERSION_CN, uMod.getHWVersion());
        values.put(UModEntry.SW_VERSION_CN, uMod.getSWVersion());
        values.put(UModEntry.LAST_UPDATE_DATE_CN, dateFormat.format(uMod.getLastUpdateDate()));
        mDatabaseHelper.insert(UModEntry.UMODS_TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public Observable<UMod> updateUModAlias(@NonNull String uModUUID, @NonNull final String newAlias) {
        ContentValues values = new ContentValues();
        values.put(UModEntry.ALIAS_CN, newAlias);

        String selection = UModEntry.UUID_CN + " LIKE ?";
        String[] selectionArgs = {uModUUID};
        int updatedRows = mDatabaseHelper.update(UModEntry.UMODS_TABLE_NAME, values, selection, selectionArgs);
        if (updatedRows > 0){
            return this.getUMod(uModUUID);
        } else {
            return Observable.error(new Exception("Zero rows where updated."));
        }
        /* Does the same. Why should I prefer one way over the other??
        return this.getUMod(uModUUID)
                .flatMap(new Func1<UMod, Observable<UMod>>() {
                    @Override
                    public Observable<UMod> call(UMod uMod) {
                        uMod.setAlias(newAlias);
                        saveUMod(uMod);
                        Log.d("local_db", uMod.toString());
                        return Observable.just(uMod);
                    }
                });
          */
    }

    @Override
    public void partialUpdate(@NonNull UMod uMod) {
        checkNotNull(uMod);
        ContentValues values = new ContentValues();
        //TODO look for a better way to do a partial update in the data base
        if (uMod.getConnectionAddress() != null && !uMod.getConnectionAddress().isEmpty()){
            values.put(UModEntry.CONNECTION_ADDRESS_CN, uMod.getConnectionAddress());
        }
        values.put(UModEntry.LAST_UPDATE_DATE_CN, dateFormat.format(uMod.getLastUpdateDate()));
        String selection = UModEntry.UUID_CN + " LIKE ?";
        String[] selectionArgs = {uMod.getUUID()};
        mDatabaseHelper.update(UModEntry.UMODS_TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public void setUModNotificationStatus(@NonNull String uModUUID, @NonNull Boolean notificationStatus) {
        ContentValues values = new ContentValues();
        values.put(UModEntry.NOTIF_ENABLED_CN, notificationStatus);

        String selection = UModEntry.UUID_CN + " LIKE ?";
        String[] selectionArgs = {uModUUID};
        mDatabaseHelper.update(UModEntry.UMODS_TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public void clearAlienUMods() {
        String selection = UModEntry.APP_USER_STATUS_CN + " LIKE ?";
        String[] selectionArgs = {UModUser.Level.UNAUTHORIZED.getStatusID().toString()};
        mDatabaseHelper.delete(UModEntry.UMODS_TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public void refreshUMods() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    @Override
    public void deleteAllUMods() {
        mDatabaseHelper.delete(UModEntry.UMODS_TABLE_NAME, null);
    }

    @Override
    public void deleteUMod(@NonNull String uModUUID) {
        String selection = UModEntry.UUID_CN + " LIKE ?";
        String[] selectionArgs = {uModUUID};
        mDatabaseHelper.delete(UModEntry.UMODS_TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public Observable<GetMyUserLevelRPC.Result> getUserLevel(@NonNull UMod uMod, @NonNull GetMyUserLevelRPC.Arguments requestArguments) {
        return null;
    }

    @Override
    public Observable<TriggerRPC.Result> triggerUMod(@NonNull UMod uMod, @NonNull TriggerRPC.Arguments requestArguments) {
        return null;
    }

    @Override
    public Observable<CreateUserRPC.Result> createUModUser(@NonNull UMod uMod, @NonNull CreateUserRPC.Arguments requestArguments) {
        return null;
    }

    @Override
    public Observable<UpdateUserRPC.Result> updateUModUser(@NonNull UMod uMod, @NonNull UpdateUserRPC.Arguments requestArguments) {
        return null;
    }

    @Override
    public Observable<DeleteUserRPC.Result> deleteUModUser(@NonNull UMod uMod, @NonNull DeleteUserRPC.Arguments requestArguments) {
        return null;
    }

    @Override
    public Observable<GetUsersRPC.Result> getUModUsers(@NonNull UMod uMod, @NonNull GetUsersRPC.Arguments requestArgs) {
        return null;
    }

    @Override
    public Observable<SysGetInfoRPC.Result> getSystemInfo(@NonNull UMod uMod, @NonNull SysGetInfoRPC.Arguments request) {
        return null;
    }

    @Override
    public Observable<SetWiFiRPC.Result> setWiFiAP(UMod uMod, SetWiFiRPC.Arguments request) {
        return null;
    }

    @Override
    public Observable<Response<ResponseBody>> otaCommit(UMod uMod, OTACommitRPC.Arguments request) {
        return null;
    }

    @Override
    public Observable<FactoryResetRPC.Result> factoryResetUMod(UMod uMod, FactoryResetRPC.Arguments request) {
        return null;
    }

    @Override
    public Observable<File> getFirmwareImageFile(UMod uMod) {
        return null;
    }

    @Override
    public Observable<Response<ResponseBody>> postFirmwareUpdateToUMod(UMod uMod, File newFirmwareFile) {
        return null;
    }

}
