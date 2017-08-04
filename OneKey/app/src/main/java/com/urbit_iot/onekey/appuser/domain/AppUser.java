package com.urbit_iot.onekey.appuser.domain;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class AppUser {
    @NonNull
    private final String phoneNumber;
    @NonNull
    private final String appUUID;
    @Nullable
    private String alias;
    @Nullable
    private String firstName;
    @Nullable
    private String lastName;
    @Nullable
    private String emailAddress;
    @Nullable
    private String actionPassword;
    //TODO what if I want to use my lock screen as actionPassword?


    public AppUser(@NonNull String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.appUUID = calculateAppUUID();
    }

    private String calculateAppUUID() {
        return "123456789012345678901234567890";
    }

    public AppUser(@NonNull String phoneNumber, @NonNull String appUUID) {
        this.phoneNumber = phoneNumber;
        this.appUUID = appUUID;
    }

    @NonNull
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @NonNull
    public String getAppUUID() {
        return appUUID;
    }

    @Nullable
    public String getAlias() {
        return alias;
    }

    public void setAlias(@Nullable String alias) {
        this.alias = alias;
    }

    @Nullable
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(@Nullable String firstName) {
        this.firstName = firstName;
    }

    @Nullable
    public String getLastName() {
        return lastName;
    }

    public void setLastName(@Nullable String lastName) {
        this.lastName = lastName;
    }

    @Nullable
    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(@Nullable String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Nullable
    public String getActionPassword() {
        return actionPassword;
    }

    public void setActionPassword(@Nullable String actionPassword) {
        this.actionPassword = actionPassword;
    }
}

