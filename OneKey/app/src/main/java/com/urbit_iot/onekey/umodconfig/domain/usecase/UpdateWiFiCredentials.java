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
import com.urbit_iot.onekey.data.rpc.SetWiFiRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
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
        final SetWiFiRPC.Request request =
                new SetWiFiRPC.Request(
                        new SetWiFiRPC.Arguments(values.getmWiFiSSID(),
                                values.getmWiFiPassword()),"SetWiFiAP", 666);
        */

        return uModsRepository.getUMod(values.getmUModUUID())
                .flatMap(new Func1<UMod, Observable<SetWiFiRPC.Result>>() {
                    @Override
                    public Observable<SetWiFiRPC.Result> call(final UMod uMod) {
                        SetWiFiRPC.Arguments setWiFiArgs = new SetWiFiRPC.Arguments(values.getmWiFiSSID(), values.getmWiFiPassword());
                        return uModsRepository.setWiFiAP(uMod,setWiFiArgs)
                                .onErrorResumeNext(new Func1<Throwable, Observable<? extends SetWiFiRPC.Result>>() {
                                    @Override
                                    public Observable<SetWiFiRPC.Result> call(Throwable throwable) {
                                        Log.e("setwifi_uc","Set WiFi Failure: " + throwable.getMessage());
                                        if (throwable instanceof HttpException) {
                                            String errorMessage = "";
                                            try {
                                                errorMessage = ((HttpException) throwable).response().errorBody().string();
                                            }catch (IOException exc){
                                                return Observable.error(exc);
                                            }
                                            int httpErrorCode = ((HttpException) throwable).response().code();

                                            Log.e("setwifi_uc", "Set WiFi Failure on error CODE:"
                                                    + httpErrorCode
                                                    + " MESSAGE: "
                                                    + errorMessage);

                                            if (httpErrorCode == HttpURLConnection.HTTP_UNAUTHORIZED
                                                    || httpErrorCode ==  HttpURLConnection.HTTP_FORBIDDEN){
                                                Log.e("setwifi_uc", "Set WiFi Failure on AUTH CODE: " + httpErrorCode);
                                            }
                                            if ((httpErrorCode == HttpURLConnection.HTTP_INTERNAL_ERROR
                                                    && errorMessage.contains(Integer.toString(HttpURLConnection.HTTP_OK)))) {
                                                Log.e("setwifi_uc", "Set WiFi in progress!");
                                                SetWiFiRPC.Result result = new SetWiFiRPC.Result(500, "no se");
                                                return Observable.just(result);
                                            }
                                        }
                                        throwable.printStackTrace();
                                        return Observable.error(throwable);
                                    }
                                })
                                .flatMap(new Func1<SetWiFiRPC.Result, Observable<SetWiFiRPC.Result>>() {
                                    @Override
                                    public Observable<SetWiFiRPC.Result> call(SetWiFiRPC.Result result) {
                                            //UMod tempUMod = uMod;
                                            //tempUMod.setWifiSSID(values.getmWiFiSSID());
                                            //Log.d("update_wifi", tempUMod.getWifiSSID());
                                            Log.d("update_wifi", result.toString());
                                            //uModsRepository.saveUMod(tempUMod);
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
                        Log.e("setwifi_uc", "Retry count: " + retryCount +
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
                .map(new Func1<SetWiFiRPC.Result, ResponseValues>() {
            @Override
            public ResponseValues call(SetWiFiRPC.Result rpcResponse) {
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

        private SetWiFiRPC.Result rpcResult;

        public ResponseValues(@NonNull SetWiFiRPC.Result rpcResult) {
            this.rpcResult = checkNotNull(rpcResult, "rpcResult cannot be null!");
        }

        public SetWiFiRPC.Result getRpcResult() {
            return this.rpcResult;
        }
    }
}
