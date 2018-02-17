package com.urbit_iot.onekey.appuser.data.source;

import com.urbit_iot.onekey.appuser.domain.AppUser;

import rx.Observable;

/**
 * Created by andresteve07 on 11/14/17.
 */

public interface AppUserDataSource {
    Observable<AppUser> createAppUser(AppUser AppUser);//POST
    Observable<AppUser> createAppUser(String appUserPhoneNumber);//POST
    //Observable<AppUser> updateAppUser(AppUser appUser);//PUT
    Observable<AppUser> partiallyUpdateAppUser(AppUser appUser);//PATCH
    //Observable<AppUser> saveAppUser(AppUser appUser);
    Observable<AppUser> getAppUser();//GET /users/MY-USER-ID
    //Observable<String> getAppUUID();
    //Observable<String> getAppUserCredentialsHash();
}
