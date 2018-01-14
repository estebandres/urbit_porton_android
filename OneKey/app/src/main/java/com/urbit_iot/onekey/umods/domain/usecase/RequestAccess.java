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
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.RPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class RequestAccess extends SimpleUseCase<RequestAccess.RequestValues, RequestAccess.ResponseValues> {

    private final UModsRepository mUModsRepository;
    @NonNull
    private final PublishSubject<Void> retrySubject = PublishSubject.create();

    @Inject
    public RequestAccess(@NonNull UModsRepository uModsRepository,
                         @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {
        /*
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }
        */
        //TODO figure what to do with rpc tag and id
        final CreateUserRPC.Request request =
                new CreateUserRPC.Request(
                        new CreateUserRPC.Arguments("AAAAAAAAA:BBBBBBBBBB:CCCCCCCC"),
                        "MockCreation",
                        666);

        //int retryCounter = 0;
        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<CreateUserRPC.Response>>() {
                    @Override
                    public Observable<CreateUserRPC.Response> call(final UMod uMod) {
                        return mUModsRepository.createUModUser(uMod, request)
                                .onErrorResumeNext(new Func1<Throwable, Observable<CreateUserRPC.Response>>() {
                                    @Override
                                    public Observable<CreateUserRPC.Response> call(Throwable throwable) {
                                        if (throwable instanceof HttpException){
                                            //Check for HTTP ANAUTHORIZED error code
                                            int httpErrorCode = ((HttpException) throwable).response().code();
                                            if (httpErrorCode != 0 && (httpErrorCode == 401 || httpErrorCode == 403)){
                                                uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                mUModsRepository.saveUMod(uMod);
                                                return Observable.error(new Exception("Forces umods UI Refresh"));
                                            }
                                        }
                                        //TODO is this a desirable behaviour? What about the other error codes?
                                        return Observable.error(throwable);
                                    }
                                })
                                //Mongoose implementation always returns a successful response and
                                // if an error occurs then its details are embedded
                                // in the response JSON body.
                                .flatMap(new Func1<CreateUserRPC.Response, Observable<CreateUserRPC.Response>>() {
                                    @Override
                                    public Observable<CreateUserRPC.Response> call(final CreateUserRPC.Response createUserResponse) {
                                        RPC.ResponseError responseError = createUserResponse.getResponseError();
                                        //
                                        if (responseError != null){
                                            Integer errorCode = responseError.getErrorCode();
                                            if (errorCode!= null && errorCode != 0
                                                    && (errorCode == 401 || errorCode == 403)){
                                                //this is the lazy alternative. One could RPC for the
                                                // user level and update the UMod registry accordingly.
                                                uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                mUModsRepository.saveUMod(uMod);
                                                return Observable.error(new Exception("Forces umods UI Refresh"));
                                            }
                                        } else {
                                            //TODO ask if this is possible. Currently the API doesn't
                                            // specify what data is returned as part of the response.
                                            uMod.setAppUserLevel(createUserResponse.getResponseResult().getUserLevel());
                                            mUModsRepository.saveUMod(uMod);
                                        }
                                        return Observable.just(createUserResponse);
                                    }
                                });
                    }
                })
                //The RPC is executed in the saved umod connectionAddress
                //only when that address is outdated and produces an error a retry is done that
                // forces the network lookup for the connected UMod.
                .retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer retryCount, Throwable throwable) {
                        Log.e("req_access_uc", "Retry count: " + retryCount + "\n Excep msge: " + throwable.getMessage());
                        if (retryCount < 3 && (throwable instanceof IOException ||
                                throwable instanceof HttpException)){
                            mUModsRepository.refreshUMods();
                            return true;
                        } else {
                            return false;
                        }
                    }
                })
                .map(new Func1<CreateUserRPC.Response, ResponseValues>() {
                    @Override
                    public ResponseValues call(CreateUserRPC.Response response) {
                        return new ResponseValues(response);
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

        private final CreateUserRPC.Response response;

        public ResponseValues(@NonNull CreateUserRPC.Response response) {
            this.response = checkNotNull(response, "response cannot be null!");
        }
        public CreateUserRPC.Response getResponse() {
            return response;
        }
    }
}