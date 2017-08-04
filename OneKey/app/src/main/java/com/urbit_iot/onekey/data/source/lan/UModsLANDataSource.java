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

package com.urbit_iot.onekey.data.source.lan;

import android.support.annotation.NonNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.urbit_iot.onekey.data.FakeUModsLANDataSource;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.commands.ApproveUserCmd;
import com.urbit_iot.onekey.data.commands.DeleteUserCmd;
import com.urbit_iot.onekey.data.commands.OpenCloseCmd;
import com.urbit_iot.onekey.data.source.UModsDataSource;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the data source that adds a latency simulating network.
 */
public class UModsLANDataSource implements UModsDataSource {

    @NonNull
    private UModsDNSSDScanner mUModsDNSSDScanner;

    @NonNull
    private UModsBLEScanner mUModsBLEScanner;

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
    @Inject
    public UModsLANDataSource(@NonNull UModsDNSSDScanner uModsDNSSDScanner,
                              @NonNull UModsBLEScanner uModsBLEScanner) {
        mUModsDNSSDScanner = checkNotNull(uModsDNSSDScanner,"uModsDNSSDScanner should not be null.");
        mUModsBLEScanner = checkNotNull(uModsBLEScanner, " uModsBLEScanner should not be null.");
    }

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

        /*
        Observable<List<UMod>> mockedUMods = Observable
                .from(UMODS_SERVICE_DATA.values())//.delay(SERVICE_LATENCY_IN_MILLIS, TimeUnit.MILLISECONDS)
                .toList();

        Observable<List<UMod>> DNSSDFounded =
                mUModsDNSSDScanner.browseLANForUMods()//.takeUntil(mockedUMods)
                .toList();
        */
        return Observable.mergeDelayError(Observable.from(UMODS_SERVICE_DATA.values()),
                //mUModsDNSSDScanner.browseLANForUMods(),
                mUModsBLEScanner.bleScanForUMods()
                ).toList();

        //return Observable.mergeDelayError(mockedUMods,DNSSDFounded);
        //return DNSSDFounded;
    }

    public Observable<UMod> getUModsOneByOne(){
        return Observable.mergeDelayError(
                Observable.from(UMODS_SERVICE_DATA.values()),
                mUModsDNSSDScanner.browseLANForUMods(),
                mUModsBLEScanner.bleScanForUMods());
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
    public Observable<OpenCloseCmd.Response> openCloseUMod(@NonNull UMod uMod, @NonNull OpenCloseCmd.CommandRequest commandRequest) {
        final OpenCloseCmd.Response response = new OpenCloseCmd.Response(123456789,"OK");
        if(response != null) {
            return Observable.just(response).delay(680, TimeUnit.MILLISECONDS);
        } else {
            return Observable.empty();
        }
    }

    @Override
    public Observable<ApproveUserCmd.Response> approveUModUser(@NonNull UModUser uModUser) {
        //Hardcoded interaction doesn't need an actual request to be constructed and sent.
        //Command.CommandRequest request = new ApproveUserCmd.Request(6666666,"asdf","lkjh");
        final ApproveUserCmd.Response response = new ApproveUserCmd.Response(123456789,"OK");
        if(response != null) {
            return Observable.just(response).delay(680, TimeUnit.MILLISECONDS);
        } else {
            return Observable.empty();
        }
    }

    @Override
    public Observable<DeleteUserCmd.Response> deleteUModUser(@NonNull UModUser uModUser) {
        //Hardcoded interaction doesn't need an actual request to be constructed and sent.
        //Command.CommandRequest request = new DeleteUserCmd.Request(6666666,"asdf","lkjh");
        final DeleteUserCmd.Response response = new DeleteUserCmd.Response(123456789,"OK");
        if(response != null) {
            return Observable.just(response).delay(680, TimeUnit.MILLISECONDS);
        } else {
            return Observable.empty();
        }
    }

    @Override
    public Observable<List<UModUser>> getUModUsers(@NonNull String uModUUID) {
        return Observable.from(UMODS_USERS_SERVICE_DATA.get(uModUUID)).
                delay(700,TimeUnit.MILLISECONDS).
                toList();
    }
}
