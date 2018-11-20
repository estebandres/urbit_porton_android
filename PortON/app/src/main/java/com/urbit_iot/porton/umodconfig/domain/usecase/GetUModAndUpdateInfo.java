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

package com.urbit_iot.porton.umodconfig.domain.usecase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Strings;
import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.SysGetInfoRPC;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.source.UModsRepository;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Completable;
import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Retrieves a {@link UMod} from the {@link UModsRepository}.
 */
public class GetUModAndUpdateInfo extends SimpleUseCase<GetUModAndUpdateInfo.RequestValues, GetUModAndUpdateInfo.ResponseValues> {
    @NonNull
    private final UModsRepository mUModsRepository;
    @NonNull
    private final AppUserRepository mAppUserRepository;

    @Inject
    public GetUModAndUpdateInfo(@NonNull UModsRepository mUModsRepository,
                                @NonNull AppUserRepository mAppUserRepository,
                                @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        this.mUModsRepository = mUModsRepository;
        this.mAppUserRepository = mAppUserRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {

        //GOLDEN RULE when an object is passed in the rxjava chain and its modifications are
        //evident then there is no need to make a clone but if said object is passed to some external method as a parameter
        //then it has to be cloned since we are not sure what that method does to our object avoiding miss behaviours.
        //
        //Observable<UMod> cacheFirst = Completable.fromAction(mUModsRepository::cachedFirst).andThen(mUModsRepository.getUMod(values.getUModUUID()));
        //mUModsRepository.getUMod(values.getUModUUID()).switchIfEmpty(refreshedUMod)
        //TODO Why doesn't the first approach worked?? On LGq6
        Observable<UMod> refreshedUMod = Completable.fromAction(mUModsRepository::refreshUMods).andThen(mUModsRepository.getUMod(values.getUModUUID()));
        mUModsRepository.cachedFirst();

        return Observable.concatDelayError(mUModsRepository.getUMod(values.getUModUUID()),refreshedUMod)
                .first()
                .flatMap(uMod -> {
                    if (uMod.getState() == UMod.State.AP_MODE
                            && !values.getConnectedWiFiAP().contains(uMod.getUUID())){
                        Log.d("getumod+info_uc","Not connected to an AP_MODE UMod!\n" + uMod.toString());
                        //return Observable.just(uMod);
                        return Observable.error(new UnconnectedFromAPModeUModException(uMod.getUUID()));
                    }
                    //TODO review behaviour
                    return mAppUserRepository.getAppUser().flatMap(appUser -> {
                        CreateUserRPC.Arguments createUserArgs = new CreateUserRPC.Arguments(appUser.getCredentialsString());
                        return mUModsRepository.createUModUser(uMod, createUserArgs).flatMap(result -> {
                                    //User Creation Succeeded
                                    Log.d("getumod+info_uc","User Creation Succeeded!");
                                    uMod.setMqttResponseTopic(appUser.getUserName());
                                    uMod.setAppUserLevel(result.getUserLevel());

                                    mUModsRepository.saveUMod(uMod);
                                    if (uMod.isInAPMode()){
                                        return mUModsRepository.getCurrentLocation().flatMap(location -> {
                                            uMod.setuModLocation(location);
                                            mUModsRepository.saveUMod(uMod);
                                            return Observable.just(uMod);
                                        }).switchIfEmpty(Observable.just(uMod));
                                    }
                                    return Observable.just(uMod);
                                })
                                //TODO discus retry policy.
                                .onErrorResumeNext(throwable -> {
                                    Log.e("getumod+info_uc", "Create User Failed: " +throwable.getMessage());
                                    if (throwable instanceof HttpException) {
                                        //Check for HTTP UNAUTHORIZED error code
                                        int httpErrorCode = ((HttpException) throwable).response().code();
                                        String errorMessage = "";
                                        try {
                                            errorMessage = ((HttpException) throwable).response().errorBody().string();
                                        }catch (Exception exc){
                                            return Observable.error(exc);
                                        }

                                        Log.e("getumod+info_uc", "Get User Status (urbit:urbit) Failed on error CODE:"
                                                +httpErrorCode
                                                +" MESSAGE: "
                                                + errorMessage);

                                        //If user is already created
                                        //Improve  httpErrorCode != 0 to isValidHttpCode(httpErrorCode)
                                        if (httpErrorCode != 0) {
                                            if (httpErrorCode == 401 || httpErrorCode == 403){
                                                uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                mUModsRepository.saveUMod(uMod);//Careful icarus!!! uMod may change
                                                return Observable.just(uMod);
                                            }
                                            if (httpErrorCode == 409
                                                    || errorMessage.contains("409")
                                                    || errorMessage.contains("412")){
                                                //TODO evaluate the benefit of getting the user_type in the error body.
                                                //That may save us the next request (Deserialization using JSONObject)
                                                GetUserLevelRPC.Arguments getUserLevelArgs =
                                                        new GetUserLevelRPC.Arguments(appUser.getUserName());
                                                return mUModsRepository.getUserLevel(uMod,getUserLevelArgs).flatMap(result -> {
                                                            Log.d("getumod+info_uc","Get User Level Succeeded!: "+result.toString());
                                                            uMod.setAppUserLevel(result.getUserLevel());
                                                            //uMod.setMqttResponseTopic(appUser.getUserName());
                                                            //TODO when CACHE state doesnt display wifi cred settings. Change mapping on presenter!!
                                                           // uMod.setuModSource(UMod.UModSource.LAN_SCAN);
                                                            mUModsRepository.saveUMod(uMod);//Careful icarus!!! uMod may change
                                                            return Observable.just(uMod);
                                                        })
                                                        .onErrorResumeNext(throwable1 -> {
                                                            Log.e("getumod+info_uc", "Get User Status Failed: " + throwable1.getMessage());
                                                            if (throwable1 instanceof HttpException) {
                                                                //Check for HTTP UNAUTHORIZED error code
                                                                int httpErrorCode1 = ((HttpException) throwable1).response().code();
                                                                String errorMessage1 = "";
                                                                try {
                                                                    errorMessage1 = ((HttpException) throwable1).response().errorBody().string();
                                                                } catch (Exception exc) {
                                                                    return Observable.error(exc);
                                                                }

                                                                Log.e("getumod+info_uc", "Get User Status (urbit:urbit) Failed on error CODE:"
                                                                        + httpErrorCode1
                                                                        +" MESSAGE: "
                                                                        + errorMessage1);

                                                                //If user is already created
                                                                //Improve  httpErrorCode != 0 to isValidHttpCode(httpErrorCode)
                                                                if (httpErrorCode1 != 0) {
                                                                    if (httpErrorCode1 == 500){
                                                                        if (errorMessage1.contains("user not found")){
                                                                            uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                                            mUModsRepository.saveUMod(uMod);//Careful icarus!!! uMod may change
                                                                            return Observable.just(uMod);
                                                                        }
                                                                        //It also can fail on: BAD_REQUEST, "userName is required" but we cannot overcome that error.
                                                                    }
                                                                }
                                                            }
                                                            //when the error is from other source like a timeout it is forwarded to the presenter.
                                                            return Observable.error(throwable1);
                                                        });
                                            }
                                        }
                                    }
                                    //when the error is from other source like a timeout it is forwarded to the presenter.
                                    return Observable.error(throwable);
                                });
                    });

                })
                .flatMap(uMod -> {
                    SysGetInfoRPC.Arguments args = new SysGetInfoRPC.Arguments();
                    return mUModsRepository.getSystemInfo(uMod,args)
                            //This request should not fail since it is done with urbit:urbit
                            .flatMap(result -> {
                                //TODO review: What fields should be updated??
                                Log.d("getumod+info_uc", result.toString());
                                uMod.setSWVersion(result.getFwVersion());
                                uMod.setWifiSSID(result.getWifi().getSsid());
                                uMod.setMacAddress(result.getMac());
                                if (uMod.getState() == UMod.State.AP_MODE){
                                    if (!Strings.isNullOrEmpty(result.getWifi().getStaIp())){
                                        uMod.setConnectionAddress(result.getWifi().getStaIp());
                                    }
                                }
                                mUModsRepository.saveUMod(uMod);
                                return Observable.just(uMod);
                            });
                })
                .retry((retryCount, throwable) -> {
                    Log.e("config_gathering", "Retry count: " + retryCount + "\n Excep msge: " + throwable.getMessage());
                    if (retryCount == 1 //Two attempts maximum
                            && (throwable instanceof IOException)){
                        mUModsRepository.refreshUMods();
                        return true;
                    } else {
                        return false;
                    }
                })
                .map(ResponseValues::new);
    }


    public static final class RequestValues implements RxUseCase.RequestValues {

        private final String mUModUUID;
        private final String mConnectedWiFiAP;

        public RequestValues(@NonNull String uModUUID, String mConnectedWiFiAP) {
            mUModUUID = checkNotNull(uModUUID, "uModUUID cannot be null!");
            this.mConnectedWiFiAP = mConnectedWiFiAP;
        }

        public String getUModUUID() {
            return mUModUUID;
        }

        public String getConnectedWiFiAP() {
            return mConnectedWiFiAP;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private UMod mUMod;

        public ResponseValues(@NonNull UMod uMod) {
            mUMod = checkNotNull(uMod, "uMod cannot be null!");
        }

        public UMod getUMod() {
            return mUMod;
        }
    }

    public static class UnconnectedFromAPModeUModException extends Exception{
        private String mAPModeUModUUID;
        public UnconnectedFromAPModeUModException(String uModUUID){
            super("Trying to configure the unconnected AP_MODE umod: " + uModUUID);
            this.mAPModeUModUUID = uModUUID;
        }
        public String getAPModeUModUUID(){
            return this.mAPModeUModUUID;
        }
    }
}