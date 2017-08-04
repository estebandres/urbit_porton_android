package com.urbit_iot.onekey.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by andresteve07 on 8/21/17.
 */

public class UModUser {

    public enum UModUserStatus{
        PENDING(2),
        AUTHORIZED(1),
        UNAUTHORIZED(0),
        TEMPORAL(3),
        WATCHER(4),
        PRE_APPROVED(5),
        ADMINISTRATOR(6);
        private final Integer statusID;
        UModUserStatus(Integer statusID){
            this.statusID = statusID;
        }
        public Integer getStatusID(){
            return this.statusID;
        }
    }

    @NonNull
    private String uModUUID;
    @NonNull
    private String phoneNumber;
    @Nullable
    private String userAlias;
    @NonNull
    private UModUserStatus userStatus;

    public UModUser(@NonNull String uModUUID, @NonNull String phoneNumber, @NonNull UModUserStatus userStatus) {
        this.uModUUID = uModUUID;
        this.phoneNumber = phoneNumber;
        this.userStatus = userStatus;
    }

    @NonNull
    public String getuModUUID() {
        return uModUUID;
    }

    public void setuModUUID(@NonNull String uModUUID) {
        this.uModUUID = uModUUID;
    }

    @NonNull
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(@NonNull String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Nullable
    public String getUserAlias() {
        return userAlias;
    }

    public void setUserAlias(@Nullable String userAlias) {
        this.userAlias = userAlias;
    }

    @NonNull
    public UModUserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(@NonNull UModUserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public String getNameForList(){
        return this.phoneNumber;
    }

    public boolean isAdmin(){
        return this.userStatus == UModUserStatus.ADMINISTRATOR;
    }
}
