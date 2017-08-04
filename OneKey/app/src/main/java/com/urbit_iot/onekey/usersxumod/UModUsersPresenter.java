package com.urbit_iot.onekey.usersxumod;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.onekey.umodconfig.UModConfigActivity;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.commands.ApproveUserCmd;
import com.urbit_iot.onekey.data.source.UModsDataSource;
import com.urbit_iot.onekey.umods.UModsFilterType;
import com.urbit_iot.onekey.umods.UModsFragment;
import com.urbit_iot.onekey.umods.domain.usecase.ClearAlienUMods;
import com.urbit_iot.onekey.umods.domain.usecase.DisableUModNotification;
import com.urbit_iot.onekey.umods.domain.usecase.EnableUModNotification;
import com.urbit_iot.onekey.umods.domain.usecase.OpenCloseUMod;
import com.urbit_iot.onekey.usersxumod.domain.usecase.ApproveUModUser;
import com.urbit_iot.onekey.usersxumod.domain.usecase.DeleteUModUser;
import com.urbit_iot.onekey.usersxumod.domain.usecase.GetUModUsers;
import com.urbit_iot.onekey.util.EspressoIdlingResource;

import java.util.List;

import javax.inject.Inject;

import rx.Subscriber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link UModsFragment}), retrieves the data and updates the
 * UI as required.
 */
public class UModUsersPresenter implements UModUsersContract.Presenter {


    private final UModUsersContract.View mUModsView;
    private final GetUModUsers getUModUsers;
    private final EnableUModNotification mEnableUModNotification;
    private final DisableUModNotification mDisableUModNotification;
    private final ClearAlienUMods mClearAlienUMods;
    private final OpenCloseUMod mOpenCloseUMod;
    private final ApproveUModUser mApproveUModUser;
    private final DeleteUModUser mDeleteUModUser;

    private UModUsersFilterType mCurrentFiltering = UModUsersFilterType.ALL_UMOD_USERS;
    private String uModUUID;

    private boolean mFirstLoad = true;

    @Inject
    public UModUsersPresenter(@NonNull UModUsersContract.View umodsView,
                              @NonNull GetUModUsers getUModUsers,
                              @NonNull EnableUModNotification enableUModNotification,
                              @NonNull DisableUModNotification disableUModNotification,
                              @NonNull ClearAlienUMods clearAlienUMods,
                              @NonNull OpenCloseUMod openCloseUMod,
                              @NonNull ApproveUModUser approveUModUser,
                              @NonNull DeleteUModUser deleteUModUser) {
        mUModsView = checkNotNull(umodsView, "tasksView cannot be null!");
        this.getUModUsers = checkNotNull(getUModUsers, "getUMod cannot be null!");
        mEnableUModNotification = checkNotNull(enableUModNotification, "enableUModNotification cannot be null!");
        mDisableUModNotification = checkNotNull(disableUModNotification, "disableUModNotification cannot be null!");
        mClearAlienUMods = checkNotNull(clearAlienUMods,
                "clearAlienUMods cannot be null!");
        mOpenCloseUMod = checkNotNull(openCloseUMod, "openCloseUMod cannot be null!");
        mApproveUModUser = checkNotNull(approveUModUser, "approveUModUser cannot be null!");
        mDeleteUModUser = checkNotNull(deleteUModUser, "deleteUModUser cannot be null!");
    }

    public void setUModUUID(String uModUUID){
        this.uModUUID = uModUUID;
    }

    public String getuModUUID(){
        return this.uModUUID;
    }

    /**
     * Method injection is used here to safely reference {@code this} after the object is created.
     * For more information, see Java Concurrency in Practice.
     */
    @Inject
    void setupListeners() {
        mUModsView.setPresenter(this);
    }

    @Override
    public void subscribe() {
        loadUModUsers(false);
    }

    @Override
    public void unsubscribe() {
        getUModUsers.unsubscribe();
        mEnableUModNotification.unsubscribe();
        mDisableUModNotification.unsubscribe();
        mClearAlienUMods.unsubscribe();
        mApproveUModUser.unsubscribe();
        mDeleteUModUser.unsubscribe();
    }

    @Override
    public void result(int requestCode, int resultCode) {
        // If a task was successfully added, show snackbar
        if (UModConfigActivity.REQUEST_ADD_TASK == requestCode
                && Activity.RESULT_OK == resultCode) {
            mUModsView.showSuccessfullySavedMessage();
        }
    }


    @Override
    public void loadUModUsers(boolean forceUpdate) {
        // Simplification for sample: a network reload will be forced on first load.
        loadUModUsers(this.uModUUID, forceUpdate || mFirstLoad, true);
        mFirstLoad = false;
    }

    /**
     * @param forceUpdate   Pass in true to refresh the data in the {@link UModsDataSource}
     * @param showLoadingUI Pass in true to display a loading icon in the UI
     */
    private void loadUModUsers(String uModUUID, boolean forceUpdate, final boolean showLoadingUI) {
        if (showLoadingUI) {
            mUModsView.setLoadingIndicator(true);
        }

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        getUModUsers.unsubscribe();
        GetUModUsers.RequestValues requestValue = new GetUModUsers.RequestValues(uModUUID,forceUpdate,
                mCurrentFiltering);
        getUModUsers.execute(requestValue, new Subscriber<GetUModUsers.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.setLoadingIndicator(false);
            }

            @Override
            public void onError(Throwable e) {
                mUModsView.showLoadingUModUsersError();
            }

            @Override
            public void onNext(GetUModUsers.ResponseValues values) {
                processTasks(values.getUModUsers());
            }
        });
    }

    private void processTasks(List<UModUser> uModUsers) {
        if (uModUsers.isEmpty()) {
            // Show a message indicating there are no uMods for that filter type.
            processEmptyTasks();
        } else {
            // Show the list of uMods
            mUModsView.showUModUsers(uModUsers);
            // Set the filter label's text.
            showFilterLabel();
        }
    }

    private void showFilterLabel() {
        switch (mCurrentFiltering) {
            case NOT_ADMINS:
                mUModsView.showNotAdminsFilterLabel();
                break;
            case ADMINS:
                mUModsView.showAdminsFilterLabel();
                break;
            default:
                mUModsView.showAllFilterLabel();
                break;
        }
    }

    private void processEmptyTasks() {
        switch (mCurrentFiltering) {
            case NOT_ADMINS:
                mUModsView.showNoActiveTasks();
                break;
            case ADMINS:
                mUModsView.showNoCompletedTasks();
                break;
            default:
                mUModsView.showNoUModUsers();
                break;
        }
    }

    @Override
    public void addNewUModUser() {
        //TODO implement user pre-approval
        //mUModsView.showAddUModUser();
    }


    @Override
    public void clearAllPendingUsers() {
        /*
        mClearAlienUMods.execute(new ClearAlienUMods.RequestValues(),
                new Subscriber<RxUseCase.NoResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.showAllPendingUModUsersCleared();
                loadUModUsers(false, false);
            }

            @Override
            public void onError(Throwable e) {
                mUModsView.showLoadingUModUsersError();
            }

            @Override
            public void onNext(RxUseCase.NoResponseValues noResponseValues) {

            }
        })
        */
    }

    /**
     * Sets the current task filtering type.
     *
     * @param requestType Can be {@link UModsFilterType#ALL_UMODS},
     *                    {@link UModsFilterType#NOTIF_EN_UMODS}, or
     *                    {@link UModsFilterType#NOTIF_DIS_UMODS}
     */
    @Override
    public void setFiltering(UModUsersFilterType requestType) {
        mCurrentFiltering = requestType;
    }

    @Override
    public UModUsersFilterType getFiltering() {
        return mCurrentFiltering;
    }

    @Override
    public void approveUser(UModUser uModUser) {
        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        mApproveUModUser.unsubscribe();
        ApproveUModUser.RequestValues requestValue = new ApproveUModUser.RequestValues(uModUser);
        mApproveUModUser.execute(requestValue, new Subscriber<ApproveUModUser.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.setLoadingIndicator(false);
            }

            @Override
            public void onError(Throwable e) {
                mUModsView.showLoadingUModUsersError();
            }

            @Override
            public void onNext(ApproveUModUser.ResponseValues responseValues) {
                ApproveUserCmd.Response response = responseValues.getResponse();
                Log.d("ModsPresenter", "Command is " + response.getCommandCode());
                mUModsView.showUserApprovalSuccess();
            }
        });
    }

    @Override
    public void deleteUser(UModUser uModUser) {
        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        mApproveUModUser.unsubscribe();
        ApproveUModUser.RequestValues requestValue = new ApproveUModUser.RequestValues(uModUser);
        mApproveUModUser.execute(requestValue, new Subscriber<ApproveUModUser.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.setLoadingIndicator(false);
            }

            @Override
            public void onError(Throwable e) {
                mUModsView.showLoadingUModUsersError();
            }

            @Override
            public void onNext(ApproveUModUser.ResponseValues responseValues) {
                ApproveUserCmd.Response response = responseValues.getResponse();
                Log.d("ModsPresenter", "Command is " + response.getCommandCode());
                mUModsView.showUserApprovalSuccess();
            }
        });
    }

}
