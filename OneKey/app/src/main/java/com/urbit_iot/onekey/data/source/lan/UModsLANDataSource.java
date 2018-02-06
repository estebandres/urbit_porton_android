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
import android.util.Log;

import com.fernandocejas.frodo.annotation.RxLogObservable;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.OTACommitRPC;
import com.urbit_iot.onekey.data.rpc.RPC;
import com.urbit_iot.onekey.data.rpc.SetWiFiAPRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.source.UModsDataSource;
import com.urbit_iot.onekey.umodconfig.domain.usecase.FactoryResetUMod;
import com.urbit_iot.onekey.util.networking.UrlHostSelectionInterceptor;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the data mSource that adds a latency simulating network.
 */
//TODO Remove all mocked data!!
public class UModsLANDataSource implements UModsDataSource {

    @NonNull
    private UModsDNSSDScanner mUModsDNSSDScanner;

    @NonNull
    private UModsBLEScanner mUModsBLEScanner;

    @NonNull
    private UModsWiFiScanner mUModsWiFiScanner;

    @NonNull
    private UModsService defaultUModsService;

    @NonNull
    private UModsService appUserUModsService;

    @NonNull
    private UrlHostSelectionInterceptor urlHostSelectionInterceptor;

    @NonNull
    private Observable.Transformer<UMod,UMod> uModLANBrander;


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
        addUModUser("0000SADFSE00", "0387 538-2229");
        addUModUser("2225FFX13000", "0387 508-8339");
        addUModUser("2225FFX13000", "0387 440-2010");
    }
    @Inject
    public UModsLANDataSource(@NonNull UModsDNSSDScanner uModsDNSSDScanner,
                              @NonNull UModsBLEScanner uModsBLEScanner,
                              @NonNull UModsWiFiScanner uModsWiFiScanner,
                              @NonNull UrlHostSelectionInterceptor urlHostSelectionInterceptor,
                              @NonNull @Named("default") UModsService defaultUModsService,
                              @NonNull @Named("app_user") UModsService AppUserUModsService){ //@NonNull @DigestAuth Credentials digestAuthCredentials) {
        mUModsDNSSDScanner = checkNotNull(uModsDNSSDScanner,"uModsDNSSDScanner should not be null.");
        mUModsBLEScanner = checkNotNull(uModsBLEScanner, " uModsBLEScanner should not be null.");
        this.mUModsWiFiScanner = checkNotNull(uModsWiFiScanner, " uModsWiFiScanner should not be null.");
        this.defaultUModsService = checkNotNull(defaultUModsService, " defaultUModsService should not be null.");
        this.appUserUModsService = checkNotNull(AppUserUModsService, " AppUserUModsService should not be null.");
        this.urlHostSelectionInterceptor = checkNotNull(urlHostSelectionInterceptor, " urlHostSelectionInterceptor should not be null.");
        //this.digestAuthCredentials = checkNotNull(digestAuthCredentials, " digestAuthCredentials should not be null.");
        this.uModLANBrander = new Observable.Transformer<UMod, UMod>() {
            @Override
            public Observable<UMod> call(Observable<UMod> uModObservable) {
                return uModObservable
                        .map(new Func1<UMod, UMod>() {
                            @Override
                            public UMod call(UMod uMod) {
                                uMod.setuModSource(UMod.UModSource.LAN_SCAN);
                                return uMod;
                            }
                        });
            }
        };
    }

    private static void addUMod(String uModUUID, String onLANIPAddress) {
        UMod uMod = new UMod(uModUUID, onLANIPAddress, true);
        uMod.setAppUserLevel(UModUser.Level.PENDING);
        UMODS_SERVICE_DATA.put(uMod.getUUID(), uMod);
    }

    private static void addUModUser(String uModUUID, String userPhoneNumber){
        UModUser uModUser = new UModUser(uModUUID,userPhoneNumber, UModUser.Level.AUTHORIZED);
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
                mUModsDNSSDScanner.browseLANForUMods(),
                mUModsBLEScanner.bleScanForUMods())
                .compose(this.uModLANBrander)
                .toList();
    }

    //@RxLogObservable
    public Observable<UMod> getUModsOneByOne(){
        return Observable.mergeDelayError(
                Observable.from(UMODS_SERVICE_DATA.values()),
                mUModsDNSSDScanner.browseLANForUMods(),
                mUModsBLEScanner.bleScanForUMods(),
                mUModsWiFiScanner.browseWiFiForUMods())
                .compose(this.uModLANBrander);
    }


    @Override
    @RxLogObservable
    public Observable<UMod> getUMod(@NonNull final String uModUUID) {
        //Log.d("lan_ds", uModUUID);
        //Log.d("lan_ds", UMODS_SERVICE_DATA.get(uModUUID).toString());
        UMod mockedUMod = UMODS_SERVICE_DATA.get(uModUUID);
        Observable<UMod> mockedUModObs;
        if (mockedUMod != null){
            mockedUModObs = Observable.just(mockedUMod);
            Log.d("lan_ds", mockedUMod.toString());
        } else {
            mockedUModObs = Observable.empty();
        }

        return Observable.mergeDelayError(mockedUModObs,
                mUModsDNSSDScanner.browseLANForUMod(uModUUID),
                mUModsWiFiScanner.browseWiFiForUMod(uModUUID))
                .doOnNext(new Action1<UMod>() {
                    @Override
                    public void call(UMod uMod) {
                        Log.e("lan_ds", uMod.toString());
                    }
                })
        //return mUModsDNSSDScanner.browseLANForUMod(uModUUID)
                .first()
                .compose(this.uModLANBrander);
    }

    @Override
    public void saveUMod(@NonNull UMod uMod) {
        UMODS_SERVICE_DATA.put(uMod.getUUID(), uMod);
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
        Iterator<Map.Entry<String, UMod>> it = UMODS_SERVICE_DATA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, UMod> entry = it.next();
            if (entry.getValue().getAppUserLevel().equals(UModUser.Level.UNAUTHORIZED)) {
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
    public Observable<GetMyUserLevelRPC.Response>
    getUserLevel(@NonNull UMod uMod, @NonNull GetMyUserLevelRPC.Request request) {
        /*
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.appUserUModsService.getAppUserLevel(request);
         */
        //TODO remove mock
        GetMyUserLevelRPC.Response response = new GetMyUserLevelRPC.Response(
                new GetMyUserLevelRPC.Result(GetMyUserLevelRPC.UModUserType.Admin),
                request.getCallTag(),
                new RPC.ResponseError(null,null));

        return Observable.just(response)
                .delay(300L, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<TriggerRPC.Response> triggerUMod(@NonNull UMod uMod, @NonNull TriggerRPC.Request request) {
        /*
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.appUserUModsService.triggerUMod(request);
        */
        //TODO remove mock
        final TriggerRPC.Response response = new TriggerRPC.Response(new TriggerRPC.Result(),
                request.getCallTag(),
                new RPC.ResponseError(401,"Unauthenticated"));

            return Observable.just(response).delay(1100L, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<CreateUserRPC.Response> createUModUser(@NonNull UMod uMod, @NonNull CreateUserRPC.Request request) {

        /*
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.appUserUModsService.createUser(request).toObservable();
        */

        //TODO remove mock
        final CreateUserRPC.Response response = new CreateUserRPC.Response(
                new CreateUserRPC.Result(GetMyUserLevelRPC.UModUserType.Admin),
                request.getCallTag(),
                null
        );
        return Observable.just(response).delay(680, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<UpdateUserRPC.Response> updateUModUser(@NonNull UMod uMod, @NonNull UpdateUserRPC.Request request) {
        UpdateUserRPC.Response defaultResponse = new UpdateUserRPC.Response(new UpdateUserRPC.Result(),
                "STEVE MOCK RESPONSE",
                new RPC.ResponseError(null,null));
        return Observable.just(defaultResponse).delay(850,TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<DeleteUserRPC.Response> deleteUModUser(@NonNull UMod uMod, @NonNull DeleteUserRPC.Request request) {
        return null;
    }

    @Override
    public Observable<List<UModUser>> getUModUsers(@NonNull UMod uMod) {
        return Observable.from(UMODS_USERS_SERVICE_DATA.get(uMod.getUUID()))
                .delay(700,TimeUnit.MILLISECONDS)
                .toList();
    }


    @Override
    public Observable<SysGetInfoRPC.Response> getSystemInfo(@NonNull UMod uMod, @NonNull SysGetInfoRPC.Request request) {
        /*
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return defaultUModsService.getSystemInfo(request);
         */
        //TODO remove mock
        SysGetInfoRPC.Response mockedResponse = new SysGetInfoRPC.Response(new SysGetInfoRPC.Result(null, "v1.0.2", null, null, null, null, null, null, null, null, null, new SysGetInfoRPC.Result.Wifi("192.168.212.134","192.168.212.1",null,"MOCKED_SSID"), null),"STEVE MOCK", null);
        return Observable.just(mockedResponse);
    }

    @Override
    public Observable<SetWiFiAPRPC.Response> setWiFiAP(UMod uMod, SetWiFiAPRPC.Request request) {
        /*
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return appUserUModsService.setWiFiAP(
                request.getMethodArguments().getSsid(),
                request.getMethodArguments().getPassword());
        */
        //TODO remove mock
        SetWiFiAPRPC.Response response = new SetWiFiAPRPC.Response(new SetWiFiAPRPC.Result(),
                "SetWiFiAP",
                null);
        return Observable.just(response)
                .delay(880L, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<File> getFirmwareImageFile(UMod uMod) {
        return null;
    }

    @Override
    public Observable<Response<ResponseBody>> postFirmwareUpdateToUMod(UMod uMod, File newFirmwareFile) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        //RequestBody requestFile = RequestBody.create(MediaType.parse("application/zip"),newFirmwareFile);
        //MultipartBody.Part multipartBody = MultipartBody.Part.create(requestFile);
        RequestBody fileRequestBody = RequestBody.create(MediaType.parse("multipart/form"),newFirmwareFile);
        MultipartBody.Part fileMultipartBody =
                MultipartBody.Part.createFormData("file",newFirmwareFile.getName(), fileRequestBody);

        RequestBody timeoutBody =
                RequestBody.create(MediaType.parse("text/plain"), String.valueOf(75L));

        //TODO remove mock
        Response<ResponseBody> mockedResp = Response.success(ResponseBody.create(MediaType.parse("text/plain"), "MOCKED"));
        return Observable.just(mockedResp).delay(500L, TimeUnit.MILLISECONDS);
        //return this.appUserUModsService.startFirmwareUpdate(timeoutBody, fileMultipartBody);
    }

    @Override
    public Observable<Response<ResponseBody>> otaCommit(UMod uMod, OTACommitRPC.Request request) {
        /*
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());//TODO could be composed
        return this.appUserUModsService.otaCommit(request);
         */
        //TODO remove mock
        Response<ResponseBody> mockedResp = Response.success(ResponseBody.create(MediaType.parse("text/plain"), "MOCKED"));
        return Observable.just(mockedResp).delay(500L, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<FactoryResetRPC.Response> factoryResetUMod(UMod uMod, FactoryResetRPC.Request request) {
        /*
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());//TODO could be composed
        return this.appUserUModsService.postFactoryReset(request);
         */
        FactoryResetRPC.Response response = new FactoryResetRPC.Response(null, "MOCKED", null);
        return Observable.just(response).delay(1350L, TimeUnit.MILLISECONDS);
    }
}
