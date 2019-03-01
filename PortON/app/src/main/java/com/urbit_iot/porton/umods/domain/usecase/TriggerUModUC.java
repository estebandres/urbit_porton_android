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

package com.urbit_iot.porton.umods.domain.usecase;

import androidx.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Strings;
import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.TriggerRPC;
import com.urbit_iot.porton.data.source.PhoneConnectivity;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import timber.log.Timber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Executes Trigger RPC on a given UMod.
 */
public class TriggerUModUC extends SimpleUseCase<TriggerUModUC.RequestValues, TriggerUModUC.ResponseValues> {

    private final UModsRepository mUModsRepository;
    private final AppUserRepository mAppUserRepository;
    //TODO Ugly dependencies are ugly. Brakes dependency rule!
    private final PhoneConnectivity connectivityInfo;
    private String uModAPSSID;
    private UModUser.Level appUserLevel;

    @Inject
    public TriggerUModUC(@NonNull UModsRepository uModsRepository,
                         @NonNull BaseSchedulerProvider schedulerProvider,
                         @NonNull AppUserRepository mAppUserRepository,
                         @NonNull PhoneConnectivity connectivityInfo) {
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
                .flatMap(uMod -> {
                    uModAPSSID = "";
                    if (!Strings.isNullOrEmpty(uMod.getWifiSSID())){
                        uModAPSSID = uMod.getWifiSSID();
                    }
                    this.appUserLevel = uMod.getAppUserLevel();
                    return mUModsRepository.triggerUMod(uMod, requestArguments)
                            .onErrorResumeNext(throwable -> {
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
                                    if (httpErrorCode == 401 || httpErrorCode == 403){
                                        /*
                                        if (uMod.getAppUserLevel()== UModUser.Level.INVITED){
                                            mUModsRepository.deleteUMod(uMod.getUUID());
                                            return Observable.error(new DeletedUserException(uMod));
                                        }
                                        */
                                        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                        this.appUserLevel = UModUser.Level.UNAUTHORIZED;
                                        mUModsRepository.saveUMod(uMod);
                                        return Observable.error(new DeletedUserException(uMod));
                                    }
                                    //If the user was Admin but now is User and vice versa...
                                    if (httpErrorCode == 500){//body: "unauthorized"
                                        return mAppUserRepository.getAppUser()
                                                .flatMap(appUser -> {
                                                    Log.d("trigger_uc", "Get AppUser Success: "+appUser.toString());
                                                    GetUserLevelRPC.Arguments getUserLevelArgs =
                                                            new GetUserLevelRPC.Arguments(appUser.getUserName());
                                                    return mUModsRepository.getUserLevel(uMod,getUserLevelArgs)
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

                                                                    Timber.e("Get User Status (urbit:urbit) Failed on error CODE:"
                                                                            + httpErrorCode1
                                                                            + " MESSAGE: "
                                                                            + errorMessage1);
                                                                    //Could ocurr if
                                                                    if (httpErrorCode1 == 500
                                                                            && errorMessage1.contains("404")) {//TODO is this case possible?
                                                                                uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                                                this.appUserLevel = UModUser.Level.UNAUTHORIZED;
                                                                                mUModsRepository.saveUMod(uMod);//Careful icarus!!! uMod may change
                                                                                return Observable.error(new DeletedUserException(uMod));
                                                                    }
                                                                }
                                                                return Observable.error(throwable1);
                                                            })
                                                            .flatMap(result -> {
                                                                //TODO what about when someone changed my status to Guest????? (This is not allowed by the app)
                                                                Log.d("trigger_uc", "Get User Level Success: "+result.toString());
                                                                Timber.d("Get User Level Success: "+result.toString());
                                                                uMod.setAppUserLevel(result.getUserLevel());
                                                                this.appUserLevel = result.getUserLevel();
                                                                mUModsRepository.saveUMod(uMod);
                                                                return mUModsRepository.triggerUMod(uMod, requestArguments);
                                                            });
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
                            });
                })
                //A retry should be performed when a timeout is produce because a umod changed its address or is suddenly disconnected.
                .retry((retryCount, throwable) -> {
                    Log.e("trigger_uc", "Retry count: " + retryCount +
                            " -- Excep msge: " + throwable.getMessage() + "Excep Type: " + throwable.getClass().getSimpleName());
                    if (retryCount == 1
                            && (throwable instanceof IOException)
                            //This sort of data should be given as a request value to the usecase in order to isolate the layer.
                            && connectivityInfo.getConnectionType() == PhoneConnectivity.ConnectionType.WIFI
                            && uModAPSSID.equals(connectivityInfo.getWifiAPSSID())){
                        mUModsRepository.refreshUMods();
                        return true;
                    } else {
                        return false;
                    }
                })
                .map(result1 -> new ResponseValues(result1,this.appUserLevel));
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
        private final UModUser.Level appUserLevel;

        public ResponseValues(@NonNull TriggerRPC.Result result, UModUser.Level appUserLevel) {
            this.result = checkNotNull(result, "result cannot be null!");
            this.appUserLevel = appUserLevel;
        }

        public TriggerRPC.Result getResult() {
            return result;
        }

        public UModUser.Level getAppUserLevel() {
            return appUserLevel;
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
