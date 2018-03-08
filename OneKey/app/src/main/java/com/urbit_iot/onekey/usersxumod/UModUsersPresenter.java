package com.urbit_iot.onekey.usersxumod;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.onekey.data.rpc.APIUserType;
import com.urbit_iot.onekey.data.rpc.GetUsersRPC;
import com.urbit_iot.onekey.umodconfig.UModConfigActivity;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.source.UModsDataSource;
import com.urbit_iot.onekey.umods.UModsFilterType;
import com.urbit_iot.onekey.umods.UModsFragment;
import com.urbit_iot.onekey.usersxumod.domain.usecase.UpdateUserType;
import com.urbit_iot.onekey.usersxumod.domain.usecase.DeleteUModUser;
import com.urbit_iot.onekey.usersxumod.domain.usecase.GetUModUsers;
import com.urbit_iot.onekey.usersxumod.domain.usecase.UpDownAdminLevel;
import com.urbit_iot.onekey.util.EspressoIdlingResource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Subscriber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link UModsFragment}), retrieves the data and updates the
 * UI as required.
 */
public class UModUsersPresenter implements UModUsersContract.Presenter {
    /*
todo inmediato
        1 repasar API vs RPCs:  api revisar args y results
    2 agregar errores en los rpc.
    3 agregar manejo de errores en los casos de uso
*/

    private final UModUsersContract.View mUModsView;
    private final GetUModUsers getUModUsers;
    private final UpdateUserType mUpdateUserType;
    private final DeleteUModUser mDeleteUModUser;
    private final UpDownAdminLevel upDownAdminLevel;

    private UModUsersFilterType mCurrentFiltering = UModUsersFilterType.ALL_UMOD_USERS;
    private String uModUUID;

    private boolean mFirstLoad = true;

    private boolean contactsAccessGranted = false;

    @Inject
    public UModUsersPresenter(@NonNull UModUsersContract.View umodsView,
                              @NonNull GetUModUsers getUModUsers,
                              @NonNull UpdateUserType updateUserType,
                              @NonNull DeleteUModUser deleteUModUser,
                              @NonNull UpDownAdminLevel upDownAdminLevel) {
        mUModsView = checkNotNull(umodsView, "tasksView cannot be null!");
        this.getUModUsers = checkNotNull(getUModUsers, "getUModUUID cannot be null!");
        mUpdateUserType = checkNotNull(updateUserType, "updateUserType cannot be null!");
        mDeleteUModUser = checkNotNull(deleteUModUser, "deleteUModUser cannot be null!");
        this.upDownAdminLevel = checkNotNull(upDownAdminLevel, "upDownAdminLevel cannot be null!");
    }

    @Override
    public void setContactsAccessGranted(boolean contactsAccessGranted) {
        this.contactsAccessGranted = contactsAccessGranted;
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
        mUpdateUserType.unsubscribe();
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
        GetUModUsers.RequestValues requestValue = new GetUModUsers.RequestValues(uModUUID);//,forceUpdate,mCurrentFiltering
        getUModUsers.execute(requestValue, new Subscriber<GetUModUsers.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.setLoadingIndicator(false);
            }

            @Override
            public void onError(Throwable e) {
                Log.e("umods_pr", e.getMessage());
                mUModsView.showLoadingUModUsersError();
            }

            @Override
            public void onNext(GetUModUsers.ResponseValues values) {
                Log.d("umod-users_pr", values.getResult().toString());
                processUModUsersVM(mapResponseToViewModels(values.getResult()));
            }
        });
    }

    private List<UModUserViewModel> mapResponseToViewModels(GetUsersRPC.Result result){
        List<UModUserViewModel> userVMsList = new ArrayList<>();
        String itemMainText;
        String buttonText;
        boolean checkboxChecked;
        boolean checkboxVisible;

        for(GetUsersRPC.UserResult userResult : result.getUsers()){

            if(this.contactsAccessGranted){
                itemMainText = mUModsView.getContactNameFromPhoneNumber("+" + userResult.getUserName());
            } else {
                itemMainText = userResult.getUserName();
            }

            switch (userResult.getUserType()){
                case Admin:
                    buttonText = "DEL";
                    checkboxChecked = true;
                    checkboxVisible = true;
                    break;
                case User:
                    buttonText = "DEL";
                    checkboxChecked = false;
                    checkboxVisible = true;
                    break;
                case Guest:
                    buttonText = "AUTH";
                    checkboxChecked = false;
                    checkboxVisible = false;
                    break;
                case NotUser:
                    buttonText = "BUG";
                    checkboxChecked = false;
                    checkboxVisible = false;
                    break;
                default:
                    buttonText = "DEF";
                    checkboxChecked = false;
                    checkboxVisible = false;
            }

            userVMsList.add(
                    new UModUserViewModel(userResult,
                            this,
                            itemMainText,
                            buttonText,
                            checkboxChecked,
                            checkboxVisible) {
                @Override
                public void onButtonClicked() {
                    switch (getUserResult().getUserType()) {
                        case Admin:
                            getPresenter().deleteUser(getUserResult().getUserName());
                            break;
                        case User:
                            getPresenter().deleteUser(getUserResult().getUserName());
                            break;
                        case Guest:
                            getPresenter().authorizeUser(getUserResult().getUserName());
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onCheckBoxClicked(Boolean cbChecked) {
                    switch (getUserResult().getUserType()) {
                        case Admin:
                            getPresenter().upDownAdminLevel(getUserResult().getUserName(),false);
                            break;
                        case User:
                            getPresenter().upDownAdminLevel(getUserResult().getUserName(),true);
                            break;
                        case Guest:
                            break;
                        default:
                            break;
                    }
                }
            });
        }
        return userVMsList;
    }

    private void tryGetFriendlyAliasForUsers(List<UModUser> uModUsers){
        for(UModUser uModUser : uModUsers){
            if(this.contactsAccessGranted){
                uModUser.setUserAlias(mUModsView.getContactNameFromPhoneNumber(uModUser.getPhoneNumber()));
            } else {
                uModUser.setUserAlias(uModUser.getPhoneNumber());
            }
        }
    }

    private void processUModUsersVM(List<UModUserViewModel> uModUsersVMs) {
        if (uModUsersVMs.isEmpty()) {
            // Show a message indicating there are no uMods for that filter type.
            processNoUsersFound();
        } else {
            // Show the list of uMods
            mUModsView.showUModUsers(uModUsersVMs);
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

    private void processNoUsersFound() {
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
    public void authorizeUser(String uModUserPhoneNum) {
        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        mUModsView.showProgressBar();
        mUpdateUserType.unsubscribe();
        UpdateUserType.RequestValues requestValue =
                new UpdateUserType.RequestValues(
                        uModUserPhoneNum,
                        this.uModUUID,
                        APIUserType.User);
        mUpdateUserType.execute(requestValue, new Subscriber<UpdateUserType.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.hideProgressBar();
            }

            @Override
            public void onError(Throwable e) {
                mUModsView.showLoadingUModUsersError();
            }

            @Override
            public void onNext(UpdateUserType.ResponseValues responseValues) {
                Log.d("um_usrs_pr", "RPC is " + responseValues.getResult().toString());
                mUModsView.showUserApprovalSuccess();
            }
        });
    }

    @Override
    public void deleteUser(String uModUserPhoneNum) {
        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        mUModsView.showProgressBar();
        mDeleteUModUser.unsubscribe();
        DeleteUModUser.RequestValues requestValue = new DeleteUModUser.RequestValues(uModUserPhoneNum, uModUUID);
        mDeleteUModUser.execute(requestValue, new Subscriber<DeleteUModUser.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.hideProgressBar();
            }

            @Override
            public void onError(Throwable e) {
                Log.d("um_usrs_pr", "Delete Fail: " + e.getMessage());
                mUModsView.showLoadingUModUsersError();
            }

            @Override
            public void onNext(DeleteUModUser.ResponseValues responseValues) {
                Log.d("um_usrs_pr", "RPC is " + responseValues.getResult().toString());
                mUModsView.showUserApprovalSuccess();
            }
        });
    }

    @Override
    public void upDownAdminLevel(String userPhoneNum, boolean toAdmin) {
        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        mUModsView.showProgressBar();
        this.mUpdateUserType.unsubscribe();
        UpdateUserType.RequestValues requestValue =
                new UpdateUserType.RequestValues(
                        userPhoneNum,
                        this.uModUUID,
                        toAdmin?APIUserType.Admin:APIUserType.User);
        this.mUpdateUserType.execute(requestValue, new Subscriber<UpdateUserType.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.hideProgressBar();
            }

            @Override
            public void onError(Throwable e) {
                Log.e("um_usrs_pr", e.getMessage());
                mUModsView.showLoadingUModUsersError();
            }

            @Override
            public void onNext(UpdateUserType.ResponseValues responseValues) {
                Log.d("um_usrs_pr", "RPC is " + responseValues.getResult().toString());
                mUModsView.showUserApprovalSuccess();
            }
        });
    }

}
