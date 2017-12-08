package com.urbit_iot.onekey.appuser.data.source;

import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.util.dagger.Local;

import javax.inject.Inject;

import rx.Observable;

/**
 * Created by andresteve07 on 11/14/17.
 */

public class AppUserRepository implements AppUserDataSource {

    private AppUserDataSource mFileDataSource;

    @Inject
    public AppUserRepository(@Local AppUserDataSource fileDataSource){
        mFileDataSource = fileDataSource;
    }
    @Override
    public Observable<AppUser> saveAppUser(AppUser appUser) {
        return mFileDataSource.saveAppUser(appUser);
    }

    @Override
    public Observable<AppUser> getAppUser() {
        return mFileDataSource.getAppUser();
    }

    @Override
    public Observable<String> getAppUUID() {
        return mFileDataSource.getAppUUID();
    }
}
