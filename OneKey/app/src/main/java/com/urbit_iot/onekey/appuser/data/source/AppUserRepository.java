package com.urbit_iot.onekey.appuser.data.source;

import com.urbit_iot.onekey.appuser.data.source.localfile.AppUserLocalFileDataSource;
import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.util.dagger.Local;

import javax.inject.Inject;

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
    public void saveAppUser(AppUser appUser) {

    }

    @Override
    public AppUser getAppUser() {
        return null;
    }
}
