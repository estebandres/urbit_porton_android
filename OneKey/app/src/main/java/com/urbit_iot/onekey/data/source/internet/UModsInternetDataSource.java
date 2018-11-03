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
import com.urbit_iot.onekey.data.rpc.AdminCreateUserRPC;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.rpc.GetUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.GetUsersRPC;
import com.urbit_iot.onekey.data.rpc.OTACommitRPC;
import com.urbit_iot.onekey.data.rpc.SetWiFiRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsDataSource;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Completable;
import rx.Observable;

/**
 * Implementation of the data source that adds a latency simulating network.
 */
public class UModsInternetDataSource implements UModsDataSource {

    @NonNull
    private FirmwareFileDownloader mFirmwareFileDownloader;
    @NonNull
    private UModMqttServiceContract mUModMqttService;
    @NonNull
    private String username;

    private Random randomGenerator;

    @Inject
    public UModsInternetDataSource(@NonNull FirmwareFileDownloader firmwareDownloader,
                                   @NonNull UModMqttServiceContract mUModMqttService,
                                   @NonNull String username) {
        this.mFirmwareFileDownloader = firmwareDownloader;
        this.mUModMqttService = mUModMqttService;
        this.username = username;
        randomGenerator = new Random();
    }

    @Override
    public Observable<List<UMod>> getUMods() {
        return null;
    }

    @Override
    public Observable<UMod> getUModsOneByOne() {
        return this.mUModMqttService.scanUModInvitations();
    }

    @Override
    public Observable<UMod> getUMod(@NonNull String uModUUID) {
        return null;
    }

    @Override
    public void saveUMod(@NonNull UMod uMod) {
        //&& !uMod.isInAPMode()
        if (uMod.belongsToAppUser()){// TODO in case the user was deleted after creation then requestAccess is possible on MQTT
            mUModMqttService.subscribeToUModResponseTopic(uMod);//Warning hazard of network on main
        }
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
        mUModMqttService.clearAllSubscriptions();
    }

    @Override
    public void deleteUMod(@NonNull String uModUUID) {

    }

    @Override
    public Observable<GetUserLevelRPC.Result> getUserLevel(@NonNull UMod uMod, @NonNull GetUserLevelRPC.Arguments requestArguments) {
        GetUserLevelRPC.Request userLevelRequest = new GetUserLevelRPC.Request(requestArguments,this.username,uMod.getUUID(),this.randomGenerator.nextInt());

        return mUModMqttService.publishRPC(uMod,userLevelRequest,GetUserLevelRPC.Response.class)
                .map(GetUserLevelRPC.Response::getResponseResult);
    }

    @Override
    public Observable<TriggerRPC.Result> triggerUMod(@NonNull UMod uMod, @NonNull TriggerRPC.Arguments requestArguments) {
        TriggerRPC.Request triggerRequest = new TriggerRPC.Request(
                this.username,
                uMod.getAppUserLevel() == UModUser.Level.ADMINISTRATOR,
                requestArguments,
                uMod.getUUID(),
                this.randomGenerator.nextInt());

        return mUModMqttService.publishRPC(uMod,
                triggerRequest,
                TriggerRPC.Response.class)
                .map(TriggerRPC.Response::getResponseResult);
    }

    /*
    public Completable responseSubscriptionChecker(String uModUUID){
        //Observable.fromCallable(() -> mUModMqttService.getListOfSubscribedUModsUUIDs().contains())
        return Completable.fromCallable(() -> {
            if (mUModMqttService.getListOfSubscribedUModsUUIDs().contains(uModUUID)){
                return true;
            }
            throw new Exception("RESPONSE SUBS MISSING");
        });
    }
    */

    @Override
    public Observable<CreateUserRPC.Result> createUModUser(@NonNull UMod uMod, @NonNull CreateUserRPC.Arguments requestArguments) {
        CreateUserRPC.Request createUserRequest = new CreateUserRPC.Request(
                requestArguments,
                this.username,
                uMod.getUUID(),
                this.randomGenerator.nextInt());

        return mUModMqttService.publishRPC(
                        uMod,
                        createUserRequest,
                        CreateUserRPC.Response.class)
                        .map(CreateUserRPC.Response::getResponseResult);
    }

    @Override
    public Observable<UpdateUserRPC.Result> updateUModUser(@NonNull UMod uMod, @NonNull UpdateUserRPC.Arguments requestArguments) {
        UpdateUserRPC.Request updateUserRequest = new UpdateUserRPC.Request(requestArguments, this.username, uMod.getUUID(), this.randomGenerator.nextInt());

        return mUModMqttService.publishRPC(uMod,updateUserRequest,UpdateUserRPC.Response.class)
                .map(UpdateUserRPC.Response::getResponseResult);
    }

    @Override
    public Observable<DeleteUserRPC.Result> deleteUModUser(@NonNull UMod uMod, @NonNull DeleteUserRPC.Arguments requestArguments) {
        DeleteUserRPC.Request deleteRequest = new DeleteUserRPC.Request(
                requestArguments,
                this.username,
                uMod.getUUID(),
                this.randomGenerator.nextInt());
        return mUModMqttService.publishRPC(uMod,deleteRequest,DeleteUserRPC.Response.class)
                .map(DeleteUserRPC.Response::getResponseResult);
    }

    @Override
    public Observable<GetUsersRPC.Result> getUModUsers(@NonNull UMod uMod, @NonNull GetUsersRPC.Arguments requestArgs) {
        GetUsersRPC.Request getUsersRequest = new GetUsersRPC.Request(requestArgs,this.username,uMod.getUUID(),this.randomGenerator.nextInt());

        return mUModMqttService.publishRPC(uMod,getUsersRequest,GetUsersRPC.Response.class)
                .map(GetUsersRPC.Response::getResponseResult);
    }

    @Override
    public Observable<SysGetInfoRPC.Result> getSystemInfo(@NonNull UMod uMod, @NonNull SysGetInfoRPC.Arguments request) {
        SysGetInfoRPC.Request rpcRequest = new SysGetInfoRPC.Request(request,this.username,uMod.getUUID(),this.randomGenerator.nextInt());
        return mUModMqttService.publishRPC(uMod,rpcRequest, SysGetInfoRPC.Response.class)
                .map(SysGetInfoRPC.Response::getResponseResult);


        //return Observable.error(new Exception("Steve was here!"));
    }

    @Override
    public Observable<SetWiFiRPC.Result> setWiFiAP(UMod uMod, SetWiFiRPC.Arguments request) {
        return null;
    }

    @Override
    public Observable<Response<ResponseBody>> otaCommit(UMod uMod, OTACommitRPC.Arguments request) {
        return null;
    }

    @Override
    public Observable<FactoryResetRPC.Result> factoryResetUMod(UMod uMod, FactoryResetRPC.Arguments request) {
        FactoryResetRPC.Request factoryResetRequest = new FactoryResetRPC.Request(request, this.username, uMod.getUUID(), this.randomGenerator.nextInt());
        return mUModMqttService.publishRPC(uMod,factoryResetRequest,FactoryResetRPC.Response.class)
                .map(FactoryResetRPC.Response::getResponseResult);
    }

    @Override
    public Observable<File> getFirmwareImageFile(UMod uMod) {
        return this.mFirmwareFileDownloader.downloadFirmwareFile();
    }

    @Override
    public Observable<Response<ResponseBody>> postFirmwareUpdateToUMod(UMod uMod, File newFirmwareFile) {
        return null;
    }

    @Override
    public Observable<AdminCreateUserRPC.Result>
    createUModUserByName(UMod uMod, AdminCreateUserRPC.Arguments createUserArgs) {
        AdminCreateUserRPC.Request adminRequest = new AdminCreateUserRPC.Request(createUserArgs,
                this.username,
                uMod.getUUID(),
                this.randomGenerator.nextInt());
        return mUModMqttService.publishRPC(uMod,
                adminRequest,
                AdminCreateUserRPC.Response.class)
                .map(AdminCreateUserRPC.Response::getResponseResult);
    }
}