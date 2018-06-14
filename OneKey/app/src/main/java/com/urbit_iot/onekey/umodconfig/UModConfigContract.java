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

package com.urbit_iot.onekey.umodconfig;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.BasePresenter;
import com.urbit_iot.onekey.BaseView;
import com.urbit_iot.onekey.data.UMod;

/**
 * This specifies the contract between the view and the presenter.
 */
public interface UModConfigContract {

    interface View extends BaseView<Presenter> {

        void showUModsList();

        void setUModUUID(String uModUUID);

        void setUModIPAddress(String ipAddress);

        boolean isActive();

        void showUModUsers(String uModUUID);

        String getWiFiAPSSID();

        void launchWiFiSettings();

        void showUModConfigs(UModConfigViewModel viewModel);

        void hideUpdateDialog();

        void showFirmwareUpdateSucessMsg();

        void showFirmwareUpdateFailMsg();

        void showResetFailMsg();

        void showResetSuccessMsg();

        void finishActivity();

        void showUpgradeCancellationMsg();

        void showProgressBar();

        void hideProgressBar();

        void hideCompletely();

        void showOngoingNotificationStatusChangeSuccess(Boolean notificationEnabled);

        void showOngoingNotificationStatusChangeFail(Boolean notificationEnabled);

        void refreshOngoingNotification();

        void showAliasConfigFailMsg();

        void showAliasConfigSuccessMsg();

        void showWiFiCredentialsConfigFailMsg();

        void showWiFiCredentialsConfigSuccessMsg();
    }

    interface Presenter extends BasePresenter {

        void populateUModSettings();

        void adminUModUsers();

        void updateUModAlias(UModConfigViewModel viewModel);

        void updateUModWiFiCredentials(UModConfigViewModel viewModel);

        void updateUModFirmware();

        void factoryResetUMod();

        void cancelFirmwareUpgrade();

        void setNotificationStatus(@NonNull String uModUUID, final Boolean notificationEnabled);
    }
}