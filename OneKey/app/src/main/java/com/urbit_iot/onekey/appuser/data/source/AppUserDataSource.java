package com.urbit_iot.onekey.appuser.data.source;

import com.urbit_iot.onekey.appuser.domain.AppUser;

import rx.Observable;

/**
 * Created by andresteve07 on 11/14/17.
 */

public interface AppUserDataSource {
    Observable<AppUser> saveAppUser(AppUser appUser);
    Observable<AppUser> getAppUser();
    Observable<String> getAppUUID();
}
