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
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Strings;
import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.umodconfig.domain.usecase.FactoryResetUMod;
import com.urbit_iot.onekey.umodconfig.domain.usecase.GetUModAndUpdateInfo;
import com.urbit_iot.onekey.umodconfig.domain.usecase.GetUModSystemInfo;
import com.urbit_iot.onekey.umodconfig.domain.usecase.SaveUMod;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.umodconfig.domain.usecase.UpdateUModAlias;
import com.urbit_iot.onekey.umodconfig.domain.usecase.UpdateWiFiCredentials;
import com.urbit_iot.onekey.umodconfig.domain.usecase.UpgradeUModFirmware;
import com.urbit_iot.onekey.umodconfig.domain.usecase.SetOngoingNotificationStatus;
import com.urbit_iot.onekey.util.IntegerContainer;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;


import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link UModConfigFragment}), retrieves the data and
 * updates
 * the UI as required.
 */
public class UModConfigPresenter implements UModConfigContract.Presenter {

    @NonNull
    private final UModConfigContract.View mUModConfigView;

    @NonNull
    private final GetUModAndUpdateInfo mGetUModAndUpdateInfo;

    @NonNull
    private final SaveUMod mSaveUMod;

    @NonNull
    private final UpdateUModAlias mUpdateUModAlias;

    @NonNull
    private final UpdateWiFiCredentials mUpdateWiFiCredentials;

    @NonNull
    private final GetUModSystemInfo mGetUModSystemInfo;

    @NonNull
    private final FactoryResetUMod mFactoryReset;

    @NonNull
    private final UpgradeUModFirmware mUpgradeUModFirmware;

    @NonNull
    private final SetOngoingNotificationStatus mSetOngoingNotificationStatus;

    @Nullable
    private UMod uModToConfig;

    @NonNull
    private String mUModUUID;


    /**
     * Dagger strictly enforces that arguments not marked with {@code @Nullable} are not injected
     * with {@code @Nullable} values.
     */
    @Inject
    public UModConfigPresenter(@NonNull String umodUUID,
                               @NonNull UModConfigContract.View addTaskView,
                               @NonNull GetUModAndUpdateInfo getUModAndUpdateInfo,
                               @NonNull UpdateUModAlias updateUModAlias,
                               @NonNull SaveUMod saveUMod,
                               @NonNull UpdateWiFiCredentials mUpdateWiFiCredentials,
                               @NonNull GetUModSystemInfo mGetUModSystemInfo,
                               @NonNull FactoryResetUMod mFactoryReset,
                               @NonNull UpgradeUModFirmware mUpgradeUModFirmware,
                               @NonNull SetOngoingNotificationStatus mSetOngoingNotificationStatus) {
        this.mUModUUID = umodUUID;
        this.mUModConfigView = addTaskView;
        this.mGetUModAndUpdateInfo = getUModAndUpdateInfo;
        this.mUpdateUModAlias = updateUModAlias;
        this.mSaveUMod = saveUMod;
        this.mUpdateWiFiCredentials = mUpdateWiFiCredentials;
        this.mGetUModSystemInfo = mGetUModSystemInfo;
        this.mFactoryReset = mFactoryReset;
        this.mUpgradeUModFirmware = mUpgradeUModFirmware;
        this.mSetOngoingNotificationStatus = mSetOngoingNotificationStatus;
    }

    /**
     * Method injection is used here to safely reference {@code this} after the object is created.
     * For more information, see Java Concurrency in Practice.
     */
    @Inject
    void setupListeners() {
        mUModConfigView.setPresenter(this);
    }

    @Override
    public void subscribe() {
        if (mUModUUID != null) {
            populateUModSettings();
        }
    }

    @Override
    public void unsubscribe() {
        mFactoryReset.unsubscribe();
        mUpgradeUModFirmware.unsubscribe();
        mUpdateUModAlias.unsubscribe();
        mUpdateWiFiCredentials.unsubscribe();
        mGetUModAndUpdateInfo.unsubscribe();
        mSaveUMod.unsubscribe();
        this.mGetUModSystemInfo.unsubscribe();
        mSetOngoingNotificationStatus.unsubscribe();
    }

    @Override
    public void populateUModSettings() {
        mUModConfigView.hideCompletely();
        if (Strings.isNullOrEmpty(mUModUUID)) {
            throw new RuntimeException("populateUModSettings() was called but umod is null.");
        }
        mUModConfigView.showProgressBar();
        final IntegerContainer onNextCount = new IntegerContainer(0);
        String currentWiFiAPSSID = mUModConfigView.getWiFiAPSSID();
        mGetUModAndUpdateInfo.execute(
                new GetUModAndUpdateInfo.RequestValues(mUModUUID, currentWiFiAPSSID),
                new Subscriber<GetUModAndUpdateInfo.ResponseValues>() {
            @Override
            public void onCompleted() {
                if (onNextCount.getValue() <= 0){
                    Log.e("conf_pr","GetUModAndUpdateInfo didn't retreive any result: "+ onNextCount);
                    mUModConfigView.finishActivity();
                }
                mUModConfigView.hideProgressBar();
            }

            @Override
            public void onError(Throwable e) {
                Log.e("conf_pr", "Error while populating: ", e);
                //TODO BUG-RISK Improve this with exception subclass polymorphism
                if (e instanceof GetUModAndUpdateInfo.UnconnectedFromAPModeUModException){
                    //TODO should the presenter call finishActivity or the fragment do it as part of launchWiFiSettings??
                    mUModConfigView.launchWiFiSettings();
                } else {
                    if (mUModConfigView.isActive()){
                        mUModConfigView.finishActivity();
                    }
                }

            }

            @Override
            public void onNext(GetUModAndUpdateInfo.ResponseValues response) {
                onNextCount.plusOne();
                uModToConfig = response.getUMod();
                if (uModToConfig == null){
                    Log.e("conf_prs", "umod to config is null.");
                    if (mUModConfigView.isActive()){
                        mUModConfigView.finishActivity();
                    }
                } else {
                    Log.d("conf_prs", response.getUMod().toString());
                    if (mUModConfigView.isActive()){
                        mUModConfigView.showUModConfigs(createViewModel(uModToConfig));
                    }
                }
            }
        });
    }

    @Override
    public void adminUModUsers() {
        mUModConfigView.showUModUsers(mUModUUID);
    }

    @Override
    public void updateUModAlias(UModConfigViewModel viewModel) {
        final IntegerContainer onNextCount = new IntegerContainer(0);
        mUpdateUModAlias.execute(new UpdateUModAlias.RequestValues(viewModel.getuModUUID(), viewModel.getAliasText()),
                new Subscriber<UpdateUModAlias.ResponseValues>() {
            @Override
            public void onCompleted() {
                if (onNextCount.getValue() <= 0){
                    Log.e("conf_pr","updateUModAlias didn't retreive any result: "+ onNextCount);
                    mUModConfigView.finishActivity();
                }
                mUModConfigView.refreshOngoingNotification();
            }

            @Override
            public void onError(Throwable e) {
                mUModConfigView.showAliasConfigFailMsg();
            }

            @Override
            public void onNext(UpdateUModAlias.ResponseValues responseValues) {
                if (responseValues.getUMod() != null){
                    onNextCount.plusOne();
                    mUModConfigView.showAliasConfigSuccessMsg();
                }
            }
        });
    }

    @Override
    public void updateUModWiFiCredentials(final UModConfigViewModel viewModel) {
        final IntegerContainer onNextCount = new IntegerContainer(0);
        mUpdateWiFiCredentials.execute(
                new UpdateWiFiCredentials.RequestValues(
                        viewModel.getuModUUID(),
                        viewModel.getWifiSSIDText(),
                        viewModel.getWifiPasswordText()),
                new Subscriber<UpdateWiFiCredentials.ResponseValues>() {
            @Override
            public void onCompleted() {
                if (onNextCount.getValue() <= 0){
                    Log.e("conf_pr","updateUModWiFiCredentials didn't retreive any result: "+ onNextCount);
                    mUModConfigView.finishActivity();
                }
            }

            @Override
            public void onError(Throwable e) {
                /*
                //TODO implement error parsing
                if (response.getResponseError()!= null && response.getResponseError().getErrorCode() == 405){
                    mUModConfigView.showConfigurationFailureMessage("WiFi AP");
                } else {
                    mUModConfigView.showConfigurationSuccessMessage("WiFi AP");
                }
                 */
                mUModConfigView.showWiFiCredentialsConfigFailMsg();
                Log.e("config_pr", e.getMessage());
            }

            @Override
            public void onNext(UpdateWiFiCredentials.ResponseValues responseValues) {
                onNextCount.plusOne();
                mUModConfigView.showWiFiCredentialsConfigSuccessMsg();
            }
        });
    }


    private void showUModSettings(UMod uMod) {
        // The view may not be able to handle UI updates anymore
        Log.e("config_prs", uMod.toString());
        if (mUModConfigView.isActive()) {
            mUModConfigView.setUModUUID(uMod.getUUID());
            mUModConfigView.setUModIPAddress(uMod.getConnectionAddress());
        }
    }

    private UModConfigViewModel createViewModel(UMod uMod){
        String uModUUID = uMod.getUUID();
        String connectionStatusText =  uMod.getuModSource() == UMod.UModSource.LAN_SCAN ?
                "CONECTADO" : "DESCONECTADO";
        String aliasText = uMod.getAlias();
        String wifiSSIDText = uMod.getWifiSSID();
        boolean adminLayoutVisible = uMod.getAppUserLevel() == UModUser.Level.ADMINISTRATOR;
        String wifiPasswordText = null;
        boolean wifiSettingsVisible = uMod.getState() == UMod.State.AP_MODE;
        boolean ongoingNotifSwitchChecked = uMod.isOngoingNotificationEnabled();
        String locationLatLong;
        if (uMod.getuModLocation()!=null){
            locationLatLong = uMod.getuModLocation().getLatitude() + "," + uMod.getuModLocation().getLatitude();
        } else {
            locationLatLong = "DESCONOCIDA";
        }
        String uModSysInfoText = uMod.getUUID()
                + "\n" + uMod.getConnectionAddress()
                + "\n" + locationLatLong
                + "\n" + uMod.getSWVersion();

        UModConfigViewModel viewModel =
                new UModConfigViewModel(uModUUID,
                        connectionStatusText,
                        aliasText,
                        wifiSSIDText,
                        adminLayoutVisible,
                        uModSysInfoText,
                        wifiPasswordText,
                        wifiSettingsVisible,
                        ongoingNotifSwitchChecked,
                        locationLatLong);

        return viewModel;
    }

    @Override
    public void updateUModFirmware() {
        final IntegerContainer onNextCount = new IntegerContainer(0);
        mUpgradeUModFirmware.execute(new UpgradeUModFirmware.RequestValues(uModToConfig.getUUID()),
                new Subscriber<UpgradeUModFirmware.ResponseValues>() {
                    @Override
                    public void onCompleted() {
                        if (onNextCount.getValue() <= 0){
                            Log.e("conf_pr","updateUModFirmware didn't retreive any result: "+ onNextCount);
                            mUModConfigView.finishActivity();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("config_pr", e.getMessage(),e);
                        mUModConfigView.hideUpdateDialog();
                        mUModConfigView.showFirmwareUpdateFailMsg();
                    }

                    @Override
                    public void onNext(UpgradeUModFirmware.ResponseValues responseValues) {
                        onNextCount.plusOne();
                        //mUModConfigView.setUpdateDialogMessage("Actualización Exitosa.");
                        mUModConfigView.hideUpdateDialog();
                        mUModConfigView.showFirmwareUpdateSucessMsg();
                    }
                });
    }

    @Override
    public void cancelFirmwareUpgrade() {
        mUpgradeUModFirmware.unsubscribe();
        mUModConfigView.showUpgradeCancellationMsg();
    }

    @Override
    public void factoryResetUMod() {
        final IntegerContainer onNextCount = new IntegerContainer(0);
        mFactoryReset.execute(new FactoryResetUMod.RequestValues(uModToConfig.getUUID()),
                new Subscriber<FactoryResetUMod.ResponseValues>() {
                    @Override
                    public void onCompleted() {
                        if (onNextCount.getValue() <= 0){
                            Log.e("conf_pr","factoryResetUMod didn't retreive any result: "+ onNextCount);
                            mUModConfigView.showResetFailMsg();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mUModConfigView.showResetFailMsg();
                    }

                    @Override
                    public void onNext(FactoryResetUMod.ResponseValues responseValues) {
                        onNextCount.plusOne();
                        mUModConfigView.showResetSuccessMsg();
                        Observable.timer(1100L, TimeUnit.MILLISECONDS)
                                .subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread())
                                .map(new Func1<Long, Object>() {
                                    @Override
                                    public Object call(Long aLong) {
                                        if (mUModConfigView.isActive()){
                                            mUModConfigView.finishActivity();
                                        }
                                        return aLong;
                                    }
                                })
                                .subscribe();
                    }
                });
    }

    @Override
    public void setNotificationStatus(@NonNull String uModUUID, final Boolean notificationEnabled) {
        checkNotNull(uModUUID, "umoduuid cannot be null!");
        mSetOngoingNotificationStatus.unsubscribe();
        mSetOngoingNotificationStatus.execute(new SetOngoingNotificationStatus.RequestValues(uModUUID, notificationEnabled),
                new Subscriber<RxUseCase.NoResponseValues>() {
                    @Override
                    public void onCompleted() {
                        mUModConfigView.showOngoingNotificationStatusChangeSuccess(notificationEnabled);
                        mUModConfigView.refreshOngoingNotification();
                    }

                    @Override
                    public void onError(Throwable e) {
                        mUModConfigView.showOngoingNotificationStatusChangeFail(notificationEnabled);
                    }

                    @Override
                    public void onNext(RxUseCase.NoResponseValues noResponseValues) {

                    }
                });
    }
}