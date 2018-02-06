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

package com.urbit_iot.onekey.data;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.common.collect.Lists;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.OTACommitRPC;
import com.urbit_iot.onekey.data.rpc.SetWiFiAPRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsDataSource;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;

/**
 * Implementation of a remote data source with static access to the data for easy testing.
 */
public class FakeUModsRemoteDataSource implements UModsDataSource {

    private static final Map<String, UMod> TASKS_SERVICE_DATA = new LinkedHashMap<>();

    public FakeUModsRemoteDataSource() {}

    @Override
    public Observable<List<UMod>> getUMods() {
        List<UMod> tasks = Lists.newArrayList(TASKS_SERVICE_DATA.values());
        return Observable.just(tasks);
    }

    @Override
    public Observable<UMod> getUModsOneByOne() {
        return null;
    }

    @Override
    public Observable<UMod> getUMod(@NonNull String taskId) {
        UMod task = TASKS_SERVICE_DATA.get(taskId);
        return Observable.just(task);
    }

    @Override
    public void saveUMod(@NonNull UMod uMod) {
        TASKS_SERVICE_DATA.put(uMod.getUUID(), uMod);
    }

    @Override
    public Observable<UMod> updateUModAlias(@NonNull String uModUUID, @NonNull String newAlias) {
        return null;
    }

    @Override
    public void partialUpdate(@NonNull UMod uMod) {

    }

    @Override
    public void setUModNotificationStatus(@NonNull String uModUUID, @NonNull Boolean notificationEnabled) {

    }

    @Override
    public void clearAlienUMods() {
        Iterator<Map.Entry<String, UMod>> it = TASKS_SERVICE_DATA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, UMod> entry = it.next();
            if (entry.getValue().getAppUserLevel()== UModUser.Level.UNAUTHORIZED) {
                it.remove();
            }
        }
    }

    public void refreshUMods() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    @Override
    public void deleteUMod(@NonNull String taskId) {
        TASKS_SERVICE_DATA.remove(taskId);
    }

    @Override
    public Observable<GetMyUserLevelRPC.Response> getUserLevel(@NonNull UMod uMod, @NonNull GetMyUserLevelRPC.Request request) {
        return null;
    }

    @Override
    public Observable<TriggerRPC.Response> triggerUMod(@NonNull UMod uMod, @NonNull TriggerRPC.Request request) {
        return null;
    }

    @Override
    public Observable<CreateUserRPC.Response> createUModUser(@NonNull UMod uMod, @NonNull CreateUserRPC.Request request) {
        return null;
    }

    @Override
    public Observable<UpdateUserRPC.Response> updateUModUser(@NonNull UMod uMod, @NonNull UpdateUserRPC.Request request) {
        return null;
    }

    @Override
    public Observable<DeleteUserRPC.Response> deleteUModUser(@NonNull UMod uMod, @NonNull DeleteUserRPC.Request request) {
        return null;
    }

    @Override
    public Observable<List<UModUser>> getUModUsers(@NonNull UMod uMod) {
        return null;
    }

    @Override
    public Observable<SysGetInfoRPC.Response> getSystemInfo(@NonNull UMod uMod, @NonNull SysGetInfoRPC.Request request) {
        return null;
    }

    @Override
    public Observable<SetWiFiAPRPC.Response> setWiFiAP(UMod uMod, SetWiFiAPRPC.Request request) {
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

    @Override
    public Observable<Response<ResponseBody>> otaCommit(UMod uMod, OTACommitRPC.Request request) {
        return null;
    }

    @Override
    public Observable<FactoryResetRPC.Response> factoryResetUMod(UMod uMod, FactoryResetRPC.Request request) {
        return null;
    }

    @Override
    public void deleteAllUMods() {
        TASKS_SERVICE_DATA.clear();
    }

    @VisibleForTesting
    public void addUMods(UMod... uMods) {
        for (UMod uMod : uMods) {
            TASKS_SERVICE_DATA.put(uMod.getUUID(), uMod);
        }
    }
}
