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

package com.urbit_iot.onekey.data.source.internet;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.GetUsersRPC;
import com.urbit_iot.onekey.data.rpc.OTACommitRPC;
import com.urbit_iot.onekey.data.rpc.SetWiFiAPRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsDataSource;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;

/**
 * Implementation of the data source that adds a latency simulating network.
 */
public class UModsInternetDataSource implements UModsDataSource {

    private FirmwareFileDownloader mFirmwareFileDownloader;

    @Inject
    public UModsInternetDataSource(FirmwareFileDownloader firmwareDownloader) {
        this.mFirmwareFileDownloader = firmwareDownloader;
    }

    @Override
    public Observable<List<UMod>> getUMods() {
        return null;
    }

    @Override
    public Observable<UMod> getUModsOneByOne() {
        return null;
    }

    @Override
    public Observable<UMod> getUMod(@NonNull String uModUUID) {
        return null;
    }

    @Override
    public void saveUMod(@NonNull UMod uMod) {

    }

    @Override
    public Observable<UMod> updateUModAlias(@NonNull String uModUUID, @NonNull String newAlias) {
        return null;
    }

    @Override
    public void partialUpdate(@NonNull UMod uMod) {

    }

    @Override
    public void setUModNotificationStatus(@NonNull String uModUUID, @NonNull Boolean notificationEnabled) {

    }

    @Override
    public void clearAlienUMods() {

    }

    @Override
    public void refreshUMods() {

    }

    @Override
    public void deleteAllUMods() {

    }

    @Override
    public void deleteUMod(@NonNull String uModUUID) {

    }

    @Override
    public Observable<GetMyUserLevelRPC.Result> getUserLevel(@NonNull UMod uMod, @NonNull GetMyUserLevelRPC.Arguments requestArguments) {
        return null;
    }

    @Override
    public Observable<TriggerRPC.Result> triggerUMod(@NonNull UMod uMod, @NonNull TriggerRPC.Arguments requestArguments) {
        return null;
    }

    @Override
    public Observable<CreateUserRPC.Result> createUModUser(@NonNull UMod uMod, @NonNull CreateUserRPC.Arguments requestArguments) {
        return null;
    }

    @Override
    public Observable<UpdateUserRPC.Result> updateUModUser(@NonNull UMod uMod, @NonNull UpdateUserRPC.Arguments requestArguments) {
        return null;
    }

    @Override
    public Observable<DeleteUserRPC.Result> deleteUModUser(@NonNull UMod uMod, @NonNull DeleteUserRPC.Arguments requestArguments) {
        return null;
    }

    @Override
    public Observable<GetUsersRPC.Result> getUModUsers(@NonNull UMod uMod, @NonNull GetUsersRPC.Arguments requestArgs) {
        return null;
    }


    @Override
    public Observable<SysGetInfoRPC.Result> getSystemInfo(@NonNull UMod uMod, @NonNull SysGetInfoRPC.Arguments request) {
        return null;
    }

    @Override
    public Observable<SetWiFiAPRPC.Result> setWiFiAP(UMod uMod, SetWiFiAPRPC.Arguments request) {
        return null;
    }

    @Override
    public Observable<Response<ResponseBody>> otaCommit(UMod uMod, OTACommitRPC.Arguments request) {
        return null;
    }

    @Override
    public Observable<FactoryResetRPC.Result> factoryResetUMod(UMod uMod, FactoryResetRPC.Arguments request) {
        return null;
    }

    @Override
    public Observable<File> getFirmwareImageFile(UMod uMod) {
        return this.mFirmwareFileDownloader.downloadFirmwareFile();
    }

    @Override
    public Observable<Response<ResponseBody>> postFirmwareUpdateToUMod(UMod uMod, File newFirmwareFile) {
        return null;
    }


}