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

package com.urbit_iot.onekey.umodsnotification.domain.usecase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.fernandocejas.frodo.annotation.RxLogObservable;
import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.appuser.data.source.AppUserRepository;
import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.GetUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class TriggerUModByNotif extends SimpleUseCase<TriggerUModByNotif.RequestValues, TriggerUModByNotif.ResponseValues> {

    private final UModsRepository mUModsRepository;
    private final AppUserRepository mAppUserRepository;

    @Inject
    public TriggerUModByNotif(@NonNull UModsRepository uModsRepository,
                              @NonNull BaseSchedulerProvider schedulerProvider, AppUserRepository mAppUserRepository) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
        this.mAppUserRepository = checkNotNull(mAppUserRepository,"mAppUserRepository cannot be null!");
    }

    //@RxLogObservable
    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {

        final TriggerRPC.Arguments requestArguments = new TriggerRPC.Arguments();
        //TODO review operator usage
        mUModsRepository.cachedFirst();
        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(uMod -> {
                    Log.d("trigger_notif",uMod.toString());
                    return mUModsRepository.triggerUMod(uMod, requestArguments)
                            .onErrorResumeNext(throwable -> {
                                if (throwable instanceof HttpException) {

                                    String errorMessage = "";
                                    try {
                                        errorMessage = ((HttpException) throwable).response().errorBody().string();
                                    } catch (IOException exc) {
                                        return Observable.error(exc);
                                    }
                                    int httpErrorCode = ((HttpException) throwable).response().code();

                                    Log.e("trigger_uc", "Trigger Failed on error CODE:"
                                            + httpErrorCode
                                            + " MESSAGE: "
                                            + errorMessage);
                                    //401 occurs when some admin deleted me
                                    if (httpErrorCode != 0 && (httpErrorCode == 401 || httpErrorCode == 403)) {
                                        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                        mUModsRepository.saveUMod(uMod);
                                        //TODO what difference would it make if Observable.error(throwable)??
                                        //return Observable.error(new Exception("Forces umods UI Refresh"));
                                        //return Observable.error(throwable);
                                    }
                                    //If the user was Admin but now is User and vice versa...
                                    if (httpErrorCode == 500) {//body: "unauthorized"
                                        return mAppUserRepository.getAppUser()
                                                .flatMap(appUser -> {
                                                    Log.d("trigger_uc", "Get AppUser Success: " + appUser.toString());
                                                    GetUserLevelRPC.Arguments getUserLevelArgs =
                                                            new GetUserLevelRPC.Arguments(appUser.getUserName());
                                                    return mUModsRepository.getUserLevel(uMod, getUserLevelArgs)
                                                            .onErrorResumeNext(throwable1 -> {
                                                                if (throwable1 instanceof HttpException) {

                                                                    String errorMessage1 = "";
                                                                    try {
                                                                        errorMessage1 = ((HttpException) throwable1).response().errorBody().string();
                                                                    } catch (IOException exc) {
                                                                        return Observable.error(exc);
                                                                    }
                                                                    int httpErrorCode1 = ((HttpException) throwable1).response().code();

                                                                    Log.e("trigger_uc", "Get User Status (urbit:urbit) Failed on error CODE:"
                                                                            + httpErrorCode1
                                                                            + " MESSAGE: "
                                                                            + errorMessage1);
                                                                    if (httpErrorCode1 != 0
                                                                            && httpErrorCode1 == 500
                                                                            && errorMessage1.contains("404")) {
                                                                        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                                        mUModsRepository.saveUMod(uMod);//Careful icarus!!! uMod may change
                                                                        return Observable.error(throwable1);//TODO Replace by custom exception
                                                                    }
                                                                }
                                                                return Observable.error(throwable1);
                                                            })
                                                            .flatMap(result -> {
                                                                //TODO what about when someone changed my status to Guest????? (This is not allowed by the app)
                                                                Log.d("trigger_uc", "Get User Level Success: " + result.toString());
                                                                uMod.setAppUserLevel(result.getUserLevel());
                                                                return mUModsRepository.triggerUMod(uMod, requestArguments);
                                                            });
                                                });
                                    }
                                }
                                if (throwable instanceof IOException){
                                    mUModsRepository.refreshUMods();
                                }
                                //TODO is this a desirable behaviour? What about the other error codes?
                                return Observable.error(throwable);
                            });
                })
                .map(ResponseValues::new);
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
