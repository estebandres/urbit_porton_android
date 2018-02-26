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
import com.urbit_iot.onekey.data.rpc.SetWiFiAPRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Retrieves a {@link UMod} from the {@link UModsRepository}.
 */
public class UpdateWiFiCredentials extends SimpleUseCase<UpdateWiFiCredentials.RequestValues, UpdateWiFiCredentials.ResponseValues> {

    private final UModsRepository uModsRepository;

    @Inject
    public UpdateWiFiCredentials(@NonNull UModsRepository tasksRepository,
                                 @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        uModsRepository = tasksRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {

        /*
        final SetWiFiAPRPC.Request request =
                new SetWiFiAPRPC.Request(
                        new SetWiFiAPRPC.Arguments(values.getmWiFiSSID(),
                                values.getmWiFiPassword()),"SetWiFiAP", 666);
        */

        return uModsRepository.getUMod(values.getmUModUUID())
                .flatMap(new Func1<UMod, Observable<SetWiFiAPRPC.Result>>() {
                    @Override
                    public Observable<SetWiFiAPRPC.Result> call(final UMod uMod) {
                        SetWiFiAPRPC.Arguments setWiFiArgs = new SetWiFiAPRPC.Arguments(values.getmWiFiSSID(), values.getmWiFiPassword());
                        return uModsRepository.setWiFiAP(uMod,setWiFiArgs)
                                .flatMap(new Func1<SetWiFiAPRPC.Result, Observable<SetWiFiAPRPC.Result>>() {
                                    @Override
                                    public Observable<SetWiFiAPRPC.Result> call(SetWiFiAPRPC.Result result) {
                                            UMod tempUMod = uMod;
                                            tempUMod.setWifiSSID(values.getmWiFiSSID());
                                            Log.d("update_wifi", tempUMod.getWifiSSID());
                                            uModsRepository.saveUMod(tempUMod);
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
                        if (retryCount < 4 &&
                                (throwable instanceof IOException)){
                            uModsRepository.refreshUMods();
                            return true;
                        } else {
                            return false;
                        }
                    }
                })
                .map(new Func1<SetWiFiAPRPC.Result, ResponseValues>() {
            @Override
            public ResponseValues call(SetWiFiAPRPC.Result rpcResponse) {
                return new ResponseValues(rpcResponse);
            }
        });
    }


    public static final class RequestValues implements RxUseCase.RequestValues {

        private final String mUModUUID;
        private final String mWiFiSSID;
        private final String mWiFiPassword;

        public RequestValues(String mUModUUID, String mWiFiSSID, String mWiFiPassword) {
            this.mUModUUID = checkNotNull(mUModUUID, "uModUUID cannot be null!");
            this.mWiFiSSID = checkNotNull(mWiFiSSID, "mWiFiSSID cannot be null!");
            this.mWiFiPassword = checkNotNull(mWiFiPassword, "mWiFiPassword cannot be null!");
        }

        public String getmUModUUID() {
            return mUModUUID;
        }

        public String getmWiFiSSID() {
            return mWiFiSSID;
        }

        public String getmWiFiPassword() {
            return mWiFiPassword;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private SetWiFiAPRPC.Result rpcResult;

        public ResponseValues(@NonNull SetWiFiAPRPC.Result rpcResult) {
            this.rpcResult = checkNotNull(rpcResult, "rpcResult cannot be null!");
        }

        public SetWiFiAPRPC.Result getRpcResult() {
            return this.rpcResult;
        }
    }
}