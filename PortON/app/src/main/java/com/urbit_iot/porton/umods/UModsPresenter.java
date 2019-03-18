package com.urbit_iot.porton.umods;

import androidx.annotation.NonNull;
import android.util.Log;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.common.base.Strings;
import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.TriggerRPC;
import com.urbit_iot.porton.data.source.PhoneConnectivity;
import com.urbit_iot.porton.data.source.UModsDataSource;
import com.urbit_iot.porton.umods.domain.usecase.CalibrateUMod;
import com.urbit_iot.porton.umods.domain.usecase.ClearAlienUMods;
import com.urbit_iot.porton.umods.domain.usecase.GateStatusUpdates;
import com.urbit_iot.porton.umods.domain.usecase.GetUMods;
import com.urbit_iot.porton.umods.domain.usecase.GetUModsOneByOne;
import com.urbit_iot.porton.umods.domain.usecase.RequestAccess;
import com.urbit_iot.porton.umods.domain.usecase.SetOngoingNotificationStatus;
import com.urbit_iot.porton.umods.domain.usecase.TriggerUModUC;
import com.urbit_iot.porton.util.EspressoIdlingResource;
import com.urbit_iot.porton.util.GlobalConstants;
import com.urbit_iot.porton.util.IntegerContainer;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import java.util.ArrayList;
import java.util.Date;
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
    @NonNull
    private final GetUMods mGetUMods;
    @NonNull
    private final ClearAlienUMods mClearAlienUMods;
    @NonNull
    private final TriggerUModUC mTriggerUModUC;
    @NonNull
    private final GetUModsOneByOne mGetUModsOneByOne;
    @NonNull
    private final SetOngoingNotificationStatus mSetOngoingNotificationStatus;
    @NonNull
    private final RequestAccess mRequestAccess;
    @NonNull
    private RxSharedPreferences rxSharedPreferences;
    @NonNull
    private final PhoneConnectivity mPhoneConnectivity;
    @NonNull
    private final CalibrateUMod mCalibrateUMod;
    @NonNull
    private final GateStatusUpdates gateStatusUpdates;

    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    private UModsFilterType mCurrentFiltering = UModsFilterType.ONLINE_UMODS;

    private boolean mFirstLoad = true;
    private String triggeredUModUUID;

    @Inject
    public UModsPresenter(@NonNull UModsContract.View umodsView,
                          //@NonNull RetrofitUtils mRetrofitUtils,
                          @NonNull GetUMods getUMods,
                          @NonNull GetUModsOneByOne getUModsOneByOne,
                          @NonNull SetOngoingNotificationStatus setOngoingNotificationStatus,
                          @NonNull ClearAlienUMods clearAlienUMods,
                          @NonNull TriggerUModUC triggerUModUC,
                          @NonNull RequestAccess requestAccess,
                          @NonNull RxSharedPreferences rxSharedPreferences,
                          @NonNull PhoneConnectivity mPhoneConnectivity,
                          @NonNull CalibrateUMod mCalibrateUMod,
                          @NonNull GateStatusUpdates statusUpdates,
                          @NonNull BaseSchedulerProvider mSchedulerProvider) {
        mUModsView = checkNotNull(umodsView, "tasksView cannot be null!");
        mGetUModsOneByOne = checkNotNull(getUModsOneByOne, "getUModsOneByOne cannot be null!");
        mGetUMods = checkNotNull(getUMods, "getUModUUID cannot be null!");
        mSetOngoingNotificationStatus = checkNotNull(setOngoingNotificationStatus, "setOngoingNotificationStatus cannot be null!");
        mClearAlienUMods = checkNotNull(clearAlienUMods,
                "clearAlienUMods cannot be null!");
        mTriggerUModUC = checkNotNull(triggerUModUC, "userTriggerUMod cannot be null!");
        mRequestAccess = checkNotNull(requestAccess, "requestAccess cannot be null!");
        //this.mRetrofitUtils = mRetrofitUtils;
        this.rxSharedPreferences = rxSharedPreferences;
        this.mPhoneConnectivity = checkNotNull(mPhoneConnectivity,"mPhoneConnectivity cannot be null!");
        this.mCalibrateUMod = checkNotNull(mCalibrateUMod, "mCalibrateUMod cannot be null!");
        this.gateStatusUpdates = statusUpdates;
        this.mSchedulerProvider = mSchedulerProvider;
    }

    /**
     * Method injection is used here to safely reference {@code this} after the object is created.
     * For more information, see Java Concurrency in Practice.
     */
    @Inject
    void setupListeners() {
        mUModsView.setPresenter(this);
        mUModsView.setSchedulerProvider(mSchedulerProvider);
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
        mTriggerUModUC.unsubscribe();
        mGetUModsOneByOne.unsubscribe();
        mSetOngoingNotificationStatus.unsubscribe();
        mRequestAccess.unsubscribe();
        this.gateStatusUpdates.unsubscribe();
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
                EspressoIdlingResource.decrement();
                mUModsView.setLoadingIndicator(false);
                if (onNextCount.getValue() <= 0) {
                    Log.e("umods_pr", "getUModsOnexOne didn't retreive any result: " + onNextCount);
                    mUModsView.showNoUMods();
                }
                if (forceUpdate){
                    mUModsView.refreshOngoingNotification();
                }
                subscribeToGateStatusUpdates();
            }

            @Override
            public void onError(Throwable e) {
                Log.e("umods_pr", "" + e.getMessage());
                mUModsView.setLoadingIndicator(false);
                mUModsView.showLoadingUModsError();
                EspressoIdlingResource.decrement();
            }

            @Override
            public void onNext(GetUModsOneByOne.ResponseValues values) {
                Log.d("umods_pr", values.getUMod().toString());
                mUModsView.showUMod(createViewModel(values.getUMod()));
                onNextCount.plusOne();
            }
        });
    }

    public void subscribeToGateStatusUpdates() {
        this.gateStatusUpdates.execute(new GateStatusUpdates.RequestValues(), new Subscriber<GateStatusUpdates.ResponseValues>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.e("UMODS_PRESENTER","GateStatusUpdates failure: " + e.getMessage(), e);
            }

            @Override
            public void onNext(GateStatusUpdates.ResponseValues responseValues) {
                String gateStatusTagText;
                UModsFragment.UModViewModelColors gateStatusTagColor;
                UModsFragment.UModViewModelColors gateStatusTagTextColor;
                UMod.GateStatus gateStatus = UMod.GateStatus.from(responseValues.getGateStatus().getStatusCode());
                //TODO switch logic is replicated inside createViewModel method.
                switch (gateStatus){
                    case OPENING:
                        gateStatusTagText = GlobalConstants.OPENING_GATE__TAG_TEXT;
                        gateStatusTagColor = UModsFragment.UModViewModelColors.OPEN_GATE_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.OPEN_GATE_TAG_TEXT;
                        break;
                    case OPEN:
                        gateStatusTagText = GlobalConstants.OPEN_GATE__TAG_TEXT;
                        gateStatusTagColor = UModsFragment.UModViewModelColors.OPEN_GATE_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.OPEN_GATE_TAG_TEXT;
                        break;
                    case CLOSING:
                        gateStatusTagText = GlobalConstants.CLOSING_GATE__TAG_TEXT;
                        gateStatusTagColor = UModsFragment.UModViewModelColors.CLOSED_GATE_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.CLOSED_GATE_TAG_TEXT;
                        break;
                    case CLOSED:
                        gateStatusTagText = GlobalConstants.CLOSED_GATE__TAG_TEXT;
                        gateStatusTagColor = UModsFragment.UModViewModelColors.CLOSED_GATE_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.CLOSED_GATE_TAG_TEXT;
                        break;
                    default:
                        gateStatusTagText = GlobalConstants.UNKNOWN_GATE_STATUS__TAG_TEXT;
                        gateStatusTagColor = UModsFragment.UModViewModelColors.UNKNOWN_GATE_STATUS_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.UNKNOWN_GATE_STATUS_TAG_TEXT;
                        break;
                }

                mUModsView.updateUModGateStatus(responseValues.getUmodUUID(),
                        gateStatusTagText, gateStatusTagColor, gateStatusTagTextColor);
            }
        });

    }

    private String getTimeDifferenceString(Date oldDate){
        String timeDiffString = "Hace %s %s";
        Date nowDate = new Date();
        long millisecondsDiff = nowDate.getTime() - oldDate.getTime();
        if (millisecondsDiff<15000L){
            return null;
        }
        long units;
        units = millisecondsDiff/(1000*3600*24*7);//weeks
        if (units == 1){
            return String.format(timeDiffString,"una","semana");
        }
        if (units > 1){
            return String.format(timeDiffString,String.valueOf(units),"semanas");
        }
        units = millisecondsDiff/(1000*3600*24);//days
        if (units == 1){
            return String.format(timeDiffString,"un","día");
        }
        if (units > 1){
            return String.format(timeDiffString,String.valueOf(units),"días");
        }
        units = millisecondsDiff/(1000*3600);//hours
        if (units == 1){
            return String.format(timeDiffString,"una","hora");
        }
        if (units > 1){
            return String.format(timeDiffString,String.valueOf(units),"hs");
        }
        units = millisecondsDiff/(1000*60);//minutes
        if (units == 1){
            return String.format(timeDiffString,"un","min");
        }
        if (units > 1){
            return String.format(timeDiffString,String.valueOf(units),"min");
        }
        units = millisecondsDiff/(1000);//seconds
        return String.format(timeDiffString,String.valueOf(units),"seg");
    }

    //TODO peer review view model creation rules
    private UModViewModel createViewModel(UMod uMod){
        Log.d("umods_pres", uMod.hashCode() + "\n" + uMod.toString());
        final String uModUUID;
        String itemMainText;
        String connectionTagText;
        String gateStatusText;
        boolean timeTextVisible = true;
        String timeText = getTimeDifferenceString(uMod.getLastUpdateDate());
        if (timeText == null){
            timeTextVisible = false;
        }
        boolean notificationBellFilled;
        boolean notificationBellVisible;
        String sliderText;
        boolean sliderVisible;
        boolean settingsButtonVisible;
        boolean sliderEnabled;
        UModsFragment.UModViewModelColors connectionTagColor,
                connectionTagTextColor, gateStatusTagColor,
                gateStatusTagTextColor, sliderBackgroundColor, sliderTextColor;
        boolean moduleTagsVisible;
        boolean gateStatusTagVisible = false;

        uModUUID = uMod.getUUID();

        if(uMod.getAlias()!=null && !uMod.getAlias().isEmpty()){
            itemMainText = uMod.getAlias();
        } else {
            itemMainText = uModUUID;
        }

        if (uMod.canBeTriggeredByAppUser() && uMod.getState() != UMod.State.AP_MODE){
            notificationBellVisible = true;
        } else {
            notificationBellVisible = false;
        }

        if (uMod.getState() == UMod.State.STATION_MODE) {
            sliderEnabled = true;
            switch (uMod.getAppUserLevel()) {
                case PENDING:
                    sliderText = GlobalConstants.PENDING_SLIDER_TEXT;
                    sliderTextColor = UModsFragment.UModViewModelColors.ACCESS_REQUEST_SLIDER_TEXT;
                    sliderBackgroundColor = UModsFragment.UModViewModelColors.ACCESS_REQUEST_SLIDER_BACKGROUND;
                    sliderVisible = true;
                    sliderEnabled = false;
                    moduleTagsVisible = false;
                    notificationBellVisible = false;
                    settingsButtonVisible = false;
                    break;
                case AUTHORIZED:
                    sliderText = GlobalConstants.TRIGGER_SLIDER_TEXT;
                    sliderBackgroundColor = UModsFragment.UModViewModelColors.TRIGGER_SLIDER_BACKGROUND;
                    sliderTextColor = UModsFragment.UModViewModelColors.TRIGGER_SLIDER_TEXT;
                    sliderVisible = true;
                    moduleTagsVisible = true;
                    settingsButtonVisible = true;
                    break;
                case ADMINISTRATOR:
                    sliderText = GlobalConstants.TRIGGER_SLIDER_TEXT;
                    sliderBackgroundColor = UModsFragment.UModViewModelColors.TRIGGER_SLIDER_BACKGROUND;
                    sliderTextColor = UModsFragment.UModViewModelColors.TRIGGER_SLIDER_TEXT;
                    sliderVisible = true;
                    moduleTagsVisible = true;
                    settingsButtonVisible = true;
                    break;
                case INVITED:
                case UNAUTHORIZED:
                    sliderText = GlobalConstants.REQUEST_ACCESS_SLIDER_TEXT;
                    sliderBackgroundColor = UModsFragment.UModViewModelColors.ACCESS_REQUEST_SLIDER_BACKGROUND;
                    sliderTextColor = UModsFragment.UModViewModelColors.ACCESS_REQUEST_SLIDER_TEXT;
                    sliderVisible = true;
                    moduleTagsVisible = false;
                    settingsButtonVisible = false;
                    break;
                default:
                    sliderText = "DEFAULT_NON";
                    sliderBackgroundColor = UModsFragment.UModViewModelColors.OFFLINE_TAG;
                    sliderTextColor = UModsFragment.UModViewModelColors.TRIGGER_SLIDER_TEXT;
                    sliderVisible = false;
                    moduleTagsVisible = false;
                    settingsButtonVisible = false;
                    break;
            }
        } else {
            //Module is in AP_MODE
            sliderText = null;
            sliderVisible = false;
            sliderEnabled = false;
            sliderBackgroundColor = null;
            sliderTextColor = null;

            settingsButtonVisible = true;
            timeTextVisible = false;
            moduleTagsVisible = false;
        }

        notificationBellFilled = uMod.isOngoingNotificationEnabled();

        if (moduleTagsVisible){
            //UMod is online or very recently cashed
            if (uMod.getuModSource() == UMod.UModSource.LAN_SCAN
                    || uMod.getuModSource() == UMod.UModSource.MQTT_SCAN || timeText == null){
                connectionTagText = GlobalConstants.ONLINE_TAG__TEXT;
                connectionTagColor = UModsFragment.UModViewModelColors.ONLINE_TAG;
                connectionTagTextColor = UModsFragment.UModViewModelColors.ONLINE_TAG_TEXT;
                gateStatusTagVisible = uMod.getGateStatus() != UMod.GateStatus.NO_SENSOR;
                switch (uMod.getGateStatus()){
                    case OPEN:
                        gateStatusText = GlobalConstants.OPEN_GATE__TAG_TEXT;;
                        gateStatusTagColor = UModsFragment.UModViewModelColors.OPEN_GATE_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.OPEN_GATE_TAG_TEXT;
                        break;
                    case CLOSED:
                        gateStatusText = GlobalConstants.CLOSED_GATE__TAG_TEXT;
                        gateStatusTagColor = UModsFragment.UModViewModelColors.CLOSED_GATE_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.CLOSED_GATE_TAG_TEXT;
                        break;
                    case CLOSING:
                        gateStatusText = GlobalConstants.CLOSING_GATE__TAG_TEXT;
                        gateStatusTagColor = UModsFragment.UModViewModelColors.CLOSED_GATE_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.CLOSED_GATE_TAG_TEXT;
                        break;
                    case OPENING:
                        gateStatusText = GlobalConstants.OPENING_GATE__TAG_TEXT;
                        gateStatusTagColor = UModsFragment.UModViewModelColors.OPEN_GATE_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.OPEN_GATE_TAG_TEXT;
                        break;
                    case PARTIAL_OPENING:
                        gateStatusText = "SEMI ABIERTO";
                        gateStatusTagColor = UModsFragment.UModViewModelColors.OPEN_GATE_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.OPEN_GATE_TAG_TEXT;
                        break;
                    default:
                        gateStatusText = GlobalConstants.UNKNOWN_GATE_STATUS__TAG_TEXT;
                        gateStatusTagColor = UModsFragment.UModViewModelColors.UNKNOWN_GATE_STATUS_TAG;
                        gateStatusTagTextColor = UModsFragment.UModViewModelColors.UNKNOWN_GATE_STATUS_TAG_TEXT;
                        break;
                }

            } else {//UMod is offline i.e. found in chache or DB
                connectionTagText = GlobalConstants.OFFLINE_TAG__TEXT;
                connectionTagColor = UModsFragment.UModViewModelColors.OFFLINE_TAG;
                connectionTagTextColor = UModsFragment.UModViewModelColors.OFFLINE_TAG_TEXT;
                gateStatusTagVisible = false;

                gateStatusText = GlobalConstants.UNKNOWN_GATE_STATUS__TAG_TEXT;
                gateStatusTagColor = UModsFragment.UModViewModelColors.UNKNOWN_GATE_STATUS_TAG;
                gateStatusTagTextColor = UModsFragment.UModViewModelColors.UNKNOWN_GATE_STATUS_TAG_TEXT;
            }
        } else {//Defaults for invisible tags
            connectionTagText = null;
            connectionTagColor = UModsFragment.UModViewModelColors.OFFLINE_TAG;
            connectionTagTextColor = UModsFragment.UModViewModelColors.OFFLINE_TAG_TEXT;
            gateStatusText = null;
            gateStatusTagColor = UModsFragment.UModViewModelColors.UNKNOWN_GATE_STATUS_TAG;
            gateStatusTagTextColor = UModsFragment.UModViewModelColors.UNKNOWN_GATE_STATUS_TAG_TEXT;
        }


        if (uMod.belongsToAppUser()){
            return new UModViewModel(uModUUID, itemMainText,
                    connectionTagText, connectionTagColor, connectionTagTextColor,
                    gateStatusTagVisible,
                    gateStatusText, gateStatusTagColor, gateStatusTagTextColor,
                    moduleTagsVisible,
                    timeText, timeTextVisible,
                    notificationBellFilled, notificationBellVisible,
                    sliderText, sliderVisible, sliderEnabled,
                    settingsButtonVisible,
                    sliderBackgroundColor,
                    sliderTextColor) {
                @Override
                public void onSlideCompleted(UModsContract.Presenter presenter) {
                    presenter.triggerUMod(getuModUUID());
                }

                @Override
                public void onSettingsButtonClicked(UModsContract.Presenter presenter) {
                    presenter.openUModConfig(getuModUUID());
                }
            };
        } else {
            return new UModViewModel(uModUUID, itemMainText,
                    connectionTagText, connectionTagColor, connectionTagTextColor,
                    gateStatusTagVisible,
                    gateStatusText, gateStatusTagColor, gateStatusTagTextColor,
                    moduleTagsVisible,
                    timeText, timeTextVisible,
                    notificationBellFilled, notificationBellVisible,
                    sliderText, sliderVisible, sliderEnabled,
                    settingsButtonVisible,
                    sliderBackgroundColor,
                    sliderTextColor) {
                @Override
                public void onSlideCompleted(UModsContract.Presenter presenter) {
                    presenter.requestAccess(getuModUUID());

                }

                @Override
                public void onSettingsButtonClicked(UModsContract.Presenter presenter) {
                    presenter.openUModConfig(getuModUUID());
                }
            };
        }
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
            case ONLINE_UMODS:
                mUModsView.showNotificationDisabledUModsFilterLabel();
                break;
            default:
                mUModsView.showAllFilterLabel();
                break;
        }
    }

    private void processEmptyTasks() {
        switch (mCurrentFiltering) {
            case ONLINE_UMODS:
                mUModsView.showNotificationDisabledUMods();
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
     * Sets the current umods filtering type.
     *
     * @param requestType Can be {@link UModsFilterType#ALL_UMODS}, or
     *                    {@link UModsFilterType#ONLINE_UMODS},
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
        this.triggeredUModUUID = uModUUID;
        //this.stopUModSearch();

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        TriggerUModUC.RequestValues requestValue = new TriggerUModUC.RequestValues(uModUUID);
        mTriggerUModUC.execute(requestValue, new Subscriber<TriggerUModUC.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.enableActionSlider(uModUUID);
                EspressoIdlingResource.decrement();
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
                Log.e("UMODS_PRES","Trigger Failure: " + e.getMessage() + " TYPE: " + e.getClass().getSimpleName(), e);
                Timber.e("Fail to Trigger UModUUID: " + uModUUID + " Cause: " + e.getMessage());
                mUModsView.showOpenCloseFail();
                if (e instanceof TriggerUModUC.DeletedUserException){
                    UMod saidUMod = ((TriggerUModUC.DeletedUserException) e).getInaccessibleUMod();
                    if (saidUMod != null){
                        mUModsView.removeItem(saidUMod.getUUID());
                    }
                    /*
                    if (saidUMod.getAppUserLevel() == UModUser.Level.INVITED){
                        mUModsView.removeItem(saidUMod.getUUID());
                        return;
                    }
                    mUModsView.appendUMod(createViewModel(saidUMod));
                    return;
                    */
                } else {
                    mUModsView.enableActionSlider(uModUUID);
                }
                EspressoIdlingResource.decrement();
            }

            @Override
            public void onNext(TriggerUModUC.ResponseValues responseValues) {

                TriggerRPC.Result triggerResult = responseValues.getResult();
                Log.d("umods_pr", "RPC is " + triggerResult.toString());
                Timber.d("Successful Trigger UModUUID: "+ uModUUID + " " + triggerResult.toString());
                //For older APIs (v1/v2) then display trigger success
                //For APIv3 this should never be null but still.
                if (triggerResult.getStatusCode() == null){
                    if (!Strings.isNullOrEmpty(triggerResult.getMessage())){
                        mUModsView.showOpenCloseSuccess();
                        return;
                    }
                }
                if (responseValues.getAppUserLevel() == UModUser.Level.ADMINISTRATOR){
                    switch (triggerResult.getStatusCode()){
                        case -2:
                            mUModsView.showDisconnectedSensorDialog();
                            break;
                        case -1:
                            mUModsView.showCalibrationDialogs();
                            break;
                        default:
                            mUModsView.showOpenCloseSuccess();
                            break;
                    }
                } else {
                    mUModsView.showOpenCloseSuccess();
                }
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
                EspressoIdlingResource.decrement();
            }

            @Override
            public void onError(Throwable e) {
                //TODO make a method that process HTTP error codes for the given RPC.
                Log.e("umods_pr", "Request Access Failed: " + e.getMessage());
                //Bugfender.e("umods_pr", "Request Access Failed: " + e.getMessage());
                Timber.e("Request Access Failed: " + e.getMessage());
                mUModsView.showRequestAccessFailedMessage();
                mUModsView.enableActionSlider(uModUUID);
                EspressoIdlingResource.decrement();
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

    @Override
    public void processCalibrationDialogChoice(int choice) {
        switch (choice){
            case 0:
                this.setUModGateStatus(UMod.GateStatus.OPEN);
                break;
            case 1:
                this.setUModGateStatus(UMod.GateStatus.CLOSED);
                break;
            default:
                break;
        }
    }

    private void setUModGateStatus(UMod.GateStatus gateStatus) {
        this.mCalibrateUMod.execute(
                new CalibrateUMod.RequestValues(
                        this.triggeredUModUUID,
                        gateStatus,
                        mPhoneConnectivity.getConnectionType() == PhoneConnectivity.ConnectionType.WIFI,
                        mPhoneConnectivity.getWifiAPSSID()),
                new Subscriber<CalibrateUMod.ResponseValues>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("UMODS_PRES","Calibration Failed: " + e.getMessage() + " Type: " + e.getClass().getSimpleName());
                    }

                    @Override
                    public void onNext(CalibrateUMod.ResponseValues responseValues) {
                        if (responseValues.getResult().getGateStatusCode() == gateStatus.getStatusID()){
                            mUModsView.showCalibrationSuccessMessage();
                        } else {
                            mUModsView.showCalibrationFailureMessage();
                        }
                    }
                });
    }
}
