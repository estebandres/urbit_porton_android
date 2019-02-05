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

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Strings;
import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.source.PhoneConnectivity;
import com.urbit_iot.porton.umodconfig.domain.usecase.FactoryResetUMod;
import com.urbit_iot.porton.umodconfig.domain.usecase.GetCurrentLocation;
import com.urbit_iot.porton.umodconfig.domain.usecase.GetUModAndUpdateInfo;
import com.urbit_iot.porton.umodconfig.domain.usecase.GetUModSystemInfo;
import com.urbit_iot.porton.umodconfig.domain.usecase.ResetUModCalibration;
import com.urbit_iot.porton.umodconfig.domain.usecase.SaveUMod;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.umodconfig.domain.usecase.UpdateUModAlias;
import com.urbit_iot.porton.umodconfig.domain.usecase.UpdateUModLocationData;
import com.urbit_iot.porton.umodconfig.domain.usecase.UpdateWiFiCredentials;
import com.urbit_iot.porton.umodconfig.domain.usecase.UpgradeUModFirmware;
import com.urbit_iot.porton.umodconfig.domain.usecase.SetOngoingNotificationStatus;
import com.urbit_iot.porton.util.GlobalConstants;
import com.urbit_iot.porton.util.IntegerContainer;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;


import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
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

    @NonNull
    private final GetCurrentLocation mGetCurrentLocation;

    @NonNull
    private final ResetUModCalibration mResetUModCalibration;

    @NonNull
    private final UpdateUModLocationData mUpdateUModLocationData;

    @Nullable
    private UMod uModToConfig;

    @NonNull
    private String mUModUUID;

    @Nullable
    private Location mCurrentLocation;
    @Nullable
    private String mCurrentLocationAddressString;

    private UModConfigViewModel viewModel = null;

    @NonNull
    private PhoneConnectivity mConnectivityInfo;

    private Subscription finishActivityAfterFactoryReset;

    private Subscription launchPopulationAfterReconnection;

    private Subscription updateSettingsSubscription;

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
                               @NonNull SetOngoingNotificationStatus mSetOngoingNotificationStatus,
                               @NonNull GetCurrentLocation mGetCurrentLocation,
                               @NonNull ResetUModCalibration mResetUModCalibration,
                               @NonNull UpdateUModLocationData mUpdateUModLocationData,
                               @NonNull PhoneConnectivity mConnectivityInfo) {
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
        this.mGetCurrentLocation = mGetCurrentLocation;
        this.mResetUModCalibration = mResetUModCalibration;
        this.mUpdateUModLocationData = mUpdateUModLocationData;
        this.mConnectivityInfo = mConnectivityInfo;
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
        populateUModSettings();
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
        mGetCurrentLocation.unsubscribe();
        mUpdateUModLocationData.unsubscribe();
        mResetUModCalibration.unsubscribe();
        if (finishActivityAfterFactoryReset != null){
            finishActivityAfterFactoryReset.unsubscribe();
        }
        if (launchPopulationAfterReconnection != null){
            launchPopulationAfterReconnection.unsubscribe();
        }
        if (updateSettingsSubscription != null){
            updateSettingsSubscription.unsubscribe();
        }
    }

    @Override
    public void populateUModSettings() {
        mUModConfigView.hideCompletely();
        if (Strings.isNullOrEmpty(mUModUUID)) {
            throw new RuntimeException("populateUModSettings() was called but umod is null.");
        }
        if (this.viewModel!=null){
            if (mUModConfigView.isActive()){
                mUModConfigView.showUModConfigs(this.viewModel);
                return;
            }
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
                if (e instanceof GetUModAndUpdateInfo.UnconnectedFromAPModeUModException){
                    //mUModConfigView.launchWiFiSettings();
                    if (mConnectivityInfo.connectToWifiAP(
                            GlobalConstants.URBIT_PREFIX+mUModUUID,
                            GlobalConstants.URBIT_PREFIX+mUModUUID)){
                        launchPopulationAfterReconnection = Observable.timer(10000L, TimeUnit.MILLISECONDS)
                                .doOnNext(aLong -> populateUModSettings())
                                .subscribe();
                        return;
                    }
                }
                if (mUModConfigView.isActive()){
                    mUModConfigView.finishActivity();
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
                        viewModel = createViewModel(uModToConfig);
                        mUModConfigView.showUModConfigs(viewModel);
                    }
                }
            }
        });
    }

    private void connectToAPModeUMod(String mUModUUID){

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
                Log.e("config_pr", "" + e.getMessage());
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
        String connectionStatusText ="CONECTADO";
        /*
        String connectionStatusText =  uMod.getuModSource() == UMod.UModSource.LAN_SCAN ?
                "CONECTADO" : "DESCONECTADO";
                */
        String aliasText = uMod.getAlias();
        String wifiSSIDText = uMod.getWifiSSID();
        boolean adminLayoutVisible = uMod.getAppUserLevel() == UModUser.Level.ADMINISTRATOR;
        boolean controlButtonsVisible = uMod.getAppUserLevel() == UModUser.Level.ADMINISTRATOR
                && uMod.getState() == UMod.State.STATION_MODE;
        String wifiPasswordText = null;
        boolean wifiSettingsVisible = uMod.getState() == UMod.State.AP_MODE;
        boolean ongoingNotifSwitchChecked = uMod.isOngoingNotificationEnabled();
        String latLongText = "";
        String addressText = uMod.getLocationAddressString();
        if (addressText == null || addressText.equals("")){
            if (uMod.getuModLocation()!=null
                    && uMod.getuModLocation().getLatitude() != 0.0
                    && uMod.getuModLocation().getLongitude() != 0.0){
                addressText = uMod.getuModLocation().getLatitude() + "," + uMod.getuModLocation().getLatitude();
            } else {
                addressText = "DESCONOCIDA";
            }
        }
        if (uMod.getuModLocation()!=null){
            latLongText = uMod.getuModLocation().getLatitude() + " "
                    + uMod.getuModLocation().getLongitude();
        }
        String uModSysInfoText = "ID: " + uMod.getUUID() + " VERSIÓN: " + uMod.getSWVersion()
                + "\nIP: " +uMod.getConnectionAddress();
        boolean updateButtonVisible = uMod.getWifiSSID() != null
                && uMod.getWifiSSID().equals(mConnectivityInfo.getWifiAPSSID());
        boolean locationSettingsLayoutVisible = uMod.getState() != UMod.State.AP_MODE;

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
                        locationSettingsLayoutVisible, addressText,
                        updateButtonVisible,
                        controlButtonsVisible
                        );

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
                        Log.e("config_pr", "" + e.getMessage(),e);
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
                        finishActivityAfterFactoryReset = Observable.timer(1100L, TimeUnit.MILLISECONDS)
                                .subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread())
                                .map(aLong -> {
                                    if (mUModConfigView.isActive()){
                                        mUModConfigView.finishActivity();
                                    }
                                    return aLong;
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

    @Override
    public void getPhoneLocation() {
        //TODO here the update fab button should be enabled..
        mGetCurrentLocation.unsubscribe();
        mUModConfigView.showLocationLoadingProgressBar();
        mGetCurrentLocation.execute(new GetCurrentLocation.RequestValues(),
                new Subscriber<GetCurrentLocation.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModConfigView.hideLocationLoadingProgressBar();
            }

            @Override
            public void onError(Throwable e) {
                mUModConfigView.hideLocationLoadingProgressBar();
            }

            @Override
            public void onNext(GetCurrentLocation.ResponseValues responseValues) {
                String locationString = "";
                mCurrentLocationAddressString = responseValues.getLocationAddress();
                if (mCurrentLocationAddressString != null && !mCurrentLocationAddressString.equals("")){
                    locationString = mCurrentLocationAddressString;
                }
                mCurrentLocation = responseValues.getCurrentLocation();
                if (mCurrentLocation != null) {
                    locationString = locationString
                            + "\n"
                            + mCurrentLocation.getLatitude()
                            + "  "
                            + mCurrentLocation.getLongitude();
                }
                mUModConfigView.updateLocationText(locationString);
            }
        });
    }

    @Override
    public void updateUModLocationData(){
        mUpdateUModLocationData.unsubscribe();
        mUpdateUModLocationData.execute(
                new UpdateUModLocationData.RequestValues(
                        mUModUUID,
                        mCurrentLocation,
                        mCurrentLocationAddressString),
                new Subscriber<UpdateUModLocationData.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModConfigView.showLocationUpdateSuccessMsg();
            }

            @Override
            public void onError(Throwable e) {
                mUModConfigView.showLocationUpdateFailureMsg();
            }

            @Override
            public void onNext(UpdateUModLocationData.ResponseValues responseValues) {

            }
        });
    }

    @Override
    public void updateSettings(String newAlias, String newWifiSsid, String newWifiPassword, boolean updateLocation) {
        Observable<UpdateUModAlias.ResponseValues> updateAliasObs;
        Observable<UpdateWiFiCredentials.ResponseValues> updateWifiCredentialsObs;
        Observable<UpdateUModLocationData.ResponseValues> updateLocationObs;
        if (Strings.isNullOrEmpty(newAlias)
                || this.viewModel.getAliasText().equals(newAlias)){
            updateAliasObs = Observable.empty();
        } else {
            this.viewModel.setAliasText(newAlias);
            updateAliasObs = mUpdateUModAlias.buildUseCase(
                    new UpdateUModAlias.RequestValues(this.mUModUUID,newAlias));
        }

        if (!Strings.isNullOrEmpty(newWifiSsid)
                && !Strings.isNullOrEmpty(newWifiPassword)){//Wifi credentials validation here?
            updateWifiCredentialsObs = mUpdateWiFiCredentials.buildUseCase(
                    new UpdateWiFiCredentials.RequestValues(
                            this.mUModUUID,
                            newWifiSsid,
                            newWifiPassword));
            this.viewModel.setWifiSSIDText(newWifiSsid);
            this.viewModel.setWifiPasswordText(newWifiPassword);
        } else {
            updateWifiCredentialsObs = Observable.empty();
        }
        if (updateLocation && mCurrentLocation != null){
            updateLocationObs = mUpdateUModLocationData.buildUseCase(new UpdateUModLocationData.RequestValues(
                    mUModUUID,
                    mCurrentLocation,
                    mCurrentLocationAddressString));
        } else {
            updateLocationObs = Observable.empty();
        }

        updateSettingsSubscription = updateAliasObs.toCompletable()
                .onErrorComplete(throwable -> {
                    mUModConfigView.showAliasConfigFailMsg();
                    return true;
                })
                .andThen(updateLocationObs.toCompletable())
                .doOnCompleted(() -> mCurrentLocation = null)
                .onErrorComplete(throwable -> {
                    mUModConfigView.showLocationUpdateFailureMsg();
                    mCurrentLocation = null;
                    return true;
                })
                .andThen(updateWifiCredentialsObs.toCompletable())
                .timeout(4000L,TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mUModConfigView::showSettingsApplySuccessMsg,
                        throwable -> mUModConfigView.showWiFiCredentialsConfigFailMsg());
    }

    @Override
    public void resetCalibration() {
        mResetUModCalibration.execute(new ResetUModCalibration.RequestValues(
                this.mUModUUID,
                mConnectivityInfo.getConnectionType()
                        == PhoneConnectivity.ConnectionType.WIFI,
                mConnectivityInfo.getWifiAPSSID()),
                new Subscriber<ResetUModCalibration.ResponseValues>() {
                    @Override
                    public void onCompleted() {
                        mUModConfigView.showCalibrationResetSuccessMsg();
                    }

                    @Override
                    public void onError(Throwable e) {
                        mUModConfigView.showCalibrationResetFailMsg();
                    }

                    @Override
                    public void onNext(ResetUModCalibration.ResponseValues responseValues) {
                        Log.d("CONFIG_PRES", "Reset Calibration Result: " + responseValues.getResult().toString());
                    }
                });
    }
}