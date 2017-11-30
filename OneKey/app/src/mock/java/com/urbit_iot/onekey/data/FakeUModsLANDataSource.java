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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
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
public class FakeUModsLANDataSource implements UModsDataSource {

    private static FakeUModsLANDataSource INSTANCE;

    private static final int SERVICE_LATENCY_IN_MILLIS = 5000;

    private final static Map<String, UMod> UMODS_SERVICE_DATA;

    private final static Multimap<String, UModUser> UMODS_USERS_SERVICE_DATA;

    static {
        UMODS_SERVICE_DATA = new LinkedHashMap<>(2);
        addUMod("0000SADFSE00", "192.168.0.6");
        addUMod("2225FFX13000", "10.0.2.62");
    }

    static {
        UMODS_USERS_SERVICE_DATA = ArrayListMultimap.create();
        addUModUser("0000SADFSE00", "3874623893");
        addUModUser("2225FFX13000", "3874624009");
        addUModUser("2225FFX13000", "3874402010");
    }

    public static FakeUModsLANDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FakeUModsLANDataSource();
        }
        return INSTANCE;
    }

    // Prevent direct instantiation.
    private FakeUModsLANDataSource() {}

    private static void addUMod(String uModUUID, String onLANIPAddress) {
        UMod uMod = new UMod(uModUUID, onLANIPAddress, true);
        UMODS_SERVICE_DATA.put(uMod.getUUID(), uMod);
    }

    private static void addUModUser(String uModUUID, String userPhoneNumber){
        UModUser uModUser = new UModUser(uModUUID,userPhoneNumber,UModUser.UModUserStatus.AUTHORIZED);
        UMODS_USERS_SERVICE_DATA.put(uModUUID,uModUser);
    }

    @Override
    public Observable<List<UMod>> getUMods() {
        return Observable
                .from(UMODS_SERVICE_DATA.values())
                .delay(SERVICE_LATENCY_IN_MILLIS, TimeUnit.MILLISECONDS)
                .toList();
    }

    @Override
    public Observable<UMod> getUModsOneByOne() {
        return null;
    }

    @Override
    public Observable<UMod> getUMod(@NonNull String uModUUID) {
        final UMod uMod = UMODS_SERVICE_DATA.get(uModUUID);
        if(uMod != null) {
            return Observable.just(uMod).delay(SERVICE_LATENCY_IN_MILLIS, TimeUnit.MILLISECONDS);
        } else {
            return Observable.empty();
        }
    }

    @Override
    public void saveUMod(@NonNull UMod uMod) {
        UMODS_SERVICE_DATA.put(uMod.getUUID(), uMod);
    }

    @Override
    public void enableUModNotification(@NonNull UMod uMod) {
        UMod notificationEnabledUMod = new UMod(uMod.getUUID(), uMod.getLANIPAddress(), true);
        notificationEnabledUMod.enableNotification();
        UMODS_SERVICE_DATA.put(notificationEnabledUMod.getUUID(), notificationEnabledUMod);
    }

    @Override
    public void enableUModNotification(@NonNull String uModUUID) {
        // Not required for the remote data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link task} using its cached data.
    }

    @Override
    public void disableUModNotification(@NonNull UMod uMod) {
        UMod notificationDisabledUMod = new UMod(uMod.getUUID(), uMod.getLANIPAddress(), true);
        notificationDisabledUMod.disableNotification();
        UMODS_SERVICE_DATA.put(uMod.getUUID(), notificationDisabledUMod);
    }

    @Override
    public void disableUModNotification(@NonNull String uModUUID) {
        // Not required for the remote data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link task} using its cached data.
    }

    @Override
    public void clearAlienUMods() {
        Iterator<Map.Entry<String, UMod>> it = UMODS_SERVICE_DATA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, UMod> entry = it.next();
            if (entry.getValue().getAppUserStatus().equals(UModUser.UModUserStatus.UNAUTHORIZED)) {
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
        UMODS_SERVICE_DATA.clear();
    }

    @Override
    public void deleteUMod(@NonNull String uModUUID) {
        UMODS_SERVICE_DATA.remove(uModUUID);
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
        return Observable.from(UMODS_USERS_SERVICE_DATA.get(uModUUID)).
                delay(700,TimeUnit.MILLISECONDS).
                toList();
    }
}
