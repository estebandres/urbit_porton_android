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

package com.urbit_iot.onekey.umods.domain.usecase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.appuser.data.source.AppUserRepository;
import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.APIUserType;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class RequestAccess extends SimpleUseCase<RequestAccess.RequestValues, RequestAccess.ResponseValues> {

    private final UModsRepository mUModsRepository;
    private final AppUserRepository mAppUserRepository;

    @NonNull
    private final PublishSubject<Void> retrySubject = PublishSubject.create();

    @Inject
    public RequestAccess(@NonNull UModsRepository uModsRepository,
                         @NonNull AppUserRepository appUserRepository,
                         @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
        this.mAppUserRepository = checkNotNull(appUserRepository, "appUserRepository cannot be null!");
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {
        /*
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }
        */
        //TODO figure what to do with rpc tag and id
        /*
        final CreateUserRPC.Request request =
                new CreateUserRPC.Request(
                        new CreateUserRPC.Arguments("AAAAAAAAA:BBBBBBBBBB:CCCCCCCC"),
                        "MockCreation",
                        666);
         */

        //int retryCounter = 0;
        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<CreateUserRPC.Result>>() {
                    @Override
                    public Observable<CreateUserRPC.Result> call(final UMod uMod) {
                        return mAppUserRepository.getAppUser()
                                .flatMap(new Func1<AppUser, Observable<CreateUserRPC.Result>>() {
                                    @Override
                                    public Observable<CreateUserRPC.Result> call(final AppUser appUser) {
                                        CreateUserRPC.Arguments requestArguments =
                                                new CreateUserRPC.Arguments(appUser.getCredentialsString());
                                        return mUModsRepository.createUModUser(uMod, requestArguments)
                                                .flatMap(new Func1<CreateUserRPC.Result, Observable<CreateUserRPC.Result>>() {
                                                    @Override
                                                    public Observable<CreateUserRPC.Result> call(final CreateUserRPC.Result createUserResult) {
                                                        //TODO ask if this is possible. Currently the API doc doesn't
                                                        // specify what data is returned as part of the result.
                                                        uMod.setAppUserLevel(createUserResult.getUserLevel());
                                                        mUModsRepository.saveUMod(uMod);
                                                        return Observable.just(createUserResult);
                                                    }
                                                })
                                                .onErrorResumeNext(new Func1<Throwable, Observable<CreateUserRPC.Result>>() {
                                                    @Override
                                                    public Observable<CreateUserRPC.Result> call(Throwable throwable) {

                                                        if (throwable instanceof HttpException){
                                                            //Check for HTTP UNAUTHORIZED error code
                                                            int httpErrorCode = ((HttpException) throwable).response().code();
                                                            Log.e("req_access_uc", "CreateUser Failed: " + httpErrorCode);
                                                            if (CreateUserRPC.ALLOWED_ERROR_CODES.contains(httpErrorCode)){
                                                                if (httpErrorCode == HttpURLConnection.HTTP_UNAUTHORIZED
                                                                || httpErrorCode ==  HttpURLConnection.HTTP_FORBIDDEN){
                                                                    Log.e("req_access_uc", "CreateUser failed to Auth with urbit:urbit CODE: " + httpErrorCode);
                                                                }
                                                                if (httpErrorCode == HttpURLConnection.HTTP_CONFLICT){
                                                                    //TODO evaluate the benefit of getting the user_type in the error body.
                                                                    //That may save us the next request (Deserialization using JSONObject)
                                                                    GetMyUserLevelRPC.Arguments getUserLevelArgs =
                                                                            new GetMyUserLevelRPC.Arguments(appUser.getPhoneNumber());
                                                                    return mUModsRepository.getUserLevel(uMod,getUserLevelArgs)
                                                                            .flatMap(new Func1<GetMyUserLevelRPC.Result, Observable<CreateUserRPC.Result>>() {
                                                                                @Override
                                                                                public Observable<CreateUserRPC.Result> call(GetMyUserLevelRPC.Result result) {
                                                                                    Log.d("getumod+info_uc","Get User Level Succeeded! " + result.toString());
                                                                                    uMod.setAppUserLevel(result.getUserLevel());
                                                                                    mUModsRepository.saveUMod(uMod);
                                                                                    return Observable.just(new CreateUserRPC.Result(result.getAPIUserType()));
                                                                                }
                                                                            })
                                                                            .onErrorResumeNext(new Func1<Throwable, Observable<? extends CreateUserRPC.Result>>() {
                                                                                @Override
                                                                                public Observable<? extends CreateUserRPC.Result> call(Throwable throwable) {
                                                                                    Log.e("getumod+info_uc","Get User Level Failed: " + throwable.getMessage());
                                                                                    if (throwable instanceof HttpException) {
                                                                                        int httpErrorCode = ((HttpException) throwable).response().code();
                                                                                        if (GetMyUserLevelRPC.ALLOWED_ERROR_CODES.contains(httpErrorCode)) {
                                                                                            if (httpErrorCode == HttpURLConnection.HTTP_UNAUTHORIZED
                                                                                                    || httpErrorCode ==  HttpURLConnection.HTTP_FORBIDDEN){
                                                                                                Log.e("req_access_uc", "GetLevel failed to Auth with urbit:urbit CODE: " + httpErrorCode);
                                                                                            }
                                                                                            if ((httpErrorCode == HttpURLConnection.HTTP_NOT_FOUND)) {
                                                                                                Log.e("req_access_uc", "CreateUser Failed: " + httpErrorCode);
                                                                                                uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                                                                mUModsRepository.saveUMod(uMod);
                                                                                                return Observable.error(throwable);
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    return Observable.error(throwable);
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        }
                                                        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                        mUModsRepository.saveUMod(uMod);
                                                        //TODO is this a desirable behaviour? What about the other error codes?
                                                        return Observable.error(throwable);
                                                    }
                                                });
                                    }
                                });
                    }
                })
                //TODO review retry policy
                //The RPC is executed in the saved umod connectionAddress
                //only when that address is outdated and produces an error a retry is performed that
                // forces a network lookup for the connected UMod.
                .retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer retryCount, Throwable throwable) {
                        Log.e("req_access_uc", "Retry count: " + retryCount + "\n Excep msge: " + throwable.getMessage());
                        if (retryCount < 3 && (throwable instanceof IOException)){
                            mUModsRepository.refreshUMods();
                            return true;
                        } else {
                            return false;
                        }
                    }
                })
                .map(new Func1<CreateUserRPC.Result, ResponseValues>() {
                    @Override
                    public ResponseValues call(CreateUserRPC.Result result) {
                        return new ResponseValues(result);
                    }
                });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final String uModUUID;

        public RequestValues(@NonNull String uModUUID) {
            this.uModUUID = checkNotNull(uModUUID, "uModUUID cannot be null!");
        }

        public String getUModUUID() {
            return uModUUID;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final CreateUserRPC.Result result;

        public ResponseValues(@NonNull CreateUserRPC.Result result) {
            this.result = checkNotNull(result, "result cannot be null!");
        }
        public CreateUserRPC.Result getResult() {
            return result;
        }
    }
}