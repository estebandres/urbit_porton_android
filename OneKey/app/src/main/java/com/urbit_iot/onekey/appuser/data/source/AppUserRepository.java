package com.urbit_iot.onekey.appuser.data.source;

import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.util.dagger.Local;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Action1;

/**
 * Created by andresteve07 on 11/14/17.
 */

public class AppUserRepository implements AppUserDataSource {

    private AppUserDataSource mFileDataSource;
    private AppUser mCachedAppUser;

    @Inject
    public AppUserRepository(@Local AppUserDataSource fileDataSource){
        mFileDataSource = fileDataSource;
        mCachedAppUser = null;
    }

    @Override
    public Observable<AppUser> createAppUser(AppUser appUser) {
        return mFileDataSource.createAppUser(appUser);
    }

    @Override
    public Observable<AppUser> createAppUser(String appUserPhoneNumber) {
        return mFileDataSource.createAppUser(appUserPhoneNumber)
                .doOnNext(new Action1<AppUser>() {
                    @Override
                    public void call(AppUser appUser) {
                        mCachedAppUser = appUser;
                    }
                });
    }

    @Override
    public Observable<AppUser> partiallyUpdateAppUser(AppUser appUser) {
        return mFileDataSource.partiallyUpdateAppUser(appUser);
    }

    @Override
    public Observable<AppUser> getAppUser() {
        if (mCachedAppUser != null){
            return Observable.just(mCachedAppUser);
        } else {
            return mFileDataSource.getAppUser();
        }

    }
}
