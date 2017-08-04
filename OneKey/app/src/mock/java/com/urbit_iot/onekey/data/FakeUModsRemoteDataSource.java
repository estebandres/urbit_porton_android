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
import com.urbit_iot.onekey.data.commands.ApproveUserCmd;
import com.urbit_iot.onekey.data.commands.DeleteUserCmd;
import com.urbit_iot.onekey.data.commands.OpenCloseCmd;
import com.urbit_iot.onekey.data.source.UModsDataSource;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public void enableUModNotification(@NonNull UMod task) {
        UMod completedTask = new UMod(task.getUUID(), task.getLANIPAddress(), true);
        TASKS_SERVICE_DATA.put(task.getUUID(), completedTask);
    }

    @Override
    public void enableUModNotification(@NonNull String taskId) {
        // Not required for the remote data source.
    }

    @Override
    public void disableUModNotification(@NonNull UMod task) {
        UMod activeTask = new UMod(task.getUUID(), task.getLANIPAddress(), true);
        TASKS_SERVICE_DATA.put(task.getUUID(), activeTask);
    }

    @Override
    public void disableUModNotification(@NonNull String taskId) {
        // Not required for the remote data source.
    }

    @Override
    public void clearAlienUMods() {
        Iterator<Map.Entry<String, UMod>> it = TASKS_SERVICE_DATA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, UMod> entry = it.next();
            if (entry.getValue().getAppUserStatus()==UModUser.UModUserStatus.UNAUTHORIZED) {
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
    public Observable<OpenCloseCmd.Response> openCloseUMod(@NonNull UMod uMod, @NonNull OpenCloseCmd.CommandRequest commandRequest) {
        return null;
    }

    @Override
    public Observable<ApproveUserCmd.Response> approveUModUser(@NonNull UModUser uModUser) {
        return null;
    }

    @Override
    public Observable<DeleteUserCmd.Response> deleteUModUser(@NonNull UModUser uModUser) {
        return null;
    }

    @Override
    public Observable<List<UModUser>> getUModUsers(@NonNull String uModUUID) {
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
