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
import com.urbit_iot.onekey.data.commands.ApproveUserCmd;
import com.urbit_iot.onekey.data.commands.DeleteUserCmd;
import com.urbit_iot.onekey.data.commands.OpenCloseCmd;

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

    //Observable<Command.CommandResponse> postCommand(@NonNull Command.CommandRequest commandRequest);

    Observable<OpenCloseCmd.Response> openCloseUMod(@NonNull UMod uMod, @NonNull OpenCloseCmd.CommandRequest commandRequest);

    Observable<ApproveUserCmd.Response> approveUModUser(@NonNull UModUser uModUser);

    Observable<DeleteUserCmd.Response> deleteUModUser(@NonNull UModUser uModUser);

    Observable<List<UModUser>> getUModUsers(@NonNull String uModUUID);
}
