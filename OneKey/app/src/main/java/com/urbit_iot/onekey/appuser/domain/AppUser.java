package com.urbit_iot.onekey.appuser.domain;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class AppUser {
    @NonNull
    @SerializedName("phone_number")
    private final String phoneNumber;

    @NonNull
    @SerializedName("app_uuid")
    private final String appUUID;

    @Nullable
    @SerializedName("appuid_hash")
    private String appUUIDHash;

    @Nullable
    @SerializedName("user_alias")
    private String alias;

    @Nullable
    @SerializedName("first_name")
    private String firstName;

    @Nullable
    @SerializedName("last_name")
    private String lastName;

    @Nullable
    @SerializedName("email_address")
    private String emailAddress;

    @Nullable
    @SerializedName("action_password")
    private String actionPassword;
    //TODO what if I want to use my lock screen as actionPassword?

    public AppUser(@NonNull String phoneNumber, @NonNull String appUUID, @NonNull String appUUIDHash) {
        this.phoneNumber = phoneNumber;
        this.appUUID = appUUID;
        this.appUUIDHash = appUUIDHash;
    }

    @Nullable
    public String getAppUUIDHash(){
        return this.appUUIDHash;
    }

    public void setAppUUIDHash(@Nullable String appUUIDHash) {
        this.appUUIDHash = appUUIDHash;
    }

    @NonNull
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Nullable
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

    @Override
    public String toString() {
        return "[" + this.getPhoneNumber() + " ; " + this.getAppUUID() + "]" ;
    }
}