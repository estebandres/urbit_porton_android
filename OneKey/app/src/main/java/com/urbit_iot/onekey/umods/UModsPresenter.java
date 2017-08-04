package com.urbit_iot.onekey.umods;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.umodconfig.UModConfigActivity;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.commands.OpenCloseCmd;
import com.urbit_iot.onekey.data.source.UModsDataSource;
import com.urbit_iot.onekey.umods.domain.usecase.ClearAlienUMods;
import com.urbit_iot.onekey.umods.domain.usecase.DisableUModNotification;
import com.urbit_iot.onekey.umods.domain.usecase.EnableUModNotification;
import com.urbit_iot.onekey.umods.domain.usecase.GetUMods;
import com.urbit_iot.onekey.umods.domain.usecase.GetUModsOneByOne;
import com.urbit_iot.onekey.umods.domain.usecase.OpenCloseUMod;
import com.urbit_iot.onekey.util.EspressoIdlingResource;

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
    private final EnableUModNotification mEnableUModNotification;
    private final DisableUModNotification mDisableUModNotification;
    private final ClearAlienUMods mClearAlienUMods;
    private final OpenCloseUMod mOpenCloseUMod;
    private final GetUModsOneByOne mGetUModsOneByOne;

    private UModsFilterType mCurrentFiltering = UModsFilterType.ALL_UMODS;

    private boolean mFirstLoad = true;

    @Inject
    public UModsPresenter(@NonNull UModsContract.View umodsView,
                          @NonNull GetUMods getUMods,
                          @NonNull GetUModsOneByOne getUModsOneByOne,
                          @NonNull EnableUModNotification enableUModNotification,
                          @NonNull DisableUModNotification disableUModNotification,
                          @NonNull ClearAlienUMods clearAlienUMods,
                          @NonNull OpenCloseUMod openCloseUMod) {
        mUModsView = checkNotNull(umodsView, "tasksView cannot be null!");
        mGetUModsOneByOne = checkNotNull(getUModsOneByOne, "getUModsOneByOne cannot be null!");
        mGetUMods = checkNotNull(getUMods, "getUMod cannot be null!");
        mEnableUModNotification = checkNotNull(enableUModNotification, "enableUModNotification cannot be null!");
        mDisableUModNotification = checkNotNull(disableUModNotification, "disableUModNotification cannot be null!");
        mClearAlienUMods = checkNotNull(clearAlienUMods,
                "clearAlienUMods cannot be null!");
        mOpenCloseUMod = checkNotNull(openCloseUMod, "openCloseUMod cannot be null!");
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
        mEnableUModNotification.unsubscribe();
        mDisableUModNotification.unsubscribe();
        mClearAlienUMods.unsubscribe();
        mGetUModsOneByOne.unsubscribe();
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
    private void loadUMods(boolean forceUpdate, final boolean showLoadingUI) {
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
                processTasks(values.getUMods());
            }
        });
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

        mGetUModsOneByOne.unsubscribe();
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
                mUModsView.showLoadingUModsError();
            }

            @Override
            public void onNext(GetUModsOneByOne.ResponseValues values) {
                Log.d("umods_pr", values.getUMod().toString());
                mUModsView.showUMod(values.getUMod());
            }
        });
    }

    private void processTasks(List<UMod> uMods) {
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
    public void openUModDetails(@NonNull UMod requestedUMod) {
        checkNotNull(requestedUMod, "requestedUMod cannot be null!");
        mUModsView.showUModConfigUi(requestedUMod.getUUID());
    }

    @Override
    public void enableUModNotification(@NonNull UMod notificationDisabledUMod) {
        checkNotNull(notificationDisabledUMod, "notificationDisabledUMod cannot be null!");
        mEnableUModNotification.execute(new EnableUModNotification.RequestValues(notificationDisabledUMod.getUUID()),
                new Subscriber<RxUseCase.NoResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.showUModNotificationEnabled();
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
    public void disableUModNotification(@NonNull UMod notificationEnabledUMod) {
        checkNotNull(notificationEnabledUMod, "notificationEnabledUMod cannot be null!");
        mDisableUModNotification.unsubscribe();
        mDisableUModNotification.execute(new DisableUModNotification.RequestValues(notificationEnabledUMod.getUUID()),
                new Subscriber<RxUseCase.NoResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.showUModNotificationDisabled();
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
    public void openCloseUMod(UMod uMod) {

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        mOpenCloseUMod.unsubscribe();
        OpenCloseUMod.RequestValues requestValue = new OpenCloseUMod.RequestValues(uMod);
        mOpenCloseUMod.execute(requestValue, new Subscriber<OpenCloseUMod.ResponseValues>() {
            @Override
            public void onCompleted() {
                mUModsView.setLoadingIndicator(false);
            }

            @Override
            public void onError(Throwable e) {
                mUModsView.showLoadingUModsError();
            }

            @Override
            public void onNext(OpenCloseUMod.ResponseValues responseValues) {
                OpenCloseCmd.Response response = responseValues.getResponse();
                Log.d("ModsPresenter", "Command is " + response.getCommandCode());
                mUModsView.showOpenCloseSuccess();
            }
        });
    }

}
