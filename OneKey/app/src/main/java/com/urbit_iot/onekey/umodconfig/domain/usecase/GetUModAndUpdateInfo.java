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

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.appuser.data.source.AppUserRepository;
import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.source.UModsRepository;

import javax.inject.Inject;

import rx.Observable;
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
        final SysGetInfoRPC.Request infoRequest = new SysGetInfoRPC.Request(null, "GetSysInfo", 666);
        final CreateUserRPC.Request createUserRequest = new CreateUserRPC.Request(
                new CreateUserRPC.Arguments("AAA:BBBB:CCCC"),
                "CreateUser",
                666);

        mUModsRepository.refreshUMods();
        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<UMod>>() {
                    @Override
                    public Observable<UMod> call(final UMod uMod) {
                        if (uMod.getState() == UMod.State.AP_MODE
                                && !values.getmConnectedWiFiAP().contains(uMod.getUUID())){
                            return Observable.just(uMod);
                        }
                        return mUModsRepository.createUModUser(uMod, createUserRequest)
                                .flatMap(new Func1<CreateUserRPC.Response, Observable<UMod>>() {
                                    @Override
                                    public Observable<UMod> call(CreateUserRPC.Response response) {
                                        //If creation succeeds.
                                        if (response.getResponseError() == null){
                                            uMod.setAppUserLevel(response.getResponseResult().getUserLevel());
                                            return Observable.just(uMod);
                                        }
                                        //If error exists and isn't 400: User already exists.
                                        if (response.getResponseError() != null){
                                            //TODO check error code perhaps 409 is more appropriate
                                            //and save us the need for message parsing.
                                            if (response.getResponseError().getErrorCode() != 400
                                                    || !response.getResponseError().getErrorMessage()
                                                    .contains("already")){
                                                return Observable.error(
                                                        new Exception("createUser Error" +
                                                                response.getResponseError().
                                                                        getErrorMessage()));
                                            }
                                        }
                                        return mAppUserRepository.getAppUser()
                                                .flatMap(new Func1<AppUser, Observable<UMod>>() {
                                                    @Override
                                                    public Observable<UMod> call(AppUser appUser) {
                                                        GetMyUserLevelRPC.Request request =
                                                                new GetMyUserLevelRPC.Request(new GetMyUserLevelRPC.Arguments(appUser.getPhoneNumber()),uMod.getUUID());
                                                        return mUModsRepository.getUserLevel(uMod,request)//TODO should be called from other UseCase??
                                                                .flatMap(new Func1<GetMyUserLevelRPC.Response, Observable<UMod>>() {
                                                                    @Override
                                                                    public Observable<UMod> call(GetMyUserLevelRPC.Response successResponse) {
                                                                        uMod.setAppUserLevel(successResponse.getResponseResult().getUserLevel());
                                                                        mUModsRepository.saveUMod(uMod);
                                                                        return Observable.just(uMod);
                                                                    }
                                                                });
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
}
