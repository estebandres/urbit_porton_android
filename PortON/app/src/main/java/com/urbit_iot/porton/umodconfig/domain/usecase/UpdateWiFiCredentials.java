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
import android.util.Log;

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.SetWiFiRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.functions.Func1;

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

        uModsRepository.cachedFirst();
        return uModsRepository.getUMod(values.getmUModUUID())
                .flatMap(uMod -> {
                    if (uMod.getState() != UMod.State.AP_MODE || uMod.getAppUserLevel() != UModUser.Level.ADMINISTRATOR ){
                        return Observable.error(new NotAdminUserOrNotAPModeUModException());
                    }
                    SetWiFiRPC.Arguments setWiFiArgs = new SetWiFiRPC.Arguments(values.getmWiFiSSID(), values.getmWiFiPassword());
                    return uModsRepository.setWiFiAP(uMod,setWiFiArgs)
                            .onErrorResumeNext(throwable -> {
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
                                    /*
                                    if ((httpErrorCode == HttpURLConnection.HTTP_INTERNAL_ERROR
                                            && errorMessage.contains(Integer.toString(HttpURLConnection.HTTP_OK)))) {
                                        Log.e("setwifi_uc", "Set WiFi in progress!");
                                        SetWiFiRPC.Result result = new SetWiFiRPC.Result(500, "no se");
                                        return Observable.just(result);
                                    }
                                    */
                                }
                                throwable.printStackTrace();
                                return Observable.error(throwable);
                            })
                            .flatMap((Func1<SetWiFiRPC.Result, Observable<SetWiFiRPC.Result>>) result -> {
                                    Log.d("update_wifi", result.toString());
                                    uMod.setWifiSSID(values.getmWiFiSSID());
                                    uModsRepository.saveUMod(uMod);
                                return Observable.just(result);
                            });
                })
                //A retry should be performed when a timeout is produce because a umod changed its address or is suddenly disconnected.
                //This makes sense because this operations is done in LAN MODE only.
                .retry((retryCount, throwable) -> {
                    Log.e("setwifi_uc", "Retry count: " + retryCount +
                            "\n Excep msge: " + throwable.getMessage());
                    if (retryCount == 1//Just the one retry
                            && (throwable instanceof IOException)){
                        uModsRepository.refreshUMods();
                        return true;
                    } else {
                        return false;
                    }
                })
                .map(ResponseValues::new);
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

    public static final class NotAdminUserOrNotAPModeUModException extends Exception{
        NotAdminUserOrNotAPModeUModException(){
            super("User is not an Admin or the module isn't in AP_MODE.");
        }
    }
}
