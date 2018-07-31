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
import com.urbit_iot.onekey.appuser.data.source.AppUserRepository;
import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.GetUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.PhoneConnectivityInfo;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import timber.log.Timber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class TriggerUMod extends SimpleUseCase<TriggerUMod.RequestValues, TriggerUMod.ResponseValues> {

    private final UModsRepository mUModsRepository;
    private final AppUserRepository mAppUserRepository;
    //TODO Ugly dependencies are ugly. Brakes dependency rule!
    private final PhoneConnectivityInfo connectivityInfo;
    private String uModAPSSID;

    @Inject
    public TriggerUMod(@NonNull UModsRepository uModsRepository,
                       @NonNull BaseSchedulerProvider schedulerProvider,
                       @NonNull AppUserRepository mAppUserRepository,
                       @NonNull PhoneConnectivityInfo connectivityInfo) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
        this.mAppUserRepository = checkNotNull(mAppUserRepository,"mAppUserRepository cannot be null!");
        this.connectivityInfo = connectivityInfo;
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
        mUModsRepository.cachedFirst();
        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<TriggerRPC.Result>>() {
                    @Override
                    public Observable<TriggerRPC.Result> call(final UMod uMod) {
                        uModAPSSID = "";
                        return mUModsRepository.triggerUMod(uMod, requestArguments)
                                .onErrorResumeNext(new Func1<Throwable, Observable<? extends TriggerRPC.Result>>() {
                                    @Override
                                    public Observable<? extends TriggerRPC.Result> call(Throwable throwable) {
                                        if (throwable instanceof HttpException){

                                            String errorMessage = "";
                                            try {
                                                errorMessage = ((HttpException) throwable).response().errorBody().string();
                                            }catch (IOException exc){
                                                return Observable.error(exc);
                                            }
                                            int httpErrorCode = ((HttpException) throwable).response().code();

                                            Log.e("trigger_uc", "Trigger Failed on error CODE:"
                                                    +httpErrorCode
                                                    +" MESSAGE: "
                                                    + errorMessage);

                                            Timber.e("Trigger Failed on error CODE:"
                                                    +httpErrorCode
                                                    +" MESSAGE: "
                                                    + errorMessage);

                                            //401 occurs when some admin deleted me
                                            if (httpErrorCode != 0 && (httpErrorCode == 401 || httpErrorCode == 403)){
                                                uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                mUModsRepository.saveUMod(uMod);
                                                //TODO what difference would it make if Observable.error(throwable)??
                                                //return Observable.error(new Exception("Forces umods UI Refresh"));
                                                return Observable.error(new DeletedUserException(uMod));
                                            }
                                            //If the user was Admin but now is User and vice versa...
                                            if (httpErrorCode == 500){//body: "unauthorized"
                                                return mAppUserRepository.getAppUser()
                                                        .flatMap(new Func1<AppUser, Observable<TriggerRPC.Result>>() {
                                                            @Override
                                                            public Observable<TriggerRPC.Result> call(AppUser appUser) {
                                                                Log.d("trigger_uc", "Get AppUser Success: "+appUser.toString());
                                                                GetUserLevelRPC.Arguments getUserLevelArgs =
                                                                        new GetUserLevelRPC.Arguments(appUser.getUserName());
                                                                return mUModsRepository.getUserLevel(uMod,getUserLevelArgs)
                                                                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends GetUserLevelRPC.Result>>() {
                                                                            @Override
                                                                            public Observable<? extends GetUserLevelRPC.Result> call(Throwable throwable) {
                                                                                if (throwable instanceof HttpException) {

                                                                                    String errorMessage = "";
                                                                                    try {
                                                                                        errorMessage = ((HttpException) throwable).response().errorBody().string();
                                                                                    } catch (IOException exc) {
                                                                                        return Observable.error(exc);
                                                                                    }
                                                                                    int httpErrorCode = ((HttpException) throwable).response().code();

                                                                                    Log.e("trigger_uc", "Get User Status (urbit:urbit) Failed on error CODE:"
                                                                                            + httpErrorCode
                                                                                            + " MESSAGE: "
                                                                                            + errorMessage);

                                                                                    Timber.e("Get User Status (urbit:urbit) Failed on error CODE:"
                                                                                            + httpErrorCode
                                                                                            + " MESSAGE: "
                                                                                            + errorMessage);

                                                                                    if (httpErrorCode != 0
                                                                                        && httpErrorCode == 500
                                                                                            && errorMessage.contains("404")) {//TODO is this case possible?
                                                                                                uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                                                                mUModsRepository.saveUMod(uMod);//Careful icarus!!! uMod may change
                                                                                                return Observable.error(new DeletedUserException(uMod));
                                                                                    }
                                                                                }
                                                                                return Observable.error(throwable);
                                                                            }
                                                                        })
                                                                        .flatMap(new Func1<GetUserLevelRPC.Result, Observable<TriggerRPC.Result>>() {
                                                                            @Override
                                                                            public Observable<TriggerRPC.Result> call(GetUserLevelRPC.Result result) {
                                                                                //TODO what about when someone changed my status to Guest????? (This is not allowed by the app)
                                                                                Log.d("trigger_uc", "Get User Level Success: "+result.toString());
                                                                                Timber.d("Get User Level Success: "+result.toString());
                                                                                uMod.setAppUserLevel(result.getUserLevel());
                                                                                return mUModsRepository.triggerUMod(uMod, requestArguments);
                                                                            }
                                                                        });
                                                            }
                                                        });
                                            }
                                        }
                                        /*
                                        if (throwable instanceof IOException){
                                            mUModsRepository.refreshUMods();
                                        }
                                        */
                                        //TODO is this a desirable behaviour? What about the other error codes?
                                        return Observable.error(throwable);
                                    }
                                });
                    }
                })
                //A retry should be performed when a timeout is produce because a umod changed its address or is suddenly disconnected.
                .retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer retryCount, Throwable throwable) {
                        Log.e("trigger_uc", "Retry count: " + retryCount +
                                " -- Excep msge: " + throwable.getMessage() + "Excep Type: " + throwable.getClass().getSimpleName());
                        if (retryCount == 1
                                && (throwable instanceof IOException)
                                //This sort of data should be given as a request value to the usecase in order to isolate the layer.
                                && connectivityInfo.getConnectionType() == PhoneConnectivityInfo.ConnectionType.WIFI
                                && uModAPSSID.equals(connectivityInfo.getWifiAPSSID())){
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
