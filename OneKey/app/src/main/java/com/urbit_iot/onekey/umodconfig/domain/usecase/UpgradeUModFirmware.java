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
import com.urbit_iot.onekey.data.rpc.OTACommitRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Retrieves a {@link UMod} from the {@link UModsRepository}.
 */
public class UpgradeUModFirmware extends SimpleUseCase<UpgradeUModFirmware.RequestValues, UpgradeUModFirmware.ResponseValues> {

    private final UModsRepository uModsRepository;

    @Inject
    public UpgradeUModFirmware(@NonNull UModsRepository tasksRepository,
                               @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        uModsRepository = tasksRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(RequestValues values) {
        final SysGetInfoRPC.Request infoRequest =
                new SysGetInfoRPC.Request(new SysGetInfoRPC.Arguments(),"Sys.GetInfo",234234145);
        final OTACommitRPC.Request otaCommitRequest =
                new OTACommitRPC.Request(null,"OTA.Update",666);

        uModsRepository.refreshUMods();

        return uModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<Response<ResponseBody>>>() {
                    @Override
                    public Observable<Response<ResponseBody>> call(final UMod uMod) {
                        Log.d("OTA_UC","Downloading File...to update " + uMod.toString());
                        //return uModsRepository.getSystemInfo(uMod,request);
                        return uModsRepository.getFirmwareImageFile(uMod)
                                .flatMap(new Func1<File, Observable<Response<ResponseBody>>>() {
                                    @Override
                                    public Observable<Response<ResponseBody>> call(final File file) {
                                        Log.d("OTA_UC","Posting file: " + file.getName() + " size: "+file.length());
                                        return uModsRepository.postFirmwareUpdateToUMod(uMod, file)
                                                .delay(12L, TimeUnit.SECONDS)
                                                .flatMap(new Func1<Response<ResponseBody>, Observable<Response<ResponseBody>>>() {
                                                    @Override
                                                    public Observable<Response<ResponseBody>> call(Response<ResponseBody> responseBodyResponse) {
                                                        file.delete();
                                                        if (!responseBodyResponse.isSuccessful()){
                                                            Log.d("OTA_UC","Update Post Failed!!");
                                                            return Observable.error(new Exception("postFirmwareUpdate errored."));
                                                        } else {
                                                            Log.d("OTA_UC","Verifying umod version");
                                                            return uModsRepository.getSystemInfo(uMod, infoRequest)
                                                                    .flatMap(new Func1<SysGetInfoRPC.Response, Observable<Response<ResponseBody>>>() {
                                                                        @Override
                                                                        public Observable<Response<ResponseBody>> call(SysGetInfoRPC.Response response) {
                                                                            Log.d("OTA_UC","The current version is: " + response.getResponseResult().getFwVersion());
                                                                            //TODO implement method for comparing versions as v1.5.4 > v1.3.6. Question: is always the upgrade forward??
                                                                            if(!uMod.getSWVersion().contentEquals(response.getResponseResult().getFwVersion())){
                                                                                uMod.setSWVersion(response.getResponseResult().getFwVersion());
                                                                                Log.d("OTA_UC","Update was Successful. Preparing commit.");
                                                                                return uModsRepository.otaCommit(uMod, otaCommitRequest);
                                                                            } else {
                                                                                Log.d("OTA_UC","Versions missmatch. Upgrade FAILED.");
                                                                                return Observable.error(new Exception("Incorrect firmware version after update."));
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                    }
                                });
                    }
                })
                .flatMap(new Func1<Response<ResponseBody>, Observable<ResponseValues>>() {
                    @Override
                    public Observable<ResponseValues> call(Response<ResponseBody> otaCommitResponse) {

                        if (otaCommitResponse.isSuccessful()){
                            Log.d("OTA_UC","Update commit successful");
                            return Observable.just(new ResponseValues(otaCommitResponse.code()));
                        } else {
                            Log.d("OTA_UC","Update commit FAILED");
                            return Observable.error(new Exception("Unseccessful OTA commit."));
                        }
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

        private int responseCode;

        public ResponseValues(int responseCode) {
            this.responseCode = responseCode;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }
    }
}
