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

package com.urbit_iot.onekey.data.source.remote;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsDataSource;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;

/**
 * Implementation of the data source that adds a latency simulating network.
 */
public class UModsRemoteDataSource implements UModsDataSource {

    private static UModsRemoteDataSource INSTANCE;

    private static final int SERVICE_LATENCY_IN_MILLIS = 5000;

    private final static Map<String, UMod> TASKS_SERVICE_DATA;

    static {
        TASKS_SERVICE_DATA = new LinkedHashMap<>(2);
        addUMod("Build tower in Pisa", "Ground looks good, no foundation work required.");
        addUMod("Finish bridge in Tacoma", "Found awesome girders at half the cost!");
    }

    public static UModsRemoteDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UModsRemoteDataSource();
        }
        return INSTANCE;
    }

    // Prevent direct instantiation.
    private UModsRemoteDataSource() {}

    private static void addUMod(String title, String description) {
        UMod newTask = new UMod(title, description, true);
        TASKS_SERVICE_DATA.put(newTask.getUUID(), newTask);
    }

    @Override
    public Observable<List<UMod>> getUMods() {
        return Observable
                .from(TASKS_SERVICE_DATA.values())
                .delay(SERVICE_LATENCY_IN_MILLIS, TimeUnit.MILLISECONDS)
                .toList();
    }

    @Override
    public Observable<UMod> getUModsOneByOne() {
        return null;
    }

    @Override
    public Observable<UMod> getUMod(@NonNull String taskId) {
        final UMod task = TASKS_SERVICE_DATA.get(taskId);
        if(task != null) {
            return Observable.just(task).delay(SERVICE_LATENCY_IN_MILLIS, TimeUnit.MILLISECONDS);
        } else {
            return Observable.empty();
        }
    }

    @Override
    public void saveUMod(@NonNull UMod uMod) {
        TASKS_SERVICE_DATA.put(uMod.getUUID(), uMod);
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
            if (entry.getValue().getAppUserLevel() == UModUser.Level.UNAUTHORIZED) {
                it.remove();
            }
        }
    }

    @Override
    public void refreshUMods() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    @Override
    public void deleteAllUMods() {
        TASKS_SERVICE_DATA.clear();
    }

    @Override
    public void deleteUMod(@NonNull String uModUUID) {
        TASKS_SERVICE_DATA.remove(uModUUID);
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
}
