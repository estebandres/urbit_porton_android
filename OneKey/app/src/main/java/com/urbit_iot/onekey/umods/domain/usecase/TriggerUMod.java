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
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class TriggerUMod extends SimpleUseCase<TriggerUMod.RequestValues, TriggerUMod.ResponseValues> {

    private final UModsRepository mUModsRepository;

    @Inject
    public TriggerUMod(@NonNull UModsRepository uModsRepository,
                       @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {
        // try first force update on retry.
        /*
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }
        */

        /*
        //TODO figure what to do with rpc tag and id
        final TriggerRPC.Request request = new TriggerRPC.Request(new TriggerRPC.Arguments(),
                "SteveTriggered",666);
         */
        final TriggerRPC.Arguments requestArguments = new TriggerRPC.Arguments();
        //TODO review operator usage
        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<TriggerRPC.Result>>() {
                    @Override
                    public Observable<TriggerRPC.Result> call(final UMod uMod) {
                        return mUModsRepository.triggerUMod(uMod, requestArguments)
                                .onErrorResumeNext(new Func1<Throwable, Observable<? extends TriggerRPC.Result>>() {
                                    @Override
                                    public Observable<? extends TriggerRPC.Result> call(Throwable throwable) {
                                        if (throwable instanceof HttpException){
                                            //Check for HTTP UNAUTHORIZED error code
                                            int httpErrorCode = ((HttpException) throwable).response().code();
                                            if (httpErrorCode != 0 && (httpErrorCode == 401 || httpErrorCode == 403)){
                                                Log.e("trigger_uc", "Trigger Failed: Auth Error: " + httpErrorCode);
                                                uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                mUModsRepository.saveUMod(uMod);
                                                //TODO what difference would it make if Observable.error(throwable)??
                                                //return Observable.error(new Exception("Forces umods UI Refresh"));
                                                //return Observable.error(throwable);
                                            }
                                        }
                                        //TODO is this a desirable behaviour? What about the other error codes?
                                        return Observable.error(throwable);
                                    }
                                });
                    }
                })
                //TODO find the scenarios where retry would be useful. When do we want a retry??
                //TODO how can we force the list refresh (load) when retry finish unsuccessful??
                //A retry should be performed when a timeout is produce because a umod changed its address or is suddenly disconnected.
                .retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer retryCount, Throwable throwable) {
                        Log.e("trigger_uc", "Retry count: " + retryCount +
                                " -- Excep msge: " + throwable.getMessage() + "Excep Type: " + throwable.getClass().getSimpleName());
                        if (retryCount < 4 &&
                                (throwable instanceof IOException)){
                            mUModsRepository.refreshUMods();
                            return true;
                        } else {
                            return false;
                        }
                    }
                })
                .map(new Func1<TriggerRPC.Result, ResponseValues>() {
                    @Override
                    public ResponseValues call(TriggerRPC.Result result) {
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

        private final TriggerRPC.Result result;

        public ResponseValues(@NonNull TriggerRPC.Result result) {
            this.result = checkNotNull(result, "result cannot be null!");
        }

        public TriggerRPC.Result getResult() {
            return result;
        }
    }
}
