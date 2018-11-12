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

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.rpc.SysGetInfoRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Retrieves a {@link UMod} from the {@link UModsRepository}.
 */
public class GetUModSystemInfo extends SimpleUseCase<GetUModSystemInfo.RequestValues, GetUModSystemInfo.ResponseValues> {

    private final UModsRepository uModsRepository;

    @Inject
    public GetUModSystemInfo(@NonNull UModsRepository tasksRepository,
                             @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        uModsRepository = tasksRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(RequestValues values) {
        //final SysGetInfoRPC.Request request = new SysGetInfoRPC.Request(new SysGetInfoRPC.Arguments(),"STEVEOO",234234145);

        final SysGetInfoRPC.Arguments getSysInfoArgs = new SysGetInfoRPC.Arguments();

        return uModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<SysGetInfoRPC.Result>>() {
                    @Override
                    public Observable<SysGetInfoRPC.Result> call(UMod uMod) {
                        return uModsRepository.getSystemInfo(uMod,getSysInfoArgs);
                    }
                })
                .map(new Func1<SysGetInfoRPC.Result, ResponseValues>() {
            @Override
            public ResponseValues call(SysGetInfoRPC.Result rpcResult) {
                return new ResponseValues(rpcResult);
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

        private SysGetInfoRPC.Result rpcResult;

        public ResponseValues(@NonNull SysGetInfoRPC.Result rpcResult) {
            this.rpcResult = checkNotNull(rpcResult, "rpcResult cannot be null!");
        }

        public SysGetInfoRPC.Result getRPCResponse() {
            return this.rpcResult;
        }
    }
}
