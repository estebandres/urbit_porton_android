package com.urbit_iot.porton.appuser;

import androidx.annotation.NonNull;

import com.urbit_iot.porton.appuser.domain.SaveAppUser;

import javax.inject.Inject;

import rx.Subscriber;

/**
 * Created by andresteve07 on 11/9/17.
 */

public class AppUserPresenter implements AppUserContract.Presenter {
    @NonNull
    private final AppUserContract.View mAppUserView;
    @NonNull
    private final SaveAppUser mSaveAppUser;

    /**
     * Dagger strictly enforces that arguments not marked with {@code @Nullable} are not injected
     * with {@code @Nullable} values.
     */
    @Inject
    public AppUserPresenter(@NonNull AppUserContract.View mAppUserView,
                            @NonNull SaveAppUser mSaveAppUser) {
        this.mAppUserView = mAppUserView;
        this.mSaveAppUser = mSaveAppUser;
    }

    @Inject
    void setupListeners() {
        mAppUserView.setPresenter(this);
    }

    @Override
    public void subscribe() {
        populateAppUserView();
    }

    @Override
    public void unsubscribe() {
        mSaveAppUser.unsubscribe();
    }

    @Override
    public void saveAppUser(AppUserViewModel appUserViewModel) {
        mSaveAppUser.execute(new SaveAppUser.RequestValues(appUserViewModel), new Subscriber<SaveAppUser.ResponseValues>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(SaveAppUser.ResponseValues responseValues) {
                mAppUserView.showUModsList(responseValues.getmAppUser().getUserName(), responseValues.getmAppUser().getAppUUID());
            }
        });
    }

    @Override
    public void populateAppUserView() {

    }
}
