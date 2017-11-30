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
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;

import java.util.List;

import rx.Observable;

/**
 * Main entry point for accessing tasks data.
 * <p>
 */
public interface UModsDataSource {

    Observable<List<UMod>> getUMods();

    Observable<UMod> getUModsOneByOne();

    Observable<UMod> getUMod(@NonNull String uModUUID);

    void saveUMod(@NonNull UMod uMod);

    void enableUModNotification(@NonNull UMod uMod);

    void enableUModNotification(@NonNull String uModUUID);

    void disableUModNotification(@NonNull UMod uMod);

    void disableUModNotification(@NonNull String uModUUID);

    void clearAlienUMods();

    void refreshUMods();

    void deleteAllUMods();

    void deleteUMod(@NonNull String uModUUID);

    //Observable<RPC.CommandResponse> postCommand(@NonNull RPC.CommandRequest commandRequest);

    Observable<GetMyUserLevelRPC.SuccessResponse> getUserLevel(@NonNull UMod uMod,
                                                               @NonNull GetMyUserLevelRPC.Request request);

    Observable<TriggerRPC.SuccessResponse> triggerUMod(@NonNull UMod uMod,
                                                       @NonNull TriggerRPC.Request request);

    Observable<UpdateUserRPC.SuccessResponse> updateUModUser(@NonNull UMod uMod,
                                                             @NonNull UpdateUserRPC.Request request);

    Observable<DeleteUserRPC.SuccessResponse> deleteUModUser(@NonNull UMod uMod,
                                                             @NonNull DeleteUserRPC.Request request);

    //Observable<UpdateUserRPC.Response> approveUModUser(@NonNull UModUser uModUser);

    //Observable<DeleteUserRPC.Response> deleteUModUser(@NonNull UModUser uModUser);

    Observable<List<UModUser>> getUModUsers(@NonNull String uModUUID);
}
