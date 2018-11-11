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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.urbit_iot.onekey.R;
import com.urbit_iot.onekey.umodsnotification.UModsNotifService;
import com.urbit_iot.onekey.umodusers.UModUsersActivity;
import com.urbit_iot.onekey.util.GlobalConstants;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main UI for the add task screen. Users can enter a task title and description.
 */
public class UModConfigFragment extends Fragment implements UModConfigContract.View {

    public static final String ARGUMENT_CONFIG_UMOD_ID = "UMOD_UUID";
    public static final String ARGUMENT_UMOD_USERS = "UMOD_UUID";
    private static final int REQUEST_EDIT_TASK = 1;

    private UModConfigContract.Presenter mPresenter;

    private EditText mAliasTextInput;

    private TextView mUModSysInfoTextInput;

    private EditText mWiFiSSIDTextInput;

    private TextView mWiFiPasswordTextInput;

    private LinearLayout mAdminSettingsLayout;

    private ImageButton mUsersButton;

    private ImageButton mFactoryResetButton;

    private ImageButton mFirmwareUpdateButton;

    private UModConfigViewModel mViewModel;

    private TextWatcher mTextWatcher;

    private FloatingActionButton mUploadButton;

    private Dialog mFirmwareUpdateDialog;

    private Dialog mFactoryResetDialog;

    private Dialog mFirmwareUpdateProgressDialog;

    private Dialog mCalibrationResetDialog;

    private ProgressBar mProgressBar;

    private LinearLayout mAllSettingsLinearLayout;

    private LinearLayout mWiFiSettings;

    private Switch mOngoingNotifSwitch;

    private ProgressBar mLocationProgressBar;

    private ImageButton mLocationUpdateButton;

    private TextView mLocationText;

    private Location mUModLocation;

    private LinearLayout mLocationSettingsLayout;

    private boolean mUpdateLocation;

    private ImageButton mCalibrationResetButton;

    public static UModConfigFragment newInstance() {
        return new UModConfigFragment();
    }

    public UModConfigFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.subscribe();
    }

    @Override
    public void setPresenter(@NonNull UModConfigContract.Presenter presenter) {
        mPresenter = checkNotNull(presenter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mUploadButton = getActivity().findViewById(R.id.fab_upload_settings);
        mUploadButton.hide();
        mUploadButton.setBackgroundColor(getResources().getColor(R.color.request_access_slider_background));
        mUploadButton.setImageResource(R.drawable.ic_upload);
        mUploadButton.setOnClickListener(v -> {
            submitSettingsChanges();
            mWiFiPasswordTextInput.setText("");
        });
    }

    private void submitSettingsChanges(){
        String aliasText = mAliasTextInput.getText().toString();
        if (!Strings.isNullOrEmpty(aliasText)){
            aliasText = aliasText.trim();
        } else {
            aliasText = null;
        }

        String wifiSSID = null;
        String wifiPassword = null;
        if (mViewModel.isWifiSettingsVisible()){
            wifiSSID = mWiFiSSIDTextInput.getText().toString();
            wifiPassword = mWiFiPasswordTextInput.getText().toString();
        }

        mPresenter.updateSettings(aliasText, wifiSSID, wifiPassword, mUpdateLocation);
        mUpdateLocation = false;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.umod_config_frag, container, false);

        mAliasTextInput = root.findViewById(R.id.alias_text_input);
        mUModSysInfoTextInput =  root.findViewById(R.id.umod_sys_info);
        mWiFiSSIDTextInput = root.findViewById(R.id.wifi_ssid);
        mWiFiPasswordTextInput = (EditText) root.findViewById(R.id.wifi_password);
        mUsersButton =  root.findViewById(R.id.users_button);
        mFactoryResetButton =  root.findViewById(R.id.factory_reset_button);
        mFirmwareUpdateButton = root.findViewById(R.id.firmware_update_button);
        mAdminSettingsLayout = root.findViewById(R.id.admin_settings);
        mProgressBar = root.findViewById(R.id.umod_config_load_bar);
        mProgressBar.setVisibility(View.GONE);
        mAllSettingsLinearLayout = root.findViewById(R.id.all_settings);
        mWiFiSettings =  root.findViewById(R.id.wifi_settings);
        TextView upgradeSubtext = root.findViewById(R.id.upgrade_button_subtext);
        upgradeSubtext.setSelected(true);
        TextView recalibrateSubText = root.findViewById(R.id.calibration_reset_subtext);
        recalibrateSubText.setSelected(true);
        mOngoingNotifSwitch = root.findViewById(R.id.umod_notif_switch);
        mLocationSettingsLayout = root.findViewById(R.id.location_settings);
        mLocationProgressBar = root.findViewById(R.id.umod_config__location_load_bar);
        mLocationProgressBar.setVisibility(View.GONE);
        mLocationUpdateButton = root.findViewById(R.id.umod_config__location_update_button);
        mLocationUpdateButton.setOnClickListener(view -> {
            mUpdateLocation = true;
            mUploadButton.show();
            mPresenter.getPhoneLocation();
        });
        mLocationText = root.findViewById(R.id.umod_config__location_text);
        mCalibrationResetButton = root.findViewById(R.id.calibration_reset_button);

        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String trimmedString = s.toString().trim();
                if (trimmedString.length() != 0) {
                    mUploadButton.show();
                } else {
                    //TODO KNOWN BUG: will never show the upload button
                    // when changing WiFI SSID or Password to spaced characters only.
                    mUploadButton.hide();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        //View dialogContentView = inflater.inflate();
        mFirmwareUpdateDialog =  new AlertDialog.Builder(getActivity())
                .setTitle(R.string.firmware_update_dialog__title)
                .setMessage(R.string.firmware_update_dialog__started_message)
                .setNegativeButton(R.string.firmware_update_dialog__cancel_button,
                        (dialog, which) -> Log.d("OTADialog", "CANCEL"))
                .setPositiveButton(R.string.firmware_update_dialog__update_button, (dialogInterface, i) -> {
                    mFirmwareUpdateProgressDialog.show();
                    mPresenter.updateUModFirmware();
                })
                .setCancelable(false)
                .create();

        LayoutInflater dialogViewInflater = this.getLayoutInflater();
        View dialogView = dialogViewInflater.inflate(R.layout.firmware_update__dialog, null);
        TextView dialogMessage = dialogView.findViewById(R.id.firmware_update_dialog__message);
        dialogMessage.setText(R.string.firmware_update_time_warning);
        //ProgressBar dialogProgressBar = dialogView.findViewById(R.id.firmware_update_dialog__progress_bar);

        mFirmwareUpdateProgressDialog =  new AlertDialog.Builder(getActivity())
                .setTitle(R.string.firmware_update_dialog__title)
                //.setView(R.layout.firmware_update__dialog)
                .setView(dialogView)
                .setNegativeButton(R.string.firmware_update_dialog__cancel_button,
                        (dialog, which) -> {
                            mPresenter.cancelFirmwareUpgrade();
                            Log.d("OTADialog", "CANCEL");
                        })
                .setCancelable(false)
                .create();

        mFactoryResetDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.factory_reset_dialog__title)
                .setMessage(R.string.factory_reset_dialog__message)
                .setNegativeButton(R.string.factory_reset_dialog__cancel_button,
                        (dialogInterface, i) -> Log.d("FactoryDialog", "CANCEL"))
                .setPositiveButton(R.string.factory_reset_dialog__reset_button,
                        (dialogInterface, i) -> mPresenter.factoryResetUMod())
                .setCancelable(false)
                .create();

        mCalibrationResetDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.calibration_reset_dialog_title)
                .setMessage(R.string.calibration_reset_dialog_msg)
                .setNegativeButton("Cancelar",(dialogInterface, i) -> Log.d("CONFIG_FRAG", "Calibration Reset CANCELLED"))
                .setPositiveButton("RESTAURAR", (dialogInterface, i) -> mPresenter.resetCalibration())
                .setCancelable(false)
                .create();

        mCalibrationResetButton.setOnClickListener(view -> mCalibrationResetDialog.show());

        setHasOptionsMenu(true);
        setRetainInstance(true);
        return root;
    }

    @Override
    public void showUModsList() {
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    @Override
    public void setUModUUID(String title) {
        mAliasTextInput.setText(title);
    }

    @Override
    public void setUModIPAddress(String description) {
        mUModSysInfoTextInput.setText(description);
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void showUModUsers(@NonNull String taskId) {
        Intent intent = new Intent(getContext(), UModUsersActivity.class);
        intent.putExtra(ARGUMENT_UMOD_USERS, taskId);
        startActivityForResult(intent, REQUEST_EDIT_TASK);
    }

    @Override
    //TODO find a more elegant way to do this in a 'clean' way.
    public void launchWiFiSettings() {
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null){
            this.getActivity().finish();
        } else {
            Log.d("umodConf_frag", wifiInfo.toString());
            startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS),0);
            this.getActivity().finish();
        }
    }

    @Override
    public void hideCompletely() {
        mAllSettingsLinearLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public String getWiFiAPSSID(){
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null){
            return null;
        } else {
            Log.d("umodConf_frag", wifiInfo.toString());
            return wifiInfo.getSSID();
        }
    }

    @Override
    public void showUModConfigs(final UModConfigViewModel viewModel) {
        mViewModel = viewModel;

        mAliasTextInput.setText(mViewModel.getAliasText());
        mAliasTextInput.setSelection(mAliasTextInput.getText().length());
        mWiFiSSIDTextInput.setText(mViewModel.getWifiSSIDText());
        mUModSysInfoTextInput.setText(mViewModel.getuModSysInfoText());

        mAliasTextInput.addTextChangedListener(mTextWatcher);
        mWiFiSSIDTextInput.addTextChangedListener(mTextWatcher);
        mWiFiPasswordTextInput.addTextChangedListener(mTextWatcher);

        mOngoingNotifSwitch.setChecked(viewModel.isOngoingNotifSwitchChecked());

        mOngoingNotifSwitch.setOnCheckedChangeListener((compoundButton, isChecked) ->
                this.mPresenter.setNotificationStatus(viewModel.getuModUUID(),isChecked));

        if (viewModel.isLocationSettingsLayoutVisible()){
            mLocationSettingsLayout.setVisibility(View.VISIBLE);
        } else {
            mLocationSettingsLayout.setVisibility(View.GONE);
        }

        if (viewModel.isAdminLayoutVisible()){
            mFirmwareUpdateButton.setOnLongClickListener(v -> {
                mFirmwareUpdateDialog.show();
                return false;
            });

            mFactoryResetButton.setOnClickListener(view -> mFactoryResetDialog.show());

            mUsersButton.setOnClickListener(v -> mPresenter.adminUModUsers());

            if (viewModel.isWifiSettingsVisible()){
                mWiFiSettings.setVisibility(View.VISIBLE);
            } else {
                mWiFiSettings.setVisibility(View.GONE);
            }

            if (viewModel.isUpdateButtonVisible()){
                mFirmwareUpdateButton.setVisibility(View.VISIBLE);
            } else {
                mFirmwareUpdateButton.setVisibility(View.INVISIBLE);
            }
            mAdminSettingsLayout.setVisibility(View.VISIBLE);
        } else {
            mAdminSettingsLayout.setVisibility(View.GONE);
        }
        this.updateLocationText(viewModel.getLocationText());
        mAllSettingsLinearLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void showSettingsApplySuccessMsg() {
        showSnackBarMessage(getString(R.string.settings_apply_success_message));
    }

    @Override
    public void showAliasConfigFailMsg() {
        showSnackBarMessage(getString(R.string.alias_config_fail_message));
    }

    @Override
    public void showAliasConfigSuccessMsg() {
        showSnackBarMessage(getString(R.string.alias_config_success_message));
    }

    @Override
    public void showWiFiCredentialsConfigFailMsg() {
        showSnackBarMessage(getString(R.string.wifi_credentials_config_fail_message));
    }

    @Override
    public void showWiFiCredentialsConfigSuccessMsg() {
        showSnackBarMessage(getString(R.string.wifi_credentials_config_success_message));
    }

    @Override
    public void hideUpdateDialog() {
        mFirmwareUpdateProgressDialog.dismiss();
    }

    @Override
    public void showFirmwareUpdateSucessMsg() {
        showSnackBarMessage(getString(R.string.firmware_update_success_message));
    }

    @Override
    public void showFirmwareUpdateFailMsg() {
        showSnackBarMessage(getString(R.string.firmware_update_fail_message));
    }

    @Override
    public void showResetFailMsg() {
        showSnackBarMessage(getString(R.string.factory_reset_fail_message));
    }

    @Override
    public void showResetSuccessMsg() {
        showSnackBarMessage(getString(R.string.factory_reset_started_message));
    }

    @Override
    public void showCalibrationResetSuccessMsg() {
        showSnackBarMessage(getString(R.string.calibration_reset_success));
    }

    @Override
    public void showCalibrationResetFailMsg() {
        showSnackBarMessage(getString(R.string.calibration_reset_failed));
    }

    @Override
    public void finishActivity() {
        Activity configActivity = getActivity();
        if (configActivity!=null){
            configActivity.finish();
        }
    }

    @Override
    public void showUpgradeCancellationMsg() {
        showSnackBarMessage(getString(R.string.firmware_update_cancel_message));
    }

    @Override
    public void showProgressBar() {
        this.mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgressBar() {
        this.mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void showOngoingNotificationStatusChangeSuccess(Boolean notificationEnabled) {
        if(notificationEnabled){
            showSnackBarMessage(getString(R.string.ongoing_notif_enabled));
        } else {
            showSnackBarMessage(getString(R.string.ongoing_notif_disbled));
        }
    }

    private void showSnackBarMessage(String message) {
        View configFragmentView = getView();
        if (configFragmentView != null){
            Snackbar.make(configFragmentView, message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void showOngoingNotificationStatusChangeFail(Boolean notificationEnabled) {
        if(notificationEnabled){
            showSnackBarMessage(getString(R.string.ongoing_notif_enabled_failed));
        } else {
            showSnackBarMessage(getString(R.string.ongoing_notif_disbled_failed));
        }
    }

    @Override
    public void refreshOngoingNotification() {
        if (UModsNotifService.SERVICE_IS_ALIVE){
            Context context = getContext();
            if (context != null){
                Intent serviceIntent = new Intent(context, UModsNotifService.class);
                serviceIntent.setAction(GlobalConstants.ACTION.REFRESH_UMODS);
                context.startService(serviceIntent);
            } else {
                Log.e("config_fr", "Context is null");
            }
        }
    }

    @Override
    public void updateLocationText(String locationAddress) {
        this.mLocationText.setText(locationAddress);
    }

    @Override
    public void showLocationLoadingProgressBar() {
        this.mLocationProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLocationLoadingProgressBar() {
        this.mLocationProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void showLocationUpdateSuccessMsg() {
        showSnackBarMessage(getString(R.string.location_update_success_msg));
    }

    @Override
    public void showLocationUpdateFailureMsg() {
        showSnackBarMessage(getString(R.string.location_update_failure_msg));
    }
}
