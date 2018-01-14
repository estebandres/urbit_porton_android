package com.urbit_iot.onekey.umods;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.RPC;
import com.urbit_iot.onekey.umodconfig.UModConfigActivity;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsDataSource;
import com.urbit_iot.onekey.umods.domain.usecase.ClearAlienUMods;
import com.urbit_iot.onekey.umods.domain.usecase.GetUMods;
import com.urbit_iot.onekey.umods.domain.usecase.GetUModsOneByOne;
import com.urbit_iot.onekey.umods.domain.usecase.RequestAccess;
import com.urbit_iot.onekey.umods.domain.usecase.SetOngoingNotificationStatus;
import com.urbit_iot.onekey.umods.domain.usecase.TriggerUMod;
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
public class UModsPresenter implements UModsContract.Presenter {


    private final UModsContract.View mUModsView;
    private final GetUMods mGetUMods;
    private final ClearAlienUMods mClearAlienUMods;
    private final TriggerUMod mTriggerUMod;
    private final GetUModsOneByOne mGetUModsOneByOne;
    private final SetOngoingNotificationStatus mSetOngoingNotificationStatus;
    private final RequestAccess mRequestAccess;

    private UModsFilterType mCurrentFiltering = UModsFilterType.ALL_UMODS;

    private boolean mFirstLoad = true;

    @Inject
    public UModsPresenter(@NonNull UModsContract.View umodsView,
                          @NonNull GetUMods getUMods,
                          @NonNull GetUModsOneByOne getUModsOneByOne,
                          @NonNull SetOngoingNotificationStatus setOngoingNotificationStatus,
                          @NonNull ClearAlienUMods clearAlienUMods,
                          @NonNull TriggerUMod triggerUMod,
                          @NonNull RequestAccess requestAccess) {
        mUModsView = checkNotNull(umodsView, "tasksView cannot be null!");
        mGetUModsOneByOne = checkNotNull(getUModsOneByOne, "getUModsOneByOne cannot be null!");
        mGetUMods = checkNotNull(getUMods, "getUModUUID cannot be null!");
        mSetOngoingNotificationStatus = checkNotNull(setOngoingNotificationStatus, "setOngoingNotificationStatus cannot be null!");
        mClearAlienUMods = checkNotNull(clearAlienUMods,
                "clearAlienUMods cannot be null!");
        mTriggerUMod = checkNotNull(triggerUMod, "triggerUMod cannot be null!");
        mRequestAccess = checkNotNull(requestAccess, "requestAccess cannot be null!");
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
        loadUMods(false);
    }

    @Override
    public void unsubscribe() {
        mGetUMods.unsubscribe();
        mSetOngoingNotificationStatus.unsubscribe();
        mClearAlienUMods.unsubscribe();
        mGetUModsOneByOne.unsubscribe();
        mRequestAccess.unsubscribe();
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
    public void loadUMods(boolean forceUpdate) {
        // Simplification for sample: a network reload will be forced on first load.
        loadUModsOneByOne(forceUpdate || mFirstLoad, true);
        mFirstLoad = false;
    }

    /**
     * @param forceUpdate   Pass in true to refresh the data in the {@link UModsDataSource}
     * @param showLoadingUI Pass in true to display a loading icon in the UI
     */
    private void OLDloadUMods(boolean forceUpdate, final boolean showLoadingUI) {
        if (showLoadingUI) {
            mUModsView.setLoadingIndicator(true);
        }

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        mGetUMods.unsubscribe();
        GetUMods.RequestValues requestValue = new GetUMods.RequestValues(forceUpdate,
                mCurrentFiltering);
        mGetUMods.execute(requestValue, new Subscriber<GetUMods.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.setLoadingIndicator(false);
            }

            @Override
            public void onError(Throwable e) {
                Log.e("STV_umods_pr", e.getMessage());
                mUModsView.showLoadingUModsError();
            }

            @Override
            public void onNext(GetUMods.ResponseValues values) {
                Log.d("STEVE_umods_presenter", values.getUMods().toString());
                //processTasks(values.getUMods());
                List<UModViewModel> models = new ArrayList<>();
                for (UMod uMod : values.getUMods()){
                    models.add(createViewModel(uMod));
                }
                mUModsView.showUMods(models);
            }
        });
    }

    private void loadUMods(boolean forceUpdate, final boolean showLoadingUI) {
        loadUModsOneByOne(forceUpdate,showLoadingUI);
    }

    /**
     * @param forceUpdate   Pass in true to refresh the data in the {@link UModsDataSource}
     * @param showLoadingUI Pass in true to display a loading icon in the UI
     */
    private void loadUModsOneByOne(boolean forceUpdate, final boolean showLoadingUI) {
        if (showLoadingUI) {
            mUModsView.setLoadingIndicator(true);
        }

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        GetUModsOneByOne.RequestValues requestValue = new GetUModsOneByOne.RequestValues(forceUpdate,
                mCurrentFiltering);
        mGetUModsOneByOne.execute(requestValue, new Subscriber<GetUModsOneByOne.ResponseValues>() {

            @Override
            public void onCompleted() {
                mUModsView.setLoadingIndicator(false);
            }

            @Override
            public void onError(Throwable e) {
                Log.e("umods_pr", e.getMessage());
                mUModsView.setLoadingIndicator(false);
                mUModsView.showLoadingUModsError();
            }

            @Override
            public void onNext(GetUModsOneByOne.ResponseValues values) {
                Log.d("umods_pr", values.getUMod().toString());
                mUModsView.showUMod(createViewModel(values.getUMod()));
            }
        });
    }

    //TODO peer review view model creation rules
    private UModViewModel createViewModel(final UMod uMod){
        final String uModUUID;
        String itemMainText;
        String itemLowerText;
        boolean checkboxChecked;
        boolean checkboxVisible;
        String buttonText;
        boolean buttonVisible;
        boolean itemOnClickListenerEnabled;

        uModUUID = uMod.getUUID();

        if(uMod.getAlias()!=null && !uMod.getAlias().isEmpty()){
            itemMainText = uMod.getAlias();
        } else {
            itemMainText = uModUUID;
        }

        if (uMod.belongsToAppUser() && uMod.getState() != UMod.State.AP_MODE){
            checkboxVisible = true;
        } else {
            checkboxVisible = false;
        }

        //Default values
        buttonText = "OFF";
        buttonVisible = false;

        if (uMod.getuModSource() == UMod.UModSource.LAN_SCAN){
            //shows action button only when online and is in station mode.
            if (uMod.getState() == UMod.State.STATION_MODE){
                buttonVisible = true;
                //TODO add button text alternatives as String resource or GlobalConstants??.
                if(uMod.belongsToAppUser()){
                    buttonText = "TRIG";
                } else {
                    buttonText = "ASK";
                }
            } else {
                buttonVisible = false;
                buttonText = "NON";
            }
        }

        checkboxChecked = uMod.isOngoingNotificationEnabled();


        //Default until all states are defined i.e. what about BLE_MODE and OTA_UPDATE??
        itemOnClickListenerEnabled = false;
        itemLowerText = "UNKNOWN";

        if (uMod.getState()== UMod.State.AP_MODE
                && uMod.getuModSource() == UMod.UModSource.LAN_SCAN){
            itemOnClickListenerEnabled = true;
            itemLowerText = "ONLINE";
        }
        if(uMod.getState()== UMod.State.STATION_MODE
                && uMod.getuModSource() == UMod.UModSource.LAN_SCAN){
            itemOnClickListenerEnabled = true;
            itemLowerText = "ONLINE";
        }

        if(uMod.getuModSource() != UMod.UModSource.LAN_SCAN){
            itemOnClickListenerEnabled = false;
            itemLowerText = "OFFLINE";
        }

        return new UModViewModel(uModUUID, this, itemMainText, itemLowerText,
                checkboxChecked, checkboxVisible, buttonText, buttonVisible, itemOnClickListenerEnabled) {
            @Override
            public void onButtonClicked() {
                if(uMod.belongsToAppUser()){
                    this.getPresenter().triggerUMod(uModUUID);
                } else {
                    if (uMod.getState() == UMod.State.STATION_MODE){
                        this.getPresenter().requestAccess(uModUUID);
                    }
                }
            }

            @Override
            public void onCheckBoxClicked(Boolean cbChecked) {
                this.getPresenter().setNotificationStatus(uModUUID, cbChecked);
            }

            @Override
            public void onItemClicked() {
                this.getPresenter().openUModConfig(uModUUID);
            }
        };
    }

    private void processTasks(List<UModViewModel> uMods) {
        if (uMods.isEmpty()) {
            // Show a message indicating there are no uMods for that filter type.
            processEmptyTasks();
        } else {
            // Show the list of uMods
            mUModsView.showUMods(uMods);
            // Set the filter label's text.
            showFilterLabel();
        }
    }

    private void showFilterLabel() {
        switch (mCurrentFiltering) {
            case NOTIF_DIS_UMODS:
                mUModsView.showNotificationDisabledUModsFilterLabel();
                break;
            case NOTIF_EN_UMODS:
                mUModsView.showNotificationEnabledUModsFilterLabel();
                break;
            default:
                mUModsView.showAllFilterLabel();
                break;
        }
    }

    private void processEmptyTasks() {
        switch (mCurrentFiltering) {
            case NOTIF_DIS_UMODS:
                mUModsView.showNotificationDisabledUMods();
                break;
            case NOTIF_EN_UMODS:
                mUModsView.showNotificationEnabledUMods();
                break;
            default:
                mUModsView.showNoUMods();
                break;
        }
    }

    @Override
    public void addNewUMod() {
        mUModsView.showAddUMod();
    }

    @Override
    public void openUModConfig(@NonNull String uModUUID) {
        checkNotNull(uModUUID, "uModUUID cannot be null!");
        mUModsView.showUModConfigUi(uModUUID);
    }

    @Override
    public void setNotificationStatus(@NonNull String uModUUID, final Boolean notificationEnabled) {
        checkNotNull(uModUUID, "umoduuid cannot be null!");
        mSetOngoingNotificationStatus.unsubscribe();
        mSetOngoingNotificationStatus.execute(new SetOngoingNotificationStatus.RequestValues(uModUUID, notificationEnabled),
                new Subscriber<RxUseCase.NoResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.showOngoingNotifStatusChanged(notificationEnabled);
                loadUMods(false, false);
            }

            @Override
            public void onError(Throwable e) {
                mUModsView.showLoadingUModsError();
            }

            @Override
            public void onNext(RxUseCase.NoResponseValues noResponseValues) {

            }
        });
    }

    @Override
    public void clearAlienUMods() {
        mClearAlienUMods.execute(new ClearAlienUMods.RequestValues(),
                new Subscriber<RxUseCase.NoResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.showAlienUModsCleared();
                loadUMods(false, false);
            }

            @Override
            public void onError(Throwable e) {
                mUModsView.showLoadingUModsError();
            }

            @Override
            public void onNext(RxUseCase.NoResponseValues noResponseValues) {

            }
        });
    }

    /**
     * Sets the current task filtering type.
     *
     * @param requestType Can be {@link UModsFilterType#ALL_UMODS},
     *                    {@link UModsFilterType#NOTIF_EN_UMODS}, or
     *                    {@link UModsFilterType#NOTIF_DIS_UMODS}
     */
    @Override
    public void setFiltering(UModsFilterType requestType) {
        mCurrentFiltering = requestType;
    }

    @Override
    public UModsFilterType getFiltering() {
        return mCurrentFiltering;
    }

    @Override
    public void triggerUMod(final String uModUUID) {

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        TriggerUMod.RequestValues requestValue = new TriggerUMod.RequestValues(uModUUID);
        mTriggerUMod.execute(requestValue, new Subscriber<TriggerUMod.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.setLoadingIndicator(false);
            }

            @Override
            public void onError(Throwable e) {
                mUModsView.showOpenCloseFail();
                loadUMods(true);
            }

            @Override
            public void onNext(TriggerUMod.ResponseValues responseValues) {
                //TODO make a method that proceses HTTP error codes for the given RPC.
                TriggerRPC.Response response = responseValues.getResponse();
                RPC.ResponseError responseError = response.getResponseError();
                if (responseError != null
                        && responseError.getErrorCode() != null
                        && responseError.getErrorCode() != 0){
                    Log.d("umods_pr", "Error Code: " + responseError.getErrorCode()
                            + "\nError Message: " + responseError.getErrorMessage());
                    mUModsView.showOpenCloseFail();
                    mUModsView.makeUModViewModelActionButtonVisible(uModUUID);
                } else {
                    Log.d("ModsPresenter", "RPC is " + response.toString());
                    mUModsView.showOpenCloseSuccess();
                }
                //If 500 then updateAppUserLevelOnUMod
                //TODO after a successful answer enable action button on view
            }
        });
    }

    @Override
    public void requestAccess(String uModUUID) {

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice
        RequestAccess.RequestValues requestValues = new RequestAccess.RequestValues(uModUUID);
        mRequestAccess.execute(requestValues, new Subscriber<RequestAccess.ResponseValues>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.e("umods_pr", "Request Access Failed: " + e.getMessage());
                mUModsView.showRequestAccessFailedMessage();
            }

            @Override
            public void onNext(RequestAccess.ResponseValues responseValues) {
                CreateUserRPC.Response successResponse = responseValues.getResponse();
                RPC.ResponseError responseError = successResponse.getResponseError();
                if (responseError != null
                        && responseError.getErrorCode() != null
                        && responseError.getErrorCode() != 0) {
                    Log.d("umods_pr", "Error Code: " + responseError.getErrorCode()
                            + "\nError Message: " + responseError.getErrorMessage());
                    mUModsView.showRequestAccessFailedMessage();
                } else {
                    Log.d("umods_pr", successResponse.toString());
                    mUModsView.showRequestAccessCompletedMessage();
                }
            }
        });
    }
}
