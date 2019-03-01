package com.urbit_iot.porton.umodsnotification;

import androidx.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.source.internet.UModMqttServiceContract;
import com.urbit_iot.porton.umods.domain.usecase.RequestAccess;
import com.urbit_iot.porton.umodsnotification.domain.usecase.GetUModsForNotif;
import com.urbit_iot.porton.umodsnotification.domain.usecase.TriggerUModByNotif;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by andresteve07 on 4/20/18.
 */

public class UModsNotifPresenter implements UModsNotifContract.Presenter {

    private Map<String, UMod> mCachedUModsMap;

    private List<String> mCachedKeysList;

    @NonNull
    private final UModsNotifContract.View mUModsNotifView;
    @NonNull
    private final GetUModsForNotif mGetUModsForNotif;
    @NonNull
    private final TriggerUModByNotif mTriggerUModByNotif;
    @NonNull
    private final RequestAccess mRequestAccess;
    @NonNull
    private final UModMqttServiceContract mUModMqttService;
    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    private final PublishSubject<Boolean> cancelPreventiveLockingSubject;

    @Inject
    public UModsNotifPresenter(@NonNull UModsNotifContract.View mUModsNotifView,
                               @NonNull GetUModsForNotif mGetUModsForNotif,
                               @NonNull TriggerUModByNotif mTriggerUModByNotif,
                               @NonNull RequestAccess mRequestAccess,
                               @NonNull UModMqttServiceContract mUModMqttService,
                               @NonNull BaseSchedulerProvider mSchedulerProvider) {
        this.mUModsNotifView = mUModsNotifView;
        this.mGetUModsForNotif = mGetUModsForNotif;
        this.mTriggerUModByNotif = mTriggerUModByNotif;
        this.mRequestAccess = mRequestAccess;
        this.mUModMqttService = mUModMqttService;
        this.mSchedulerProvider = mSchedulerProvider;
        this.mCachedUModsMap = new LinkedHashMap<>();
        this.mCachedKeysList = new ArrayList<>();
        this.cancelPreventiveLockingSubject = PublishSubject.create();
    }

    /**
     * Method injection is used here to safely reference {@code this} after the object is created.
     * For more information, see Java Concurrency in Practice.
     */
    @Inject
    void setupListeners() {
        mUModsNotifView.setPresenter(this);
    }

    @Override
    public void subscribe() {
        Log.d("notif_presenter", "subscribed." + Thread.currentThread().getName());
        this.loadUMods(false);
    }

    @Override
    public void unsubscribe() {
        this.mTriggerUModByNotif.unsubscribe();
        this.mGetUModsForNotif.unsubscribe();
        this.mRequestAccess.unsubscribe();
    }

    @Override
    public void loadUMods(boolean forceUpdate) {
        if (!this.mUModsNotifView.isWiFiConnected()){
            this.mUModsNotifView.showUnconnectedPhone();
            return;
        }

        mUModsNotifView.showLockedView();
        mUModsNotifView.showLoadProgress();

        mCachedUModsMap.clear();
        this.mGetUModsForNotif.execute(
                new GetUModsForNotif.RequestValues(forceUpdate),
                new Subscriber<GetUModsForNotif.ResponseValues>() {
            @Override
            public void onCompleted() {
                String currentUModUUID = null;
                if (mCachedKeysList.size()>0){
                    currentUModUUID = mCachedKeysList.get(0);
                }
                mCachedKeysList = new ArrayList<>(mCachedUModsMap.keySet());
                if (mCachedKeysList.size()>0){
                    Log.d("notif_presenter", Arrays.toString(mCachedKeysList.toArray()));
                    Collections.sort(mCachedKeysList, String::compareTo);
                    if (currentUModUUID != null && mCachedKeysList.contains(currentUModUUID)){
                        int index = 0;
                        for (String uuid : mCachedKeysList){
                            if (uuid.equals(currentUModUUID)){
                                break;
                            }
                            index++;
                        }
                        Collections.rotate(mCachedKeysList,-(index));
                    }

                    UMod selectionUMod = mCachedUModsMap.get(mCachedKeysList.get(0));

                    if (selectionUMod.getAppUserLevel() == UModUser.Level.AUTHORIZED
                            || selectionUMod.getAppUserLevel() == UModUser.Level.ADMINISTRATOR){
                        mUModsNotifView.showTriggerView(selectionUMod.getUUID(), selectionUMod.getAlias());
                    } else {
                        mUModsNotifView.showRequestAccessView(selectionUMod.getUUID(), selectionUMod.getAlias());
                    }
                    if (mCachedKeysList.size()==1){
                        mUModsNotifView.hideSelectionControls();
                    } else {
                        mUModsNotifView.showSelectionControls();
                    }
                } else {
                    Log.d("notif_presenter", "NO UMODS FOUND!!" + Thread.currentThread().getName());
                    mUModsNotifView.showNoUModsFound();
                }
                mUModsNotifView.hideProgressView();
            }

            @Override
            public void onError(Throwable e) {
                Log.e("notif_presenter", "" + e.getMessage());
                if (e instanceof GetUModsForNotif.NoUModsAreNotifEnabledException){
                    mUModsNotifView.showAllUModsAreNotifDisabled();
                }
                if (e instanceof GetUModsForNotif.NoTriggerableUModsFoundException){
                    mUModsNotifView.showNoConfiguredUMods();
                }
                if (e instanceof GetUModsForNotif.AllUModsTooFarAwayException){
                    mUModsNotifView.showNoUModsFound();
                }
                if (e instanceof GetUModsForNotif.PhoneCurrentLocationUnknownException){
                    mUModsNotifView.showNoUModsFound();
                }
                if (e instanceof GetUModsForNotif.TooOldPhoneLocationException){
                    mUModsNotifView.showNoUModsFound();
                }
                if (e instanceof GetUModsForNotif.PhoneCurrentLocationIsInaccurateException){
                    mUModsNotifView.showNoUModsFound();
                }
                mUModsNotifView.hideProgressView();
            }

            @Override
            public void onNext(GetUModsForNotif.ResponseValues responseValues) {
                UMod thisUMod = responseValues.getUMod();
                mCachedUModsMap.put(thisUMod.getUUID(), thisUMod);
            }
        });
    }

    @Override
    public void triggerUMod(String uModUUID) {
        mUModsNotifView.showLockedView();
        mUModsNotifView.showTriggerProgress();

        this.mTriggerUModByNotif.execute(
                new TriggerUModByNotif.RequestValues(uModUUID),
                new Subscriber<TriggerUModByNotif.ResponseValues>() {
            @Override
            public void onCompleted() {
                Log.d("triggerByNotification","Trigger Completo!!" + Thread.currentThread().getName());
                mUModsNotifView.hideProgressView();
                cancelPreventiveLockingSubject.onNext(true);
            }

            @Override
            public void onError(Throwable e) {
                Log.e("triggerByNotification","" + e.getMessage() + Thread.currentThread().getName());
                //Bugfender.e("triggerByNotification","Fail to Trigger UModUUID: " + uModUUID + " Cause: " + e.getMessage() + Thread.currentThread().getName());
                Timber.e("Fail to Trigger UModUUID: " + uModUUID + " Cause: " + e.getMessage() + Thread.currentThread().getName());
                mUModsNotifView.hideProgressView();
                cancelPreventiveLockingSubject.onNext(true);

            }

            @Override
            public void onNext(TriggerUModByNotif.ResponseValues responseValues) {
                Log.d("triggerByNotification",responseValues.getResult().toString());
                //Bugfender.d("triggerByNotification","Successful Trigger UModUUID: " + uModUUID + " " +responseValues.getResult().toString());
                Timber.d("Successful Trigger UModUUID: " + uModUUID + " " +responseValues.getResult().toString());
            }
        });
    }

    @Override
    public void requestAccess(String uModUUID) {
        this.mUModsNotifView.setTitleText("NUM:" + new Random().nextInt());
    }

    @Override
    public void lockUModOperation() {
        //this.mUModsNotifView.toggleLockState();
        if (mUModsNotifView.getLockState()){
            mUModsNotifView.showUnlockedView();
            preventiveLock();
        } else {
            mUModsNotifView.showLockedView();
            this.cancelPreventiveLockingSubject.onNext(true);

        }
    }

    private void preventiveLock(){
        Observable.just(true)
                .delay(3000, TimeUnit.MILLISECONDS)
                .takeUntil(cancelPreventiveLockingSubject)
                .doOnNext(aBoolean -> {
                    if (!mUModsNotifView.getLockState()){
                        mUModsNotifView.showLockedView();
                    }
                })
                //TODO replace scheduler for dagger instance
                .subscribeOn(mSchedulerProvider.io())
                .subscribe();
    }

    @Override
    public void previousUMod() {
        rotateUModsList(false);
    }

    @Override
    public void nextUMod() {
        rotateUModsList(true);
    }

    @Override
    public void wifiIsOn() {
        //this.mUModMqttService.reconnectionAttempt();
        this.loadUMods(false);
    }

    @Override
    public void wifiIsOff() {
        this.mUModsNotifView.showUnconnectedPhone();
    }

    private void rotateUModsList(boolean rotateForward){
        this.cancelPreventiveLockingSubject.onNext(true);
        if(!mUModsNotifView.getLockState()){
            mUModsNotifView.showLockedView();
        }
        if (mCachedUModsMap.size()>0 && this.mCachedKeysList.size()==mCachedUModsMap.size()){
            //this.mCachedKeysList = new ArrayList<>(this.mCachedUModsMap.keySet());//TODO is it always correct??
            if (mCachedKeysList.size()>1){
                if (rotateForward){
                    Collections.rotate(mCachedKeysList,-1);
                } else {
                    Collections.rotate(mCachedKeysList,+1);
                }
            }
            UMod currentUMod = mCachedUModsMap.get(mCachedKeysList.get(0));
            if (currentUMod == null){
                currentUMod = mCachedUModsMap.get(this.mCachedUModsMap.keySet().iterator().next());
            }
            if (currentUMod.canBeTriggeredByAppUser()){
                mUModsNotifView.showTriggerView(currentUMod.getUUID(), currentUMod.getAlias());
            }
            if (currentUMod.getAppUserLevel() == UModUser.Level.UNAUTHORIZED
                    || currentUMod.getAppUserLevel() == UModUser.Level.PENDING){
                mUModsNotifView.showRequestAccessView(currentUMod.getUUID(), currentUMod.getAlias());
            }
        } else {
            this.loadUMods(true);
        }
    }
}
