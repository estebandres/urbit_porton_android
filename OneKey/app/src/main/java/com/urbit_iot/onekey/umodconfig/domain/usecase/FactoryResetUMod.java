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
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Retrieves a {@link UMod} from the {@link UModsRepository}.
 */
public class FactoryResetUMod extends SimpleUseCase<FactoryResetUMod.RequestValues, FactoryResetUMod.ResponseValues> {

    private final UModsRepository uModsRepository;

    @Inject
    public FactoryResetUMod(@NonNull UModsRepository tasksRepository,
                            @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        uModsRepository = tasksRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(RequestValues values) {
        //final FactoryResetRPC.Request request = new FactoryResetRPC.Request(null,"FactoryReset",666);
        final FactoryResetRPC.Arguments requestArguments = new FactoryResetRPC.Arguments();

        //TODO Is it neccesary? Perhaps the try and refresh-retry scheme is a good option.
        uModsRepository.refreshUMods();

        return uModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<FactoryResetRPC.Result>>() {
                    @Override
                    public Observable<FactoryResetRPC.Result> call(final UMod uMod) {
                        return uModsRepository.factoryResetUMod(uMod,requestArguments)
                                .onErrorResumeNext(new Func1<Throwable, Observable<? extends FactoryResetRPC.Result>>() {
                                    @Override
                                    public Observable<FactoryResetRPC.Result> call(Throwable throwable) {
                                        Log.e("factory-reset_uc","Factory Reset Failure: " + throwable.getMessage());
                                        if (throwable instanceof HttpException) {
                                            String errorMessage = "";
                                            try {
                                                errorMessage = ((HttpException) throwable).response().errorBody().string();
                                            }catch (IOException exc){
                                                return Observable.error(exc);
                                            }
                                            int httpErrorCode = ((HttpException) throwable).response().code();

                                            Log.e("factory-reset_uc", "Factory Reset Failure on error CODE:"
                                                    + httpErrorCode
                                                    + " MESSAGE: "
                                                    + errorMessage);

                                            if (httpErrorCode == HttpURLConnection.HTTP_UNAUTHORIZED
                                                    || httpErrorCode ==  HttpURLConnection.HTTP_FORBIDDEN){
                                                Log.e("factory-reset_uc", "Factory Reset Failure on AUTH CODE: " + httpErrorCode);
                                            }
                                            if ((httpErrorCode == HttpURLConnection.HTTP_INTERNAL_ERROR
                                                    && errorMessage.contains(Integer.toString(HttpURLConnection.HTTP_OK)))) {
                                                Log.e("factory-reset_uc", "Factory Succeed!");
                                                FactoryResetRPC.Result result = new FactoryResetRPC.Result();
                                                return Observable.just(result);
                                            }
                                        }
                                        return Observable.error(throwable);
                                    }
                                })
                                //wait 12 seconds for umod restart
                                .delay(12L, TimeUnit.SECONDS)
                                .flatMap(new Func1<FactoryResetRPC.Result, Observable<FactoryResetRPC.Result>>() {
                                    @Override
                                    public Observable<FactoryResetRPC.Result> call(FactoryResetRPC.Result result) {
                                        uModsRepository.deleteUMod(uMod.getUUID());
                                        return Observable.just(result);
                                    }
                                });
                    }
                })
                //TODO find the scenarios where retry would be useful. When do we want a retry??
                //A retry should be performed when a timeout is produce because a umod changed its address or is suddenly disconnected.
                .retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer retryCount, Throwable throwable) {
                        Log.e("factory-reset_uc", "Retry count: " + retryCount +
                                "\n Excep msge: " + throwable.getMessage());
                        if (retryCount <= 2 &&
                                (throwable instanceof IOException)){
                            uModsRepository.refreshUMods();
                            return true;
                        } else {
                            return false;
                        }
                    }
                })
                .map(new Func1<FactoryResetRPC.Result, ResponseValues>() {
                    @Override
                    public ResponseValues call(FactoryResetRPC.Result result) {
                        return new ResponseValues(result);
                    }
                });
    }


    public static final class RequestValues implements RxUseCase.RequestValues {

        private final String mUModUUID;

        public RequestValues(@NonNull String uModUUID) {
            mUModUUID = checkNotNull(uModUUID, "uModUUID cannot be null!");
        }

        public String getUModUUID() {
            return mUModUUID;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private FactoryResetRPC.Result rpcResult;

        public ResponseValues(@NonNull FactoryResetRPC.Result rpcResult) {
            this.rpcResult = checkNotNull(rpcResult, "rpcResult cannot be null!");
        }

        public FactoryResetRPC.Result getRpcResult() {
            return this.rpcResult;
        }
    }
}
