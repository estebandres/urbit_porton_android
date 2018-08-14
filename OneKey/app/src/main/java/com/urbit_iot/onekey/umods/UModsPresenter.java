package com.urbit_iot.onekey.umods;

import android.support.annotation.NonNull;
import android.util.Log;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.source.UModsDataSource;
import com.urbit_iot.onekey.umods.domain.usecase.ClearAlienUMods;
import com.urbit_iot.onekey.umods.domain.usecase.GetUMods;
import com.urbit_iot.onekey.umods.domain.usecase.GetUModsOneByOne;
import com.urbit_iot.onekey.umods.domain.usecase.RequestAccess;
import com.urbit_iot.onekey.umods.domain.usecase.SetOngoingNotificationStatus;
import com.urbit_iot.onekey.umods.domain.usecase.TriggerUMod;
import com.urbit_iot.onekey.util.EspressoIdlingResource;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.IntegerContainer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Subscriber;
import timber.log.Timber;

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
    private RxSharedPreferences rxSharedPreferences;
    /*
    @NonNull
    private final RetrofitUtils mRetrofitUtils;
    */

    private UModsFilterType mCurrentFiltering = UModsFilterType.ALL_UMODS;

    private boolean mFirstLoad = true;

    @Inject
    public UModsPresenter(@NonNull UModsContract.View umodsView,
                          //@NonNull RetrofitUtils mRetrofitUtils,
                          @NonNull GetUMods getUMods,
                          @NonNull GetUModsOneByOne getUModsOneByOne,
                          @NonNull SetOngoingNotificationStatus setOngoingNotificationStatus,
                          @NonNull ClearAlienUMods clearAlienUMods,
                          @NonNull TriggerUMod triggerUMod,
                          @NonNull RequestAccess requestAccess,
                          @NonNull RxSharedPreferences rxSharedPreferences) {
        mUModsView = checkNotNull(umodsView, "tasksView cannot be null!");
        mGetUModsOneByOne = checkNotNull(getUModsOneByOne, "getUModsOneByOne cannot be null!");
        mGetUMods = checkNotNull(getUMods, "getUModUUID cannot be null!");
        mSetOngoingNotificationStatus = checkNotNull(setOngoingNotificationStatus, "setOngoingNotificationStatus cannot be null!");
        mClearAlienUMods = checkNotNull(clearAlienUMods,
                "clearAlienUMods cannot be null!");
        mTriggerUMod = checkNotNull(triggerUMod, "userTriggerUMod cannot be null!");
        mRequestAccess = checkNotNull(requestAccess, "requestAccess cannot be null!");
        //this.mRetrofitUtils = mRetrofitUtils;
        this.rxSharedPreferences = rxSharedPreferences;
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
        mUModsView.clearAllItems();
        loadUMods(false);
        Preference<Boolean> ongoingNotificationPref = rxSharedPreferences.getBoolean(GlobalConstants.ONGOING_NOTIFICATION_STATE_KEY);
        if (ongoingNotificationPref.isSet()){
            if (ongoingNotificationPref.get()){
                mUModsView.startOngoingNotification();
            }
        } else {
            mUModsView.startOngoingNotification();
            ongoingNotificationPref.set(true);
        }
    }

    @Override
    public void unsubscribe() {
        mGetUMods.unsubscribe();
        mClearAlienUMods.unsubscribe();
        mTriggerUMod.unsubscribe();
        mGetUModsOneByOne.unsubscribe();
        mSetOngoingNotificationStatus.unsubscribe();
        mRequestAccess.unsubscribe();
    }

    @Override
    public void loadUMods(boolean forceUpdate) {
        // Simplification for sample: a network reload will be forced on first load.
        Log.d("umods_pr","Forced UPDATE: " + forceUpdate + "  FIRST LOAD: " + mFirstLoad);
        loadUModsOneByOne(forceUpdate || mFirstLoad, true);
        //loadUModsOneByOne(forceUpdate, true);
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
                Log.e("STV_umods_pr", "" + e.getMessage());
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

        /*
        if (forceUpdate){
            this.mUModsView.clearAllItems();
        }
        */

        this.mUModsView.clearAllItems();
        final IntegerContainer onNextCount = new IntegerContainer(0);

        GetUModsOneByOne.RequestValues requestValue = new GetUModsOneByOne.RequestValues(forceUpdate,
                mCurrentFiltering);
        mGetUModsOneByOne.execute(requestValue, new Subscriber<GetUModsOneByOne.ResponseValues>() {

            @Override
            public void onCompleted() {
                mUModsView.setLoadingIndicator(false);
                if (onNextCount.getValue() <= 0) {
                    Log.e("umods_pr", "getUModsOnexOne didn't retreive any result: " + onNextCount);
                    mUModsView.showNoUMods();
                }
                if (forceUpdate){
                    mUModsView.refreshOngoingNotification();
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e("umods_pr", "" + e.getMessage());
                mUModsView.setLoadingIndicator(false);
                mUModsView.showLoadingUModsError();
            }

            @Override
            public void onNext(GetUModsOneByOne.ResponseValues values) {
                Log.d("umods_pr", values.getUMod().toString());
                mUModsView.showUMod(createViewModel(values.getUMod()));
                onNextCount.plusOne();
            }
        });
    }

    //TODO peer review view model creation rules
    private UModViewModel createViewModel(UMod uMod){
        Log.d("umods_pres", uMod.hashCode() + "\n" + uMod.toString());
        final String uModUUID;
        String itemMainText;
        String itemLowerText;
        boolean checkboxChecked;
        boolean checkboxVisible;
        String sliderText;
        boolean sliderVisible;
        boolean itemOnClickListenerEnabled;
        boolean sliderEnabled;
        UModsFragment.UModViewModelColors lowerTextColor, sliderBackgroundColor, sliderTextColor;

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
        sliderText = null;
        sliderVisible = false;
        sliderEnabled = false;
        sliderBackgroundColor = null;
        sliderTextColor = null;

        /*
        if (uMod.getuModSource() == UMod.UModSource.LAN_SCAN
                || uMod.getuModSource() == UMod.UModSource.MQTT_SCAN) {//LAN_SCAN means online but can be dnssd/ap/ble discovery
            //shows action button only when online and is in station mode.
        }
        */
        if (uMod.getState() == UMod.State.STATION_MODE) {
            sliderEnabled = true;
            switch (uMod.getAppUserLevel()) {
                case PENDING:
                    sliderText = GlobalConstants.PENDING_SLIDER_TEXT;
                    sliderTextColor = UModsFragment.UModViewModelColors.ACCESS_REQUEST_SLIDER_TEXT;
                    sliderBackgroundColor = UModsFragment.UModViewModelColors.ACCESS_REQUEST_SLIDER_BACKGROUND;
                    sliderVisible = true;
                    sliderEnabled = false;
                    break;
                case INVITED:
                case AUTHORIZED:
                    sliderText = GlobalConstants.TRIGGER_SLIDER_TEXT;
                    sliderBackgroundColor = UModsFragment.UModViewModelColors.TRIGGER_SLIDER_BACKGROUND;
                    sliderTextColor = UModsFragment.UModViewModelColors.TRIGGER_SLIDER_TEXT;
                    sliderVisible = true;
                    break;
                case ADMINISTRATOR:
                    sliderText = GlobalConstants.TRIGGER_SLIDER_TEXT;
                    sliderBackgroundColor = UModsFragment.UModViewModelColors.TRIGGER_SLIDER_BACKGROUND;
                    sliderTextColor = UModsFragment.UModViewModelColors.TRIGGER_SLIDER_TEXT;
                    sliderVisible = true;
                    break;
                case UNAUTHORIZED:
                    sliderText = GlobalConstants.REQUEST_ACCESS_SLIDER_TEXT;
                    sliderBackgroundColor = UModsFragment.UModViewModelColors.ACCESS_REQUEST_SLIDER_BACKGROUND;
                    sliderTextColor = UModsFragment.UModViewModelColors.ACCESS_REQUEST_SLIDER_TEXT;
                    sliderVisible = true;
                    break;
                default:
                    sliderText = "DEFAULT_NON";
                    sliderBackgroundColor = UModsFragment.UModViewModelColors.STORED_BLUE;
                    sliderTextColor = UModsFragment.UModViewModelColors.TRIGGER_SLIDER_TEXT;
                    sliderVisible = false;
                    break;
            }
        }

        checkboxChecked = uMod.isOngoingNotificationEnabled();

        if (uMod.getuModSource() == UMod.UModSource.LAN_SCAN
                || uMod.getuModSource() == UMod.UModSource.MQTT_SCAN){
            itemLowerText = GlobalConstants.ONLINE_LOWER_TEXT;
            lowerTextColor = UModsFragment.UModViewModelColors.ONLINE_GREEN;
        } else {
            itemLowerText = GlobalConstants.STORED_LOWER_TEXT;
            lowerTextColor = UModsFragment.UModViewModelColors.STORED_BLUE;
        }

        //TODO Default until all states are defined i.e. what about BLE_MODE and OTA_UPDATE??

        switch (uMod.getAppUserLevel()){
            case ADMINISTRATOR:
                itemOnClickListenerEnabled = true;
                break;
            case INVITED:
            case AUTHORIZED:
                itemOnClickListenerEnabled = true;
                break;
            case PENDING:
                itemOnClickListenerEnabled = false;
                break;
            case UNAUTHORIZED:
                itemOnClickListenerEnabled = false;
                break;
            default:
                itemOnClickListenerEnabled = false;
                break;
        }

        if (uMod.getState() == UMod.State.AP_MODE){
            itemOnClickListenerEnabled = true;
        }

        return new UModViewModel(uMod, uModUUID, this, itemMainText, itemLowerText,
                checkboxChecked, checkboxVisible, sliderText, sliderVisible, sliderEnabled, itemOnClickListenerEnabled,
                lowerTextColor, sliderBackgroundColor, sliderTextColor) {
            @Override
            public void onSlideCompleted() {
                //TODO this code shouldnt depend on external objects.
                if(this.getuMod().belongsToAppUser()){
                    this.getPresenter().triggerUMod(getuModUUID());
                } else {
                    //TODO this code shouldnt depend on external objects.
                    if (this.getuMod().getState() == UMod.State.STATION_MODE){
                        this.getPresenter().requestAccess(getuModUUID());
                    }
                }
            }

            @Override
            public void onButtonToggled(Boolean toggleState) {
                this.getPresenter().setNotificationStatus(getuModUUID(), toggleState);
            }

            @Override
            public void onItemClicked() {
                this.getPresenter().openUModConfig(getuModUUID());
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
        //this.stopUModSearch();

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        TriggerUMod.RequestValues requestValue = new TriggerUMod.RequestValues(uModUUID);
        mTriggerUMod.execute(requestValue, new Subscriber<TriggerUMod.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.enableActionSlider(uModUUID);

            }

            @Override
            public void onError(Throwable e) {
                //TODO make a method that process HTTP error codes for the given RPC.
                /*
                if (e instanceof HttpException) {
                    //Check for HTTP UNAUTHORIZED error code
                    Response<?> response = ((HttpException) e).response();
                    RPC.ResponseError responseError =
                }
                 */
                //Bugfender.e("trigger","Fail to Trigger UModUUID: " + uModUUID + " Cause: " + e.getMessage());
                Timber.e("Fail to Trigger UModUUID: " + uModUUID + " Cause: " + e.getMessage());
                mUModsView.showOpenCloseFail();
                if (e instanceof TriggerUMod.DeletedUserException){
                    UMod saidUMod = ((TriggerUMod.DeletedUserException) e).getInaccessibleUMod();
                    if (saidUMod.getAppUserLevel() == UModUser.Level.INVITED){
                        mUModsView.removeItem(saidUMod.getUUID());
                        return;
                    }
                    mUModsView.appendUMod(createViewModel(saidUMod));
                    return;
                }
                mUModsView.enableActionSlider(uModUUID);
            }

            @Override
            public void onNext(TriggerUMod.ResponseValues responseValues) {

                TriggerRPC.Result response = responseValues.getResult();
                /*
                RPC.ResponseError responseError = response.getResponseError();
                if (responseError != null
                        && responseError.getErrorCode() != null
                        && responseError.getErrorCode() != 0){
                    Log.d("umods_pr", "Error Code: " + responseError.getErrorCode()
                            + "\nError Message: " + responseError.getErrorMessage());
                    mUModsView.showOpenCloseFail();
                    mUModsView.enableActionSlider(uModUUID);
                } else {
                    Log.d("ModsPresenter", "RPC is " + response.toString());
                    mUModsView.showOpenCloseSuccess();
                }
                //If 500 then updateAppUserLevelOnUMod
                //TODO after a successful answer enable action button on view
                 */
                Log.d("umods_pr", "RPC is " + response.toString());
                //Bugfender.d("umods_pr", "Successful Trigger UModUUID: "+ uModUUID + " " + response.toString());
                Timber.d("Successful Trigger UModUUID: "+ uModUUID + " " + response.toString());
                mUModsView.showOpenCloseSuccess();
            }
        });
    }

    private void stopUModSearch(){
        mUModsView.setLoadingIndicator(false);
        mGetUModsOneByOne.unsubscribe();
    }

    @Override
    public void requestAccess(String uModUUID) {
        //this.stopUModSearch();

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice
        RequestAccess.RequestValues requestValues = new RequestAccess.RequestValues(uModUUID);
        mRequestAccess.execute(requestValues, new Subscriber<RequestAccess.ResponseValues>() {
            @Override
            public void onCompleted() {
                //mUModsView.enableActionSlider(uModUUID);
                loadUMods(false);
            }

            @Override
            public void onError(Throwable e) {
                //TODO make a method that process HTTP error codes for the given RPC.
                Log.e("umods_pr", "Request Access Failed: " + e.getMessage());
                //Bugfender.e("umods_pr", "Request Access Failed: " + e.getMessage());
                Timber.e("Request Access Failed: " + e.getMessage());
                mUModsView.showRequestAccessFailedMessage();
                mUModsView.enableActionSlider(uModUUID);
            }

            @Override
            public void onNext(RequestAccess.ResponseValues responseValues) {
                Log.d("umods_pr", responseValues.getResult().toString());
                //Bugfender.d("umods_pr", "Successful Access Request: " + responseValues.getResult().toString());
                Timber.d( "Successful Access Request: " + responseValues.getResult().toString());
                mUModsView.showRequestAccessCompletedMessage();
            }
        });
    }

    @Override
    public void saveOngoingNotificationPreference(boolean isChecked) {
        Preference<Boolean> ongoingNotificationPref = rxSharedPreferences.getBoolean(GlobalConstants.ONGOING_NOTIFICATION_STATE_KEY);
        ongoingNotificationPref.set(isChecked);
    }

    @Override
    public boolean fetchOngoingNotificationPreference() {
        Preference<Boolean> ongoingNotificationPref = rxSharedPreferences.getBoolean(GlobalConstants.ONGOING_NOTIFICATION_STATE_KEY);
        if (ongoingNotificationPref.isSet()){
            return ongoingNotificationPref.get();
        } else {
            return true;//TODO review this
        }
    }
}
