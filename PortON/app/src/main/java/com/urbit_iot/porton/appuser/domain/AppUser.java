package com.urbit_iot.porton.appuser.domain;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.porton.util.GlobalConstants;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class AppUser {
    @NonNull
    @SerializedName("user_name")
    private String userName;

    @NonNull
    @SerializedName("phone_number")
    private String phoneNumber;

    @NonNull
    @SerializedName("app_uuid")
    private String appUUID;

    @Nullable
    @SerializedName("user_credentials_hash")
    private String appUserCredentialsHash;

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

    public AppUser(@NonNull String phoneNumber, @NonNull String appUUID, @NonNull String appUserCredentialsHash) {
        this.phoneNumber = phoneNumber;
        this.userName = phoneNumber.replace("+","");
        this.appUUID = appUUID;
        this.appUserCredentialsHash = appUserCredentialsHash;
    }

    @NonNull
    public String getUserName() {
        return userName;
    }

    public void setUserName(@NonNull String userName) {
        this.userName = userName;
    }

    public void setPhoneNumber(@NonNull String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setAppUUID(@NonNull String appUUID) {
        this.appUUID = appUUID;
    }

    @Nullable
    public String getAppUserCredentialsHash(){
        return this.appUserCredentialsHash;
    }

    public void setAppUserCredentialsHash(@Nullable String appUserCredentialsHash) {
        this.appUserCredentialsHash = appUserCredentialsHash;
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

    public String getCredentialsString(){
        return this.userName + GlobalConstants.CREDENTIALS_REALM_SEPARATOR + this.appUserCredentialsHash;
    }

    @Override
    public String toString() {
        return "AppUser{" +
                "userName='" + userName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", appUUID='" + appUUID + '\'' +
                ", appUserCredentialsHash='" + appUserCredentialsHash + '\'' +
                ", alias='" + alias + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", actionPassword='" + actionPassword + '\'' +
                '}';
    }
}