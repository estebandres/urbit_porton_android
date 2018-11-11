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

import com.google.common.base.Strings;
import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.SetGateStatusRPC;
import com.urbit_iot.onekey.data.source.PhoneConnectivityInfo;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import timber.log.Timber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class ResetUModCalibration extends SimpleUseCase<ResetUModCalibration.RequestValues, ResetUModCalibration.ResponseValues> {

    private final UModsRepository mUModsRepository;
    private String uModAPSSID;

    @Inject
    public ResetUModCalibration(@NonNull UModsRepository uModsRepository,
                                @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {

        final SetGateStatusRPC.Arguments reqArguments = new SetGateStatusRPC.Arguments(UMod.GateStatus.UNKNOWN.getStatusID());
        //TODO review operator usage
        mUModsRepository.cachedFirst();
        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(uMod -> {
                    uModAPSSID = "";
                    if (!Strings.isNullOrEmpty(uMod.getWifiSSID())){
                        uModAPSSID = uMod.getWifiSSID();
                    }
                    return mUModsRepository.setUModGateStatus(uMod, reqArguments)
                            .onErrorResumeNext(throwable -> {
                                if (throwable instanceof HttpException) {

                                    String errorMessage = "";
                                    try {
                                        errorMessage = ((HttpException) throwable).response().errorBody().string();
                                    } catch (IOException exc) {
                                        return Observable.error(exc);
                                    }
                                    int httpErrorCode = ((HttpException) throwable).response().code();

                                    Log.e("calibrate_uc", "calibrate Failed on error CODE:"
                                            + httpErrorCode
                                            + " MESSAGE: "
                                            + errorMessage);

                                    Timber.e("calibrate Failed on error CODE:"
                                            + httpErrorCode
                                            + " MESSAGE: "
                                            + errorMessage);

                                    //401 occurs when some admin deleted me
                                    if (httpErrorCode != 0 && (httpErrorCode == 401 || httpErrorCode == 403)) {
                                        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                        mUModsRepository.saveUMod(uMod);
                                        return Observable.error(new DeletedUserException(uMod));
                                    }
                                }
                                //TODO is this a desirable behaviour? What about the other error codes?
                                return Observable.error(throwable);
                            });
                })
                //A retry should be performed when a timeout is produce because a umod changed its address or is suddenly disconnected.
                .retry((retryCount, throwable) -> {
                    Log.e("calibrate_uc", "Retry count: " + retryCount
                            + " -- Excep msge: " + throwable.getMessage()
                            + "Excep Type: " + throwable.getClass().getSimpleName());
                    if (retryCount == 1
                            && (throwable instanceof IOException)
                            && values.isPhoneConnectedToWiFi()
                            && uModAPSSID.equals(values.getPhoneAPWifiSSID())) {
                        mUModsRepository.refreshUMods();
                        return true;
                    } else {
                        return false;
                    }
                })
                .map(ResponseValues::new);
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final String uModUUID;
        private boolean phoneConnectedToWiFi;
        private String phoneAPWifiSSID;

        public RequestValues(@NonNull String uModUUID,
                             boolean phoneConnectedToWiFi,
                             @NonNull String phoneAPWifiSSID) {
            this.uModUUID = checkNotNull(uModUUID, "uModUUID cannot be null!");
            this.phoneConnectedToWiFi = phoneConnectedToWiFi;
            this.phoneAPWifiSSID = checkNotNull(phoneAPWifiSSID,"phoneAPWifiSSID cannot be null!");
        }

        public String getUModUUID() {
            return uModUUID;
        }

        public boolean isPhoneConnectedToWiFi() {
            return phoneConnectedToWiFi;
        }

        public String getPhoneAPWifiSSID() {
            return phoneAPWifiSSID;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final SetGateStatusRPC.Result result;

        public ResponseValues(@NonNull SetGateStatusRPC.Result result) {
            this.result = checkNotNull(result, "result cannot be null!");
        }

        public SetGateStatusRPC.Result getResult() {
            return result;
        }
    }

    public static class DeletedUserException extends Exception{
        private UMod inaccessibleUMod;
        public DeletedUserException(UMod inaccessibleUMod){
            super("The user was deleted by an Admin.");
            this.inaccessibleUMod = inaccessibleUMod;
        }

        public UMod getInaccessibleUMod(){
            return this.inaccessibleUMod;
        }
    }
}
