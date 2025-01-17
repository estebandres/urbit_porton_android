package com.urbit_iot.porton.umodsnotification;

import com.urbit_iot.porton.BasePresenter;
import com.urbit_iot.porton.BaseView;

/**
 * Created by steve-urbit on 02/08/17.
 * This specifies the contract between the view and the presenter.
 */

public interface UModsNotifContract {
    interface View extends BaseView<Presenter> {

        void showUnconnectedPhone();

        void showNoUModsFound();

        void showSingleUModControl();

        void showUModSelectAndControl();

        void showSelectionControls();

        void hideSelectionControls();

        void showRequestAccessView(String uModUUID, String uModAlias);

        void showTriggerView(String uModUUID, String uModAlias);

        void setTitleText(String newTitle);

        void setSecondaryText(String newText);

        void showUnlockedView();

        void showLockedView();

        void enableOperationButton();

        void disableOperationButton();

        boolean isWiFiConnected();

        void showTriggerProgress();

        void showAccessRequestProgress();

        void hideProgressView();

        void toggleLockState();

        boolean getLockState();

        void setLockState(boolean lockState);

        void showLoadProgress();

        void showAllUModsAreNotifDisabled();

        void showNoConfiguredUMods();
    }

    interface Presenter extends BasePresenter {

        void loadUMods(boolean forceUpdate);

        void triggerUMod(String uModUUID);

        void requestAccess(String uModUUID);

        void lockUModOperation();


        void previousUMod();

        void nextUMod();

        void wifiIsOn();

        void wifiIsOff();
    }
}
