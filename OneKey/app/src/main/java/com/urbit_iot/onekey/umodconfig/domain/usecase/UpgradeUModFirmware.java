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
import com.urbit_iot.onekey.data.rpc.EnableUpdateRPC;
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
        /*
        final SysGetInfoRPC.Request infoRequest =
                new SysGetInfoRPC.Request(new SysGetInfoRPC.Arguments(),"Sys.GetInfo",234234145);
        final OTACommitRPC.Request otaCommitRequest =
                new OTACommitRPC.Request(null,"OTA.Update",666);
         */

        uModsRepository.refreshUMods();

        return uModsRepository.getUMod(values.getUModUUID())
                .flatMap(uMod -> {
                    Log.d("OTA_UC","Downloading File...to update " + uMod.toString());
                    //return uModsRepository.getSystemInfo(uMod,request);
                    return uModsRepository.getFirmwareImageFile(uMod)
                            .flatMap(file -> {
                                if (Strings.isNullOrEmpty(uMod.getSWVersion())){
                                    return Observable.error(new UModSwVersionUnavailable(uMod.getUUID()));
                                }
                                if(file.getName().contains(uMod.getSWVersion())){
                                    return Observable.error(new UpgradeIsntNeccessary(uMod.getSWVersion()));
                                } else {
                                    Log.d("OTA_UC","Posting file: " + file.getName() + " size: "+file.length());
                                    return uModsRepository.enableUModUpdate(uMod, new EnableUpdateRPC.Arguments())
                                            .flatMap(enablingResult -> uModsRepository.postFirmwareUpdateToUMod(uMod, file)
                                                    .flatMap(responseBodyResponse -> {
                                                        if (file.delete()){
                                                            Log.d("OTA_UC","File deleted successfully.");
                                                        } else {
                                                            Log.e("OTA_UC","Failed to delete file.");
                                                        }
                                                        Log.d("OTA_UC","Update Post Response: \n" +
                                                                responseBodyResponse.toString());
                                                        if (!responseBodyResponse.isSuccessful()){
                                                            Log.d("OTA_UC","Update Post Failed!!");
                                                            return Observable.error(new Exception("postFirmwareUpdate failed."));
                                                        } else {
                                                            return Observable.timer(25L,TimeUnit.SECONDS)
                                                                    .flatMap(aLong -> {
                                                                        Log.d("OTA_UC","Verifying umod version");
                                                                        SysGetInfoRPC.Arguments sysGetInfoArgs = new SysGetInfoRPC.Arguments();
                                                                        return uModsRepository.getSystemInfo(uMod, sysGetInfoArgs)
                                                                                .flatMap(result -> {
                                                                                    Log.d("OTA_UC","The current version is: " + result.getFwVersion());
                                                                                    //TODO implement method for comparing versions as v1.5.4 > v1.3.6. Question: is always the upgrade forward??
                                                                                    if(!uMod.getSWVersion().contentEquals(result.getFwVersion())){
                                                                                        uMod.setSWVersion(result.getFwVersion());
                                                                                        Log.d("OTA_UC","Update was Successful. Preparing commit.");
                                                                                        OTACommitRPC.Arguments otaCommitArgs = new OTACommitRPC.Arguments();
                                                                                        return uModsRepository.otaCommit(uMod, otaCommitArgs);
                                                                                    } else {
                                                                                        Log.d("OTA_UC","Versions mismatch. Upgrade FAILED.");
                                                                                        return Observable.error(new Exception("Incorrect firmware version after update."));
                                                                                    }
                                                                                });
                                                                    });
                                                        }
                                                    }));
                                }
                            });
                })
                .flatMap(otaCommitResponse -> {
                    if (otaCommitResponse.isSuccessful()){
                        Log.d("OTA_UC","Update commit successful "+
                                otaCommitResponse.toString());
                        return Observable.just(new ResponseValues(otaCommitResponse.code()));
                    } else {
                        Log.d("OTA_UC","Update commit FAILED " + otaCommitResponse.toString());
                        return Observable.error(new Exception("Unsuccessful OTA commit."));
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

    public static class UpgradeIsntNeccessary extends Exception{
        private String uModSoftwareVersion;
        public UpgradeIsntNeccessary(String uModSoftwareVersion){
            super("Firmware upgrade isn't necessary. Current Version: " + uModSoftwareVersion);
            this.uModSoftwareVersion = uModSoftwareVersion;
        }
        public String getUModSoftwareVersion(){
            return this.uModSoftwareVersion;
        }
    }

    public static class UModSwVersionUnavailable extends Exception{
        private String uModUUID;
        public UModSwVersionUnavailable(String uModUUID){
            super("Firmware version is unknown for UMod: " + uModUUID);
            this.uModUUID = uModUUID;
        }
        public String getUModUUID(){
            return this.uModUUID;
        }
    }
}
