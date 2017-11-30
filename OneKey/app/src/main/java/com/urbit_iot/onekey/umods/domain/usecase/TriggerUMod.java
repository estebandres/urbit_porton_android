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

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.rpc.RPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

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
        /*
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }
        */

        final TriggerRPC.Request request = new TriggerRPC.Request(new TriggerRPC.Arguments(),
                "SteveTriggered",666);
        return mUModsRepository.triggerUMod(values.getUMod(), request)
                .onErrorResumeNext(new Func1<Throwable, Observable<TriggerRPC.SuccessResponse>>() {
                    @Override
                    public Observable<TriggerRPC.SuccessResponse> call(Throwable throwable) {
                        return mUModsRepository.getUMod(values.getUMod().getUUID())
                                .flatMap(new Func1<UMod, Observable<TriggerRPC.SuccessResponse>>() {
                                    @Override
                                    public Observable<TriggerRPC.SuccessResponse> call(UMod uMod) {
                                        return mUModsRepository.triggerUMod(values.getUMod(),request);
                                    }
                                });
                    }
                })
                .map(new Func1<TriggerRPC.SuccessResponse, ResponseValues>() {
                    @Override
                    public ResponseValues call(TriggerRPC.SuccessResponse response) {
                        return new ResponseValues(response);
                    }
                });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final UMod uMod;

        public RequestValues(@NonNull UMod uMod) {
            this.uMod = checkNotNull(uMod, "uMod cannot be null!");
        }

        public UMod getUMod() {
            return uMod;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final TriggerRPC.SuccessResponse response;

        public ResponseValues(@NonNull TriggerRPC.SuccessResponse response) {
            this.response = checkNotNull(response, "response cannot be null!");
        }

        public TriggerRPC.SuccessResponse getResponse() {
            return response;
        }
    }
}
