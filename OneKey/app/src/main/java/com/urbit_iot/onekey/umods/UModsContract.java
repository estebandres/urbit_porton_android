package com.urbit_iot.onekey.umods;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.BasePresenter;
import com.urbit_iot.onekey.BaseView;
import com.urbit_iot.onekey.data.UMod;

import java.util.List;

/**
 * Created by steve-urbit on 02/08/17.
 * This specifies the contract between the view and the presenter.
 */

public interface UModsContract {
    interface View extends BaseView<Presenter> {

        void setLoadingIndicator(boolean active);

        void showUMods(List<UModViewModel> uMods);

        void appendUMod(UModViewModel uMod);

        void showUMod(UModViewModel uMod);

        void showAddUMod();

        void showUModConfigUi(String taskId);

        void showOngoingNotifStatusChanged(Boolean ongoingNotifStatus);

        void showUModNotificationEnabled();

        void showUModNotificationDisabled();

        void showLoadingUModsError();

        void showNoUMods();

        void showNotificationDisabledUModsFilterLabel();

        void showNotificationEnabledUModsFilterLabel();

        void showAllFilterLabel();

        void showNotificationDisabledUMods();

        void showNotificationEnabledUMods();

        boolean isNotificationDisabled();

        void showFilteringPopUpMenu();

        void showOpenCloseSuccess();

        void showOpenCloseFail();

        void showRequestAccessFailedMessage();

        void showRequestAccessCompletedMessage();

        void enableActionSlider(String uModUUID);

        void clearAllItems();

        void startOngoingNotification();

        void shutdownOngoingNotification();

        void refreshOngoingNotification();

        void showAlienUModsCleared();

        void removeItem(String uuid);

        void showDisconnectedSensorDialog();

        void showCalibrationDialogs();

        void showCalibrationSuccessMessage();

        void showCalibrationFailureMessage();
    }

    interface Presenter extends BasePresenter {

        void loadUMods(boolean forceUpdate);

        void addNewUMod();

        void openUModConfig(@NonNull String uModUUID);

        void clearAlienUMods();

        void setFiltering(UModsFilterType requestType);

        UModsFilterType getFiltering();

        void triggerUMod(String uModUUID);

        void requestAccess(String uModUUID);

        void setNotificationStatus(String uModUUID, Boolean notificationEnabled);

        void saveOngoingNotificationPreference(boolean isChecked);

        boolean fetchOngoingNotificationPreference();

        void processCalibrationDialogChoice(int choice);
    }
}
