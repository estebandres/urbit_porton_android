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
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.umodconfig.domain.usecase.FactoryResetUMod;
import com.urbit_iot.onekey.umodconfig.domain.usecase.GetUModAndUpdateInfo;
import com.urbit_iot.onekey.umodconfig.domain.usecase.GetUModSystemInfo;
import com.urbit_iot.onekey.umodconfig.domain.usecase.SaveUMod;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.umodconfig.domain.usecase.UpdateUModAlias;
import com.urbit_iot.onekey.umodconfig.domain.usecase.UpdateWiFiCredentials;
import com.urbit_iot.onekey.umodconfig.domain.usecase.UpgradeUModFirmware;
import com.urbit_iot.onekey.util.IntegerContainer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;


import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

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
                               @NonNull UpgradeUModFirmware mUpgradeUModFirmware) {
        this.mUModUUID = umodUUID;
        this.mUModConfigView = addTaskView;
        this.mGetUModAndUpdateInfo = getUModAndUpdateInfo;
        this.mUpdateUModAlias = updateUModAlias;
        this.mSaveUMod = saveUMod;
        this.mUpdateWiFiCredentials = mUpdateWiFiCredentials;
        this.mGetUModSystemInfo = mGetUModSystemInfo;
        this.mFactoryReset = mFactoryReset;
        this.mUpgradeUModFirmware = mUpgradeUModFirmware;
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
    }

    @Override
    public void saveUMod(String title, String description) {
        if (isNewTask()) {
            createTask(title, description);
        } else {
            updateTask(title, description);
        }
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
                Log.e("conf_pr",e.getMessage());
                //showEmptyTaskError();
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
        if (Strings.isNullOrEmpty(mUModUUID)) {
            mUModConfigView.showEmptyUModError();
            return;
        }
        mUModConfigView.showEditUModUsers(mUModUUID);
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
            }

            @Override
            public void onError(Throwable e) {
                mUModConfigView.showConfigurationFailureMessage("Alias");
            }

            @Override
            public void onNext(UpdateUModAlias.ResponseValues responseValues) {
                if (responseValues.getUMod() != null){
                    onNextCount.plusOne();
                    mUModConfigView.showConfigurationSuccessMessage("Alias");
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
                mUModConfigView.showConfigurationFailureMessage("WiFi AP");
                Log.e("config_pr", e.getMessage());
            }

            @Override
            public void onNext(UpdateWiFiCredentials.ResponseValues responseValues) {
                onNextCount.plusOne();
                mUModConfigView.showConfigurationSuccessMessage("WiFi AP");
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
        boolean adminLayoutVisible = uMod.getuModSource() == UMod.UModSource.LAN_SCAN
                && uMod.getAppUserLevel() == UModUser.Level.ADMINISTRATOR;
        String uModSysInfoText =
                uMod.getUUID() + "\n" +
                uMod.getConnectionAddress() + "\n" +
                uMod.getSWVersion();
        String wifiPasswordText = null;
        boolean wifiSettingsVisible = uMod.getState() == UMod.State.AP_MODE;

        UModConfigViewModel viewModel =
                new UModConfigViewModel(uModUUID,
                        connectionStatusText,
                        aliasText,
                        wifiSSIDText,
                        adminLayoutVisible,
                        uModSysInfoText,
                        wifiPasswordText,
                        wifiSettingsVisible);

        return viewModel;
    }

    private void showSaveError() {
        // Show error, log, etc.
    }

    private void showEmptyTaskError() {
        // The view may not be able to handle UI updates anymore
        if (mUModConfigView.isActive()) {
            mUModConfigView.showEmptyUModError();
        }
    }

    private boolean isNewTask() {
        return mUModUUID == null;
    }

    private void createTask(String title, String description) {
        UMod newTask = new UMod(title, description,true);
        if (newTask.isEmpty()) {
            mUModConfigView.showEmptyUModError();
        } else {
            mSaveUMod.execute(new SaveUMod.RequestValues(newTask), new Subscriber<SaveUMod.ResponseValues>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    showSaveError();
                }

                @Override
                public void onNext(SaveUMod.ResponseValues responseValues) {
                    mUModConfigView.showUModsList();
                }
            });
        }
    }

    private void updateTask(String title, String description) {
        if (mUModUUID == null) {
            throw new RuntimeException("updateTask() was called but task is new.");
        }
        UMod newTask = new UMod(title, description, true);
        mSaveUMod.execute(new SaveUMod.RequestValues(newTask), new Subscriber<SaveUMod.ResponseValues>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                showSaveError();
            }

            @Override
            public void onNext(SaveUMod.ResponseValues responseValues) {
                // After an edit, go back to the list.
                mUModConfigView.showUModsList();
            }
        });
    }

    @Override
    public void getUModSystemInfo(String uModUUID) {
        final IntegerContainer onNextCount = new IntegerContainer(0);
        this.mGetUModSystemInfo.execute(new GetUModSystemInfo.RequestValues(uModUUID), new Subscriber<GetUModSystemInfo.ResponseValues>() {
            @Override
            public void onCompleted() {
                if (onNextCount.getValue() <= 0){
                    Log.e("conf_pr","getUModSystemInfo didn't retreive any result: "+ onNextCount);
                    mUModConfigView.finishActivity();
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.d("config_pr", "RESPUESTA: " + e.getMessage() + " TIPO : " + e.getClass());
                if(e instanceof HttpException){
                    if(((HttpException) e).response().errorBody() != null){
                        try {
                            Log.e("config_pr", ((HttpException) e).response().errorBody().string());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        Log.e("config_pr", " " + ((HttpException) e).response().code());
                        Log.e("config_pr", " " + ((HttpException) e).response().message());
                        Log.e("config_pr", " " + ((HttpException) e).response().toString());
                        //((HttpException) e).response().toString();
                    }
                }
                showEmptyTaskError();
            }

            @Override
            public void onNext(GetUModSystemInfo.ResponseValues responseValues) {
                onNextCount.plusOne();
                Log.d("config_pr", "RESPUESTA: \n" + responseValues.getRPCResponse());
            }
        });

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
                        mUModConfigView.showUpdateErrorMessage();
                    }

                    @Override
                    public void onNext(UpgradeUModFirmware.ResponseValues responseValues) {
                        onNextCount.plusOne();
                        //mUModConfigView.setUpdateDialogMessage("Actualización Exitosa.");
                        mUModConfigView.hideUpdateDialog();
                        mUModConfigView.showUpdateSucessMessage();
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
}