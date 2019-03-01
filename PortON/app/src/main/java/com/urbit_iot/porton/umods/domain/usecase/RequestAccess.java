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

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.subjects.PublishSubject;
import timber.log.Timber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class RequestAccess extends SimpleUseCase<RequestAccess.RequestValues, RequestAccess.ResponseValues> {

    private final UModsRepository mUModsRepository;
    private final AppUserRepository mAppUserRepository;
    private final BaseSchedulerProvider schedulerProvider;

    @NonNull
    private final PublishSubject<Void> retrySubject = PublishSubject.create();

    @Inject
    public RequestAccess(@NonNull UModsRepository uModsRepository,
                         @NonNull AppUserRepository appUserRepository,
                         @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
        this.mAppUserRepository = checkNotNull(appUserRepository, "appUserRepository cannot be null!");
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {

        /*
        Observable<UMod> refreshedUMod = Completable.fromAction(mUModsRepository::refreshUMods).andThen(mUModsRepository.getUMod(values.getUModUUID()));
        mUModsRepository.cachedFirst();
        return mUModsRepository.getUMod(values.getUModUUID()).switchIfEmpty(refreshedUMod);
        */
        //TODO why the upper approach didn't work on dooge????
        mUModsRepository.refreshUMods();
        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(uMod -> mAppUserRepository.getAppUser()
                        //.observeOn(schedulerProvider.io())
                        .flatMap(appUser -> {
                            CreateUserRPC.Arguments requestArguments =
                                    new CreateUserRPC.Arguments(appUser.getCredentialsString());
                            return mUModsRepository.createUModUser(uMod, requestArguments)
                                    .flatMap(createUserResult -> {
                                        //TODO ask if this is possible. Currently the API doc doesn't
                                        // specify what data is returned as part of the result.
                                        //subscribirme al topico
                                        uMod.setMqttResponseTopic(appUser.getUserName());
                                        //mUModMqttService.subscribeToUModTopics(uMod);
                                        Timber.d("CreateUser Success: " + createUserResult.toString());
                                        uMod.setAppUserLevel(createUserResult.getUserLevel());
                                        mUModsRepository.saveUMod(uMod);
                                        //Get Location could take a while. Perhaps a good alternative would be to launch a background use case...
                                        return Observable.just(createUserResult);
                                    })
                                    .onErrorResumeNext(throwable -> {
                                        if (throwable instanceof HttpException){
                                            //Check for HTTP UNAUTHORIZED error code
                                            String errorMessage = "";
                                            try {
                                                errorMessage = ((HttpException) throwable).response().errorBody().string();
                                            }catch (IOException exc){
                                                return Observable.error(exc);
                                            }
                                            int httpErrorCode = ((HttpException) throwable).response().code();

                                            Log.e("req_access_uc", "CreateUser (urbit:urbit) Failed on error CODE:"
                                                    + httpErrorCode
                                                    + " MESSAGE: "
                                                    + errorMessage);

                                            Timber.e("CreateUser (urbit:urbit) Failed on error CODE:"
                                                    + httpErrorCode
                                                    + " MESSAGE: "
                                                    + errorMessage);

                                            if (CreateUserRPC.DOC_ERROR_CODES.contains(httpErrorCode)){
                                                if (httpErrorCode == HttpURLConnection.HTTP_UNAUTHORIZED
                                                        || httpErrorCode ==  HttpURLConnection.HTTP_FORBIDDEN){
                                                    Log.e("req_access_uc", "CreateUser failed to Auth with urbit:urbit CODE: " + httpErrorCode);
                                                    Timber.e("CreateUser failed to Auth with urbit:urbit CODE: " + httpErrorCode);
                                                }
                                                if (httpErrorCode == HttpURLConnection.HTTP_INTERNAL_ERROR){
                                                    if(errorMessage.contains(Integer.toString(HttpURLConnection.HTTP_CONFLICT))){
                                                        //TODO evaluate the benefit of getting the user_type in the error body.
                                                        //That may save us the next request (Deserialization using JSONObject)
                                                        GetUserLevelRPC.Arguments getUserLevelArgs =
                                                                new GetUserLevelRPC.Arguments(appUser.getUserName());
                                                        return mUModsRepository.getUserLevel(uMod,getUserLevelArgs)
                                                                .flatMap( result -> {
                                                                    Log.d("getumod+info_uc","Get User Level Succeeded! " + result.toString());
                                                                    Timber.d("Get User Level Succeeded! " + result.toString());
                                                                    uMod.setAppUserLevel(result.getUserLevel());
                                                                    mUModsRepository.saveUMod(uMod);
                                                                    return Observable.just(new CreateUserRPC.Result(result.getAPIUserType()));
                                                                })
                                                                .onErrorResumeNext(throwable1 -> {
                                                                    Log.e("getumod+info_uc","Get User Level Failed: " + throwable1.getMessage());
                                                                    Timber.e("Get User Level Failed: " + throwable1.getMessage());
                                                                    if (throwable1 instanceof HttpException) {
                                                                        String errorMessage1 = "";
                                                                        try {
                                                                            errorMessage1 = ((HttpException) throwable1).response().errorBody().string();
                                                                        }catch (IOException exc){
                                                                            return Observable.error(exc);
                                                                        }
                                                                        int httpErrorCode1 = ((HttpException) throwable1).response().code();

                                                                        Log.e("req_access_uc", "GetUserStatus (urbit:urbit) Failed on error CODE:"
                                                                                + httpErrorCode1
                                                                                + " MESSAGE: "
                                                                                + errorMessage1);

                                                                        Timber.e("GetUserStatus (urbit:urbit) Failed on error CODE:"
                                                                                + httpErrorCode1
                                                                                + " MESSAGE: "
                                                                                + errorMessage1);

                                                                        if (GetUserLevelRPC.ALLOWED_ERROR_CODES.contains(httpErrorCode1)) {
                                                                            if (httpErrorCode1 == HttpURLConnection.HTTP_UNAUTHORIZED
                                                                                    || httpErrorCode1 ==  HttpURLConnection.HTTP_FORBIDDEN){
                                                                                Log.e("req_access_uc", "GetLevel failed to Auth with urbit:urbit CODE: " + httpErrorCode1);
                                                                                Timber.e( "GetLevel failed to Auth with urbit:urbit CODE: " + httpErrorCode1);
                                                                            }
                                                                            if ((httpErrorCode1 == HttpURLConnection.HTTP_INTERNAL_ERROR
                                                                                    && errorMessage1.contains(Integer.toString(HttpURLConnection.HTTP_NOT_FOUND)))) {
                                                                                Log.e("req_access_uc", "GetLevel failed User NOT FOUND.");
                                                                                Timber.e( "GetLevel failed User NOT FOUND.");
                                                                                uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                                                mUModsRepository.saveUMod(uMod);
                                                                                return Observable.error(throwable1);
                                                                            }
                                                                        }
                                                                    }
                                                                    return Observable.error(throwable1);
                                                                });
                                                    }
                                                    if(errorMessage.contains(Integer.toString(HttpURLConnection.HTTP_INTERNAL_ERROR))){
                                                        return Observable.error(new FailedToWriteUserFilesException(uMod.getUUID()));
                                                    }
                                                    if(errorMessage.contains(Integer.toString(HttpURLConnection.HTTP_PRECON_FAILED))){
                                                        return Observable.error(new MaxUModUsersQuantityReachedException(uMod.getUUID()));
                                                    }
                                                }

                                            }
                                        }
                                        //Not an API error but timeout or other sort of error.
                                        return Observable.error(throwable);
                                    });
                        }))
                //TODO review retry policy is it necessesary for this or this profound in the chain??
                //The RPC is executed in the saved umod connectionAddress
                //only when that address is outdated and produces an error a retry is performed that
                // forces a network lookup for the connected UMod.
                .retry((retryCount, throwable) -> {
                    Log.e("req_access_uc", "Retry count: " + retryCount + "\n Excep msge: " + throwable.getMessage());
                    if (retryCount == 1 //Two attempts maximum
                            && (throwable instanceof IOException
                            || throwable instanceof FailedToWriteUserFilesException)){
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

        public RequestValues(@NonNull String uModUUID) {
            this.uModUUID = checkNotNull(uModUUID, "uModUUID cannot be null!");
        }

        public String getUModUUID() {
            return uModUUID;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final CreateUserRPC.Result result;

        public ResponseValues(@NonNull CreateUserRPC.Result result) {
            this.result = checkNotNull(result, "result cannot be null!");
        }
        public CreateUserRPC.Result getResult() {
            return result;
        }
    }

    public static class MaxUModUsersQuantityReachedException extends Exception{
        private String mUModUUID;
        public MaxUModUsersQuantityReachedException(String uModUUID){
            super("Maximum UMod Users reached: " + uModUUID);
            this.mUModUUID = uModUUID;
        }
        public String getUModUUID(){
            return this.mUModUUID;
        }
    }

    public static class FailedToWriteUserFilesException extends Exception{
        private String mUModUUID;
        public FailedToWriteUserFilesException(String uModUUID){
            super("Failed to write user files in server: " + uModUUID);
            this.mUModUUID = uModUUID;
        }
        public String getUModUUID(){
            return this.mUModUUID;
        }
    }
}