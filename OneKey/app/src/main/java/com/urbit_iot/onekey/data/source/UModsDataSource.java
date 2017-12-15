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

package com.urbit_iot.onekey.data.source;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;

import java.util.List;

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

    //TODO PartialUpdate

    void partialUpdate(@NonNull UMod uMod);// PATCH /umods/?? vs /umods/UMOD_ID

    void setUModNotificationStatus(@NonNull String uModUUID, @NonNull Boolean notificationEnabled);
    // PUT??PATCH /umods/UMOD_ID?notification_status=true

    void clearAlienUMods();

    void refreshUMods();

    void deleteAllUMods();// DELETE /umods/

    void deleteUMod(@NonNull String uModUUID);// DELETE /umods/UMOD_ID

    //Observable<RPC.CommandResponse> postCommand(@NonNull RPC.CommandRequest commandRequest);

    Observable<GetMyUserLevelRPC.Response>
    getUserLevel(@NonNull UMod uMod,
                 @NonNull GetMyUserLevelRPC.Request request);
    //GET /rpc/ && /umods/UMOD_ID/rpc?token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    Observable<TriggerRPC.Response> triggerUMod(@NonNull UMod uMod,
                                                @NonNull TriggerRPC.Request request);
    //POST /rpc/ && /umods/UMOD_ID/rpc?token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    Observable<CreateUserRPC.Response> createUModUser(@NonNull UMod uMod,
                                                      @NonNull CreateUserRPC.Request request);
    //PUT /rpc/ && /umods/UMOD_ID/rpc?token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    Observable<UpdateUserRPC.Response> updateUModUser(@NonNull UMod uMod,
                                                      @NonNull UpdateUserRPC.Request request);
    //PUT /rpc/ && /umods/UMOD_ID/rpc?token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    Observable<DeleteUserRPC.Response> deleteUModUser(@NonNull UMod uMod,
                                                      @NonNull DeleteUserRPC.Request request);
    //DELETE /rpc/ && /umods/UMOD_ID/rpc?token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    //Observable<UpdateUserRPC.Response> approveUModUser(@NonNull UModUser uModUser);

    //Observable<DeleteUserRPC.Response> deleteUModUser(@NonNull UModUser uModUser);

    Observable<List<UModUser>> getUModUsers(@NonNull UMod uMod);
    //GET /rpc/ && /umods/UMOD_ID/rpc/ vs /umods/UMOD_ID/users

    Observable<SysGetInfoRPC.Response> getSystemInfo(@NonNull UMod uMod,
                                                     @NonNull SysGetInfoRPC.Request request);
    // GET /rpc/ && /umods/UMOD_ID/rpc/ vs /umods/UMOD_ID/info
}
