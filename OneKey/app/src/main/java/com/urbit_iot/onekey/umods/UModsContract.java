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

        void showUMods(List<UMod> uMods);

        void appendUMod(UMod uMod);

        void showUMod(UMod uMod);

        void showAddUMod();

        void showUModConfigUi(String taskId);

        void showUModNotificationEnabled();

        void showUModNotificationDisabled();

        void showAlienUModsCleared();

        void showLoadingUModsError();

        void showNoUMods();

        void showNotificationDisabledUModsFilterLabel();

        void showNotificationEnabledUModsFilterLabel();

        void showAllFilterLabel();

        void showNotificationDisabledUMods();

        void showNotificationEnabledUMods();

        void showSuccessfullySavedMessage();

        boolean isNotificationDisabled();

        void showFilteringPopUpMenu();

        void showOpenCloseSuccess();

        void showOpenCloseFail();
    }

    interface Presenter extends BasePresenter {

        void result(int requestCode, int resultCode);

        void loadUMods(boolean forceUpdate);

        void addNewUMod();

        void openUModDetails(@NonNull UMod requestedUMod);

        void enableUModNotification(@NonNull UMod completedUMod);

        void disableUModNotification(@NonNull UMod activeUMod);

        void clearAlienUMods();

        void setFiltering(UModsFilterType requestType);

        UModsFilterType getFiltering();

        void openCloseUMod(UMod uMod);

        void requestAccess(UMod uMod);
    }
}
