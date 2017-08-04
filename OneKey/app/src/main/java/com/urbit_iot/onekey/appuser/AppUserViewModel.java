package com.urbit_iot.onekey.appuser;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by andresteve07 on 11/9/17.
 */

public class AppUserViewModel {
    @NonNull
    private final String phoneNumber;

    public AppUserViewModel(@NonNull String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @NonNull
    public String getPhoneNumber() {
        return phoneNumber;
    }

}