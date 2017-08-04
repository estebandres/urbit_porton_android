package com.urbit_iot.onekey.appuser.data.source;

import com.urbit_iot.onekey.appuser.domain.AppUser;

/**
 * Created by andresteve07 on 11/14/17.
 */

public interface AppUserDataSource {
    void saveAppUser(AppUser appUser);
    AppUser getAppUser();
}
