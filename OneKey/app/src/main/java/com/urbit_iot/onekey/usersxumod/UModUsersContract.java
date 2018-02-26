package com.urbit_iot.onekey.usersxumod;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.BasePresenter;
import com.urbit_iot.onekey.BaseView;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.umods.UModsFilterType;

import java.util.List;

/**
 * Created by steve-urbit on 02/08/17.
 * This specifies the contract between the view and the presenter.
 */

public interface UModUsersContract {
    interface View extends BaseView<Presenter> {

        void setLoadingIndicator(boolean active);

        void showUModUsers(List<UModUserViewModel> uModUsers);

        void showAddUModUser();

        //void showUModConfigUi(String taskId);

        //void showUModNotificationEnabled();

        //void showUModNotificationDisabled();

        void showAllPendingUModUsersCleared();

        void showLoadingUModUsersError();

        void showNoUModUsers();

        void showNotAdminsFilterLabel();

        void showAdminsFilterLabel();

        void showAllFilterLabel();

        void showNoActiveTasks();

        void showNoCompletedTasks();

        void showSuccessfullySavedMessage();

        boolean isActive();

        void showFilteringPopUpMenu();

        void showUserDeletionSuccess();

        void showUserDeletionFail();

        void showUserApprovalSuccess();

        void showUserApprovalFail();

        String getContactNameFromPhoneNumber(String phoneNumber);

        void showProgressBar();

        void hideProgressBar();
    }

    interface Presenter extends BasePresenter {

        void result(int requestCode, int resultCode);

        void loadUModUsers(boolean forceUpdate);

        void addNewUModUser();

        //void openUModConfig(@NonNull UMod requestedUMod);

        //void enableUModNotification(@NonNull UMod completedUMod);

        //void disableUModNotification(@NonNull UMod activeUMod);

        //TODO it could be clearTemporalUsers or clearAllPendingUsers or both
        void clearAllPendingUsers();

        void setFiltering(UModUsersFilterType requestType);

        UModUsersFilterType getFiltering();

        void authorizeUser(String uModUserPhoneNum);

        void deleteUser(String uModUserPhoneNum);

        void setContactsAccessGranted(boolean contactsAccessGranted);

        void upDownAdminLevel(String uModUserPhoneNum, boolean toAdmin);
    }
}
