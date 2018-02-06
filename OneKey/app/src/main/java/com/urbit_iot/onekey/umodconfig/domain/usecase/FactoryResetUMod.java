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
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

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
        final FactoryResetRPC.Request request = new FactoryResetRPC.Request(null,"FactoryReset",666);

        //TODO Is it neccesary? Perhaps the try and refresh-retry scheme is a good option.
        uModsRepository.refreshUMods();

        return uModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<FactoryResetRPC.Response>>() {
                    @Override
                    public Observable<FactoryResetRPC.Response> call(final UMod uMod) {
                        return uModsRepository.factoryResetUMod(uMod,request)
                                .delay(12L, TimeUnit.SECONDS)
                                .flatMap(new Func1<FactoryResetRPC.Response, Observable<FactoryResetRPC.Response>>() {
                                    @Override
                                    public Observable<FactoryResetRPC.Response> call(FactoryResetRPC.Response response) {
                                        if (response.getResponseError()!=null){
                                            return Observable.error(new Exception("Factory Reset RPC Failed."));
                                        } else {
                                            uModsRepository.deleteUMod(uMod.getUUID());
                                            return Observable.just(response);
                                        }
                                    }
                                });
                    }
                })
                .map(new Func1<FactoryResetRPC.Response, ResponseValues>() {
                    @Override
                    public ResponseValues call(FactoryResetRPC.Response response) {
                        return new ResponseValues(response);
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

        private FactoryResetRPC.Response rpcResponse;

        public ResponseValues(@NonNull FactoryResetRPC.Response rpcResponse) {
            this.rpcResponse = checkNotNull(rpcResponse, "rpcResponse cannot be null!");
        }

        public FactoryResetRPC.Response getRPCResponse() {
            return this.rpcResponse;
        }
    }
}
