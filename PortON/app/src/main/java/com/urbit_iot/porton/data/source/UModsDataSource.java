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

package com.urbit_iot.porton.data.source;

import android.location.Address;
import android.location.Location;
import androidx.annotation.NonNull;

import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.rpc.AdminCreateUserRPC;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.DeleteUserRPC;
import com.urbit_iot.porton.data.rpc.EnableUpdateRPC;
import com.urbit_iot.porton.data.rpc.FactoryResetRPC;
import com.urbit_iot.porton.data.rpc.GetGateStatusRPC;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.GetUsersRPC;
import com.urbit_iot.porton.data.rpc.OTACommitRPC;
import com.urbit_iot.porton.data.rpc.SetGateStatusRPC;
import com.urbit_iot.porton.data.rpc.SetWiFiRPC;
import com.urbit_iot.porton.data.rpc.SysGetInfoRPC;
import com.urbit_iot.porton.data.rpc.TriggerRPC;
import com.urbit_iot.porton.data.rpc.UpdateUserRPC;

import java.io.File;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;

/**
 * Main entry point for accessing tasks data.
 * <p>
 */
public interface UModsDataSource {

    Observable<List<UMod>> getUMods();// GET /umods/

    Observable<UMod> getUModsOneByOne();// GET /umods/

    Observable<UMod> getUMod(@NonNull String uModUUID); // GET /umods/UMOD_ID

    void saveUMod(@NonNull UMod uMod);// POST /umods/

    Observable<UMod> updateUModAlias(@NonNull String uModUUID, @NonNull String newAlias);

    //TODO PartialUpdate

    void partialUpdate(@NonNull UMod uMod);// PATCH /umods/?? vs /umods/UMOD_ID

    void setUModNotificationStatus(@NonNull String uModUUID, @NonNull Boolean notificationEnabled);
    // PUT??PATCH /umods/UMOD_ID?notification_status=true

    void clearAlienUMods();

    void refreshUMods();

    void deleteAllUMods();// DELETE /umods/

    void deleteUMod(@NonNull String uModUUID);// DELETE /umods/UMOD_ID

    //Observable<RPC.CommandResponse> postCommand(@NonNull RPC.CommandRequest commandRequest);

    Observable<GetUserLevelRPC.Result>
    getUserLevel(@NonNull UMod uMod,
                 @NonNull GetUserLevelRPC.Arguments requestArguments);
    //GET /rpc/ && /umods/UMOD_ID/rpc?token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    Observable<TriggerRPC.Result> triggerUMod(@NonNull UMod uMod,
                                                @NonNull TriggerRPC.Arguments requestArguments);
    //POST /rpc/ && /umods/UMOD_ID/rpc?token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    Observable<CreateUserRPC.Result> createUModUser(@NonNull UMod uMod,
                                                      @NonNull CreateUserRPC.Arguments requestArguments);
    //PUT /rpc/ && /umods/UMOD_ID/rpc?token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    Observable<UpdateUserRPC.Result> updateUModUser(@NonNull UMod uMod,
                                                      @NonNull UpdateUserRPC.Arguments requestArguments);
    //PUT /rpc/ && /umods/UMOD_ID/rpc?token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    Observable<DeleteUserRPC.Result> deleteUModUser(@NonNull UMod uMod,
                                                      @NonNull DeleteUserRPC.Arguments requestArguments);
    //DELETE /rpc/ && /umods/UMOD_ID/rpc?token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    //Observable<UpdateUserRPC.Response> approveUModUser(@NonNull UModUser uModUser);

    //Observable<DeleteUserRPC.Response> deleteUModUser(@NonNull UModUser uModUser);

    Observable<GetUsersRPC.Result> getUModUsers(@NonNull UMod uMod,
                                                @NonNull GetUsersRPC.Arguments requestArgs);
    //GET /rpc/ && /umods/UMOD_ID/rpc/ vs /umods/UMOD_ID/users

    Observable<SysGetInfoRPC.Result> getSystemInfo(@NonNull UMod uMod,
                                                     @NonNull SysGetInfoRPC.Arguments request);
    // GET /rpc/ && /umods/UMOD_ID/rpc/ vs /umods/UMOD_ID/info

    Observable<SetWiFiRPC.Result> setWiFiAP(UMod uMod, SetWiFiRPC.Arguments request);

    Observable<File> getFirmwareImageFile(UMod uMod);

    Observable<Response<ResponseBody>> postFirmwareUpdateToUMod(UMod uMod, File newFirmwareFile);

    Observable<Response<ResponseBody>> otaCommit(UMod uMod, OTACommitRPC.Arguments request);

    Observable<FactoryResetRPC.Result> factoryResetUMod(UMod uMod,
                                                          FactoryResetRPC.Arguments request);
    default Observable<Location> getCurrentLocation(){ return Observable.empty();}

    default Observable<Address> getAddressFromLocation(Location location){return Observable.empty();}

    default Observable<AdminCreateUserRPC.Result>
    createUModUserByName(UMod uMod, AdminCreateUserRPC.Arguments createUserArgs){
        return Observable.empty();}

    default Observable<EnableUpdateRPC.Result>
    enableUModUpdate(UMod uMod, EnableUpdateRPC.Arguments arguments){
        return Observable.empty();
    }

    default Observable<SetGateStatusRPC.Result>
    setUModGateStatus(UMod uMod, SetGateStatusRPC.Arguments reqArguments){
        return Observable.empty();
    }

    default Observable<GetGateStatusRPC.Result>
    getUModGateStatus(UMod uMod, GetGateStatusRPC.Arguments reqArguments){
        return Observable.empty();
    }

    default Observable<GetGateStatusRPC.Response>
    getUModGateStatusUpdates(){
        return Observable.empty();
    }
}
