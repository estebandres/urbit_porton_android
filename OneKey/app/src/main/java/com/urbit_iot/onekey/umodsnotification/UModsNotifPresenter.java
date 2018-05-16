package com.urbit_iot.onekey.umodsnotification;

import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.umods.domain.usecase.RequestAccess;
import com.urbit_iot.onekey.umodsnotification.domain.usecase.GetUModsForNotif;
import com.urbit_iot.onekey.umodsnotification.domain.usecase.TriggerUModByNotif;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;

import rx.Subscriber;

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

    @Inject
    public UModsNotifPresenter(@NonNull UModsNotifContract.View mUModsNotifView,
                               @NonNull GetUModsForNotif mGetUModsForNotif,
                               @NonNull TriggerUModByNotif mTriggerUModByNotif,
                               @NonNull RequestAccess mRequestAccess) {
        this.mUModsNotifView = mUModsNotifView;
        this.mGetUModsForNotif = mGetUModsForNotif;
        this.mTriggerUModByNotif = mTriggerUModByNotif;
        this.mRequestAccess = mRequestAccess;
        this.mCachedUModsMap = new LinkedHashMap<>();
        this.mCachedKeysList = new ArrayList<>();
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
                    } else {
                        UMod selectionUMod = mCachedUModsMap.get(mCachedKeysList.get(0));
                        if (selectionUMod.getAppUserLevel() == UModUser.Level.AUTHORIZED
                                || selectionUMod.getAppUserLevel() == UModUser.Level.ADMINISTRATOR){
                            mUModsNotifView.showTriggerView(selectionUMod.getUUID(), selectionUMod.getAlias());
                        } else {
                            mUModsNotifView.showRequestAccessView(selectionUMod.getUUID(), selectionUMod.getAlias());
                        }
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
            }

            @Override
            public void onError(Throwable e) {
                Log.e("notif_presenter", e.getMessage());
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

        mUModsNotifView.disableOperationButton();
        mUModsNotifView.showLocked();

        this.mTriggerUModByNotif.execute(
                new TriggerUModByNotif.RequestValues(uModUUID),
                new Subscriber<TriggerUModByNotif.ResponseValues>() {
            @Override
            public void onCompleted() {
                Log.d("triggerByNotification","Trigger Completo!!" + Thread.currentThread().getName());
            }

            @Override
            public void onError(Throwable e) {
                Log.e("triggerByNotification","" + e.getMessage() + Thread.currentThread().getName());
            }

            @Override
            public void onNext(TriggerUModByNotif.ResponseValues responseValues) {
                Log.d("triggerByNotification",responseValues.getResult().getMessage());
            }
        });
    }

    @Override
    public void requestAccess(String uModUUID) {
        this.mUModsNotifView.setTitleText("NUM:" + new Random().nextInt());
    }

    @Override
    public void lockUModOperation(boolean isLocked) {
        if (isLocked){
            mUModsNotifView.showLocked();
            mUModsNotifView.disableOperationButton();
        } else {
            mUModsNotifView.showUnlocked();
            mUModsNotifView.enableOperationButton();
        }

    }

    @Override
    public void previousUMod() {
        rotateUModsList(false);
    }

    @Override
    public void nextUMod() {
        rotateUModsList(true);
    }

    private void rotateUModsList(boolean rotateForward){
        if (mCachedUModsMap.size()>0){
            this.mCachedKeysList = new ArrayList<>(this.mCachedUModsMap.keySet());
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
            if (currentUMod.getAppUserLevel() == UModUser.Level.UNAUTHORIZED){
                mUModsNotifView.showRequestAccessView(currentUMod.getUUID(), currentUMod.getAlias());
            }
        }
    }
}
