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

package com.urbit_iot.porton.umodconfig;

import androidx.annotation.NonNull;
import android.util.Pair;

import com.urbit_iot.porton.BasePresenter;
import com.urbit_iot.porton.BaseView;

import java.util.List;

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

        void updateLocationText(String locationAddress);

        void showLocationLoadingProgressBar();

        void hideLocationLoadingProgressBar();

        void showLocationUpdateSuccessMsg();

        void showLocationUpdateFailureMsg();

        void showSettingsApplySuccessMsg();

        void showCalibrationResetSuccessMsg();

        void showCalibrationResetFailMsg();

        void loadWifiSsidSpinnerData(List<Pair<String, UModConfigFragment.SignalStrength>> ssidList);
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

        void getPhoneLocation();

        void updateUModLocationData();

        void updateSettings(String aliasText, String wifiSSID, String wifiPassword, boolean mUpdateLocation);

        void resetCalibration();
    }
}