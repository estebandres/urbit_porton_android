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
import android.util.Log;

import com.fernandocejas.frodo.annotation.RxLogObservable;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.stealthcopter.networktools.SubnetDevices;
import com.stealthcopter.networktools.subnet.Device;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.APIUserType;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.rpc.GetUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.GetUsersRPC;
import com.urbit_iot.onekey.data.rpc.OTACommitRPC;
import com.urbit_iot.onekey.data.rpc.SetWiFiRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.source.UModsDataSource;
import com.urbit_iot.onekey.data.source.lan.UModsBLEScanner;
import com.urbit_iot.onekey.data.source.lan.UModsDNSSDScanner;
import com.urbit_iot.onekey.data.source.lan.UModsService;
import com.urbit_iot.onekey.data.source.lan.UModsWiFiScanner;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.networking.UrlHostSelectionInterceptor;

import java.io.File;
import java.util.ArrayList;
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
import rx.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the data mSource that adds a latency simulating network.
 */
public class FakeUModsLANDataSource implements UModsDataSource {


    @NonNull
    private Observable.Transformer<UMod,UMod> uModLANBrander;


    private static final int SERVICE_LATENCY_IN_MILLIS = 5000;

    private final static Map<String, UMod> UMODS_SERVICE_DATA;

    private final static ArrayListMultimap<String, UModUser> UMODS_USERS_SERVICE_DATA;

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
    public FakeUModsLANDataSource(){
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
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        UMODS_SERVICE_DATA.put(uMod.getUUID(), uMod);
    }

    private static void addUModUser(String uModUUID, String userPhoneNumber){
        UModUser uModUser = new UModUser(uModUUID,userPhoneNumber, UModUser.Level.PENDING);
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
                Observable.empty())
                .compose(this.uModLANBrander)
                .toList();
    }

    @RxLogObservable
    public Observable<UMod> getUModsOneByOne(){
        return Observable.mergeDelayError(
                Observable.from(UMODS_SERVICE_DATA.values()),
                Observable.empty())
                .compose(this.uModLANBrander);
    }



    private String convertMacAddressToUModUUID(String macAddress){
        String macAddressWithoutColons = macAddress.replaceAll(":","");
        return macAddressWithoutColons.substring(macAddressWithoutColons.length()-6).toUpperCase();
    }

    @Override
    //@RxLogObservable
    public Observable<UMod> getUMod(@NonNull final String uModUUID) {
        UMod mockedUMod = UMODS_SERVICE_DATA.get(uModUUID);
        mockedUMod.setState(UMod.State.AP_MODE);
        Observable<UMod> mockedUModObs;
        if (mockedUMod != null){
            mockedUModObs = Observable.just(mockedUMod);
            Log.d("lan_data-source", mockedUMod.toString());
        } else {
            mockedUModObs = Observable.empty();
        }

        return Observable.mergeDelayError(
                mockedUModObs,
                Observable.empty())
                //getSingleUModByLanPingingAndApiCalling(uModUUID))
                .doOnNext(new Action1<UMod>() {
                    @Override
                    public void call(UMod uMod) {
                        Log.e("lan_data-source", uMod.toString());
                    }
                })
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
    public Observable<GetUserLevelRPC.Result>
    getUserLevel(@NonNull UMod uMod, @NonNull GetUserLevelRPC.Arguments request) {


        //TODO remove mock
        /*
        GetUserLevelRPC.Response response = new GetUserLevelRPC.Response(
                new GetUserLevelRPC.Result(GetUserLevelRPC.UModUserType.Admin),
                request.getRequestTag(),
                new RPC.ResponseError(null,null));
        */

        GetUserLevelRPC.Result result =
                new GetUserLevelRPC.Result("54387666666", APIUserType.Admin);

        return Observable.just(result)
                .delay(300L, TimeUnit.MILLISECONDS);

    }

    @Override
    public Observable<TriggerRPC.Result> triggerUMod(@NonNull UMod uMod, @NonNull TriggerRPC.Arguments request) {


        //TODO remove mock
        /*
        final TriggerRPC.Response response = new TriggerRPC.Response(new TriggerRPC.Result(),
                request.getRequestTag(),
                new RPC.ResponseError(401,"Unauthenticated"));
         */

        /*
        HttpException exception = new HttpException(Response.error(401, ResponseBody.create(MediaType.parse("text/plain"), "MOCKED")));
        return Observable.error(exception);
         */


        final TriggerRPC.Result result = new TriggerRPC.Result("MOCK TRIGGER!!");
        return Observable.just(result).delay(1100L, TimeUnit.MILLISECONDS);


    }

    @Override
    public Observable<CreateUserRPC.Result> createUModUser(@NonNull UMod uMod, @NonNull CreateUserRPC.Arguments request) {
        /*
        final CreateUserRPC.Response response = new CreateUserRPC.Response(
                new CreateUserRPC.Result(GetUserLevelRPC.UModUserType.Admin),
                request.getRequestTag(),
                null
        );
         */


        final CreateUserRPC.Result result =
                new CreateUserRPC.Result(APIUserType.Admin);
        return Observable.just(result).delay(680, TimeUnit.MILLISECONDS);

    }

    @Override
    public Observable<UpdateUserRPC.Result> updateUModUser(@NonNull UMod uMod, @NonNull UpdateUserRPC.Arguments request) {

        /*
        UpdateUserRPC.Response defaultResponse = new UpdateUserRPC.Response(new UpdateUserRPC.Result(),
                "STEVE MOCK RESPONSE",
                new RPC.ResponseError(null,null));
         */
        for (UModUser user : UMODS_USERS_SERVICE_DATA.get(uMod.getUUID())){
            String phone = user.getPhoneNumber();
            if (request.getUserID().contains(phone.substring(phone.length() - 4))){
                user.setUserStatus(request.getUserType().asUModUserLevel());
            }
        }

        final UpdateUserRPC.Result mockedResult = new UpdateUserRPC.Result("update success");
        return Observable.just(mockedResult).delay(850,TimeUnit.MILLISECONDS);

    }

    @Override
    public Observable<DeleteUserRPC.Result> deleteUModUser(@NonNull UMod uMod, @NonNull DeleteUserRPC.Arguments request) {

        DeleteUserRPC.Result result = new DeleteUserRPC.Result("user deleted");

        int index = 0;
        for (UModUser user : UMODS_USERS_SERVICE_DATA.get(uMod.getUUID())){
            String phone = user.getPhoneNumber();
            if (request.getUserID().contains(phone.substring(phone.length() - 4))){
                break;
            }
            index++;
        }

        UMODS_USERS_SERVICE_DATA.get(uMod.getUUID()).remove(index);

        return Observable.just(result).delay(500L, TimeUnit.MILLISECONDS);

    }

    @Override
    public Observable<GetUsersRPC.Result> getUModUsers(@NonNull UMod uMod, @NonNull GetUsersRPC.Arguments requestArgs) {


        List<GetUsersRPC.UserResult> usersResults = new ArrayList<>();
        for (UModUser user :
                UMODS_USERS_SERVICE_DATA.get(uMod.getUUID())) {
            usersResults.add(new GetUsersRPC.UserResult(user.getPhoneNumber(), user.getUserStatus().asAPIUserType()));
        }
        GetUsersRPC.Result result = new GetUsersRPC.Result(usersResults);

        return Observable.just(result).delay(700L, TimeUnit.MILLISECONDS);

    }


    @Override
    public Observable<SysGetInfoRPC.Result> getSystemInfo(@NonNull UMod uMod, @NonNull SysGetInfoRPC.Arguments request) {
        /*
        SysGetInfoRPC.Response mockedResponse = new SysGetInfoRPC.Response(new SysGetInfoRPC.Result(null, "v1.0.2", null, null, null, null, null, null, null, null, null, new SysGetInfoRPC.Result.Wifi("192.168.212.134","192.168.212.1",null,"MOCKED_SSID"), null),"STEVE MOCK", null);
         */

        final SysGetInfoRPC.Result mockedResult =
                new SysGetInfoRPC.Result(
                        null,
                        "v1.0.2",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new SysGetInfoRPC.Result.Wifi(
                                "192.168.212.134",
                                "192.168.212.1",
                                null,
                                "MOCKED_SSID"),
                        null);
        return Observable.just(mockedResult).delay(330L, TimeUnit.MILLISECONDS);

    }

    @Override
    public Observable<SetWiFiRPC.Result> setWiFiAP(UMod uMod, SetWiFiRPC.Arguments request) {
        /*
        SetWiFiRPC.Response response = new SetWiFiRPC.Response(new SetWiFiRPC.Result(),
                "SetWiFiAP",
                null);
         */

        final SetWiFiRPC.Result mockedResult = new SetWiFiRPC.Result("MOCKED SET WIFI");
        return Observable.just(mockedResult)
                .delay(880L, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<File> getFirmwareImageFile(UMod uMod) {
        return null;
    }

    @Override
    public Observable<Response<ResponseBody>> postFirmwareUpdateToUMod(UMod uMod, File newFirmwareFile) {

        Response<ResponseBody> mockedResp = Response.success(ResponseBody.create(MediaType.parse("text/plain"), "MOCKED"));
        return Observable.just(mockedResp).delay(500L, TimeUnit.MILLISECONDS);


    }

    @Override
    public Observable<Response<ResponseBody>> otaCommit(UMod uMod, OTACommitRPC.Arguments request) {

        Response<ResponseBody> mockedResp = Response.success(ResponseBody.create(MediaType.parse("text/plain"), "MOCKED"));
        return Observable.just(mockedResp).delay(500L, TimeUnit.MILLISECONDS);

    }

    @Override
    public Observable<FactoryResetRPC.Result> factoryResetUMod(UMod uMod, FactoryResetRPC.Arguments request) {

        //FactoryResetRPC.Response response = new FactoryResetRPC.Response(null, "MOCKED", null);
        final FactoryResetRPC.Result mockedResult = new FactoryResetRPC.Result("MOCKED RESET!");
        return Observable.just(mockedResult).delay(600L, TimeUnit.MILLISECONDS);

    }
}
