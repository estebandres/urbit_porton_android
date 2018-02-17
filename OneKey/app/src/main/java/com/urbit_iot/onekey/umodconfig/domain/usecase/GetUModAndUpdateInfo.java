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

package com.urbit_iot.onekey.umodconfig.domain.usecase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.appuser.data.source.AppUserRepository;
import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.source.UModsRepository;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

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

        //TODO why not to take cached values and refresh on retry only.
        mUModsRepository.refreshUMods();

        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<UMod>>() {
                    @Override
                    public Observable<UMod> call(final UMod uMod) {
                        if (uMod.getState() == UMod.State.AP_MODE
                                && !values.getmConnectedWiFiAP().contains(uMod.getUUID())){
                            Log.d("getumod+info_uc","Not connected to an AP_MODE UMod!");
                            //return Observable.just(uMod);
                            return Observable.error(new UnconnectedFromAPModeUModException(uMod.getUUID()));
                        }
                        //TODO review behaviour
                        return mAppUserRepository.getAppUser()
                                .flatMap(new Func1<AppUser, Observable<UMod>>() {
                                    @Override
                                    public Observable<UMod> call(final AppUser appUser) {
                                        CreateUserRPC.Arguments createUserArgs = new CreateUserRPC.Arguments(appUser.getCredentialsString());
                                        return mUModsRepository.createUModUser(uMod, createUserArgs)
                                                .flatMap(new Func1<CreateUserRPC.Result, Observable<UMod>>() {
                                                    @Override
                                                    public Observable<UMod> call(CreateUserRPC.Result result) {
                                                        //User Creation Succeeded
                                                        Log.d("getumod+info_uc","User Creation Succeeded!");
                                                        uMod.setAppUserLevel(result.getUserLevel());
                                                        return Observable.just(uMod);
                                                    }
                                                })
                                                //TODO discus retry policy.
                                                .onErrorResumeNext(new Func1<Throwable, Observable<? extends UMod>>() {
                                                    @Override
                                                    public Observable<? extends UMod> call(Throwable throwable) {
                                                        Log.e("getumod+info_uc", "Create User Failed: " +throwable.getMessage());
                                                        if (throwable instanceof HttpException) {
                                                            //Check for HTTP UNAUTHORIZED error code
                                                            int httpErrorCode = ((HttpException) throwable).response().code();
                                                            //If user is already created
                                                            //Improve  httpErrorCode != 0 to isValidHttpCode(httpErrorCode)
                                                            if (httpErrorCode != 0) {
                                                                if (httpErrorCode == 401 || httpErrorCode == 403){
                                                                    uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                                    mUModsRepository.saveUMod(uMod);
                                                                    return Observable.just(uMod);
                                                                }
                                                                if (httpErrorCode == 409){
                                                                    //TODO evaluate the benefit of getting the user_type in the error body.
                                                                    //That may save us the next request (Deserialization using JSONObject)
                                                                    GetMyUserLevelRPC.Arguments getUserLevelArgs =
                                                                            new GetMyUserLevelRPC.Arguments(appUser.getPhoneNumber());
                                                                    return mUModsRepository.getUserLevel(uMod,getUserLevelArgs)
                                                                            .doOnError(new Action1<Throwable>() {
                                                                                @Override
                                                                                public void call(Throwable throwable) {
                                                                                    Log.e("getumod+info_uc","Get User Level Failed: " + throwable.getMessage());
                                                                                }
                                                                            })
                                                                            .flatMap(new Func1<GetMyUserLevelRPC.Result, Observable<UMod>>() {
                                                                                @Override
                                                                                public Observable<UMod> call(GetMyUserLevelRPC.Result result) {
                                                                                    Log.d("getumod+info_uc","Get User Level Succeeded!");
                                                                                    uMod.setAppUserLevel(result.getUserLevel());
                                                                                    mUModsRepository.saveUMod(uMod);
                                                                                    return Observable.just(uMod);
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        }
                                                        //when the error is from other source like a timeout it is forwarded to the presenter.
                                                        return Observable.error(throwable);
                                                        //return Observable.just(uMod);
                                                    }
                                                });
                                    }
                                });

                    }
                })
                .map(new Func1<UMod, ResponseValues>() {
            @Override
            public ResponseValues call(UMod uMod) {
                return new ResponseValues(uMod);
            }
        });
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

        public String getmConnectedWiFiAP() {
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
