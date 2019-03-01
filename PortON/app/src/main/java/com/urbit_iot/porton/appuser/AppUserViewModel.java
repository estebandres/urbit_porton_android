package com.urbit_iot.porton.appuser;

import androidx.annotation.NonNull;

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