/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.urbit_iot.onekey.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

import java.util.Date;

/**
 * Immutable model class for an UMod.
 */
public final class UMod {

    public enum UModState{
        AP_MODE(0),
        IDLE(1),
        IN_OPERATION(2),
        OTA_UPDATE(3),
        DEATH(4);
        private final Integer stateID;
        private static SparseArray<UModState> map = new SparseArray<>();

        static {
            for (UModState stateEnum : UModState.values()) {
                map.put(stateEnum.stateID, stateEnum);
            }
        }

        UModState(Integer stateID){
            this.stateID = stateID;
        }
        public Integer getStateID(){
            return this.stateID;
        }
        public static UModState from(int value) {
            return map.get(value);
        }
    }

    @NonNull
    private final String mUUID;

    @Nullable
    private String mAlias;

    @Nullable
    private boolean mNotificationEnabled;

    @Nullable
    private String mLANIPAddress;

    @Nullable
    private String mWiFiSSID;

    @Nullable
    private String mWiFiPassword;

    @Nullable
    private boolean isInAPMode;

    @NonNull
    private UModUser.UModUserStatus appUserStatus;

    @Nullable
    private UModState uModState;

    @Nullable
    private String uModReport;

    @Nullable
    private String productUUID;

    @Nullable
    private String mHWVersion;

    @Nullable
    private String mSWVersion;

    @NonNull
    private boolean isOpen;//It could be determined by the umodule state. Can an umodule respond to a request while updating??

    //private boolean admitsRinging;

    @NonNull
    private Date lastUpdateDate;

    /**
     * Use this constructor to create a new umod when the data is retrieve from dns-sd but TXT field is absent.
     * @param mUUID
     * @param mLANIPAddress
     */
    public UMod(@NonNull String mUUID, String mLANIPAddress) {
        this.mUUID = mUUID;
        this.mLANIPAddress = mLANIPAddress;
        this.isInAPMode = false;
    }

    /**
     * Use this constructor when the data is retrieved from WiFi AP scanner.
     * @param moduleUUID
     */
    public UMod(@NonNull String moduleUUID, @NonNull String prodUUID, @NonNull String hwVersion, @NonNull String swVersion) {
        this.mUUID = moduleUUID;
        this.mAlias = moduleUUID;
        this.productUUID = prodUUID;
        this.mHWVersion = hwVersion;
        this.mSWVersion = swVersion;

        this.isInAPMode = true;
        this.lastUpdateDate = new Date();
    }

    /**
     * Use only for old mocked execution
     * @param mUUID
     * @param mLANIPAddress
     * @param isOpen
     */

    public UMod(@NonNull String mUUID, String mLANIPAddress, @NonNull boolean isOpen) {
        this.mUUID = mUUID;
        this.mLANIPAddress = mLANIPAddress;
        this.isOpen = isOpen;
    }

    /**
     * Only for mocked execution.
     * @param mUUID
     */
    public UMod(@NonNull String mUUID) {
        this.mUUID = mUUID;
    }

    /**
     * Use this constructor to create a new umod when the data is retrieve from dns-sd and the
     * response includes the TXT fields.
     * @param mUUID
     * @param mAlias
     * @param mLANIPAddress
     * @param uModStateInt
     * @param uModReport
     * @param productUUID
     * @param mHWVersion
     * @param mSWVersion
     * @param isOpen
     */
    public UMod(@NonNull String mUUID, String mAlias, String mLANIPAddress, int uModStateInt,
                String uModReport, String productUUID, String mHWVersion, String mSWVersion,
                @NonNull boolean isOpen) {
        this.mUUID = mUUID;
        this.mAlias = mAlias;
        this.mLANIPAddress = mLANIPAddress;
        this.uModState = UModState.from(uModStateInt);
        this.uModReport = uModReport;
        this.productUUID = productUUID;
        this.mHWVersion = mHWVersion;
        this.mSWVersion = mSWVersion;
        this.isOpen = isOpen;
        this.isInAPMode = false;
        this.lastUpdateDate = new Date();
    }

    @NonNull
    public String getUUID() {
        return mUUID;
    }

    public void setAlias(@Nullable String mAlias) {
        this.mAlias = mAlias;
    }

    @Nullable
    public String getAlias() {
        return mAlias;
    }

    @Nullable
    public String getNameForList() {
        if (!Strings.isNullOrEmpty(mAlias)) {
            return mAlias;
        } else {
            return mUUID;
        }
    }

    @Nullable
    public boolean isNotificationEnabled() {
        return mNotificationEnabled;
    }

    public void enableNotification() {
        this.mNotificationEnabled = true;
    }

    public void disableNotification() {
        this.mNotificationEnabled = false;
    }

    @Nullable
    public String getLANIPAddress() {
        return mLANIPAddress;
    }

    public void setmLANIPAddress(@Nullable String mLANIPAddress) {
        this.mLANIPAddress = mLANIPAddress;
    }

    @Nullable
    public String getmWiFiSSID() {
        return mWiFiSSID;
    }

    public void setmWiFiSSID(@Nullable String mWiFiSSID) {
        this.mWiFiSSID = mWiFiSSID;
    }

    @Nullable
    public String getmWiFiPassword() {
        return mWiFiPassword;
    }

    public void setmWiFiPassword(@Nullable String mWiFiPassword) {
        this.mWiFiPassword = mWiFiPassword;
    }

    @Nullable
    public boolean isOnLAN() {
        return (!this.mLANIPAddress.equalsIgnoreCase("") && checkIPv4Address(this.mLANIPAddress));
    }

    private boolean checkIPv4Address(String ip){
        /*
        Pattern ipv4AddressPattern = Pattern.compile(
                "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

        return ipv4AddressPattern.matcher(ip).matches();
        */
        //TODO use the apache library to avoid the complex and not fully tested regex.
        String ipv4AddressPattern = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        return ip.matches(ipv4AddressPattern);
    }
    @Nullable
    public boolean isInAPMode() {
        return isInAPMode;
    }

    public void setInAPMode(@Nullable boolean inAPMode) {
        isInAPMode = inAPMode;
    }

    @NonNull
    public UModUser.UModUserStatus getAppUserStatus() {
        return appUserStatus;
    }

    public void setAppUserStatus(@NonNull UModUser.UModUserStatus appUserStatus) {
        this.appUserStatus = appUserStatus;
    }

    @Nullable
    public UModState getuModState() {
        return uModState;
    }

    public void setuModState(@Nullable UModState uModState) {
        this.uModState = uModState;
    }

    @Nullable
    public String getuModReport() {
        return uModReport;
    }

    public void setuModReport(@Nullable String uModReport) {
        this.uModReport = uModReport;
    }


    @Nullable
    public String getProductUUID() {
        return productUUID;
    }

    @Nullable
    public String getHWVersion() {
        return mHWVersion;
    }

    @Nullable
    public String getSWVersion() {
        return mSWVersion;
    }

    @NonNull
    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(@NonNull Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public void setProductUUID(@Nullable String productUUID) {
        this.productUUID = productUUID;
    }

    public void setHWVersion(@Nullable String mHWVersion) {
        this.mHWVersion = mHWVersion;
    }

    public void setSWVersion(@Nullable String mSWVersion) {
        this.mSWVersion = mSWVersion;
    }

    @NonNull
    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(@NonNull boolean open) {
        isOpen = open;
    }

    public boolean isEmpty(){
        return Strings.isNullOrEmpty(mUUID) &&
                Strings.isNullOrEmpty(mLANIPAddress);
    }

    public boolean belongsToAppUser(){
        return this.getAppUserStatus() != UModUser.UModUserStatus.UNAUTHORIZED ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UMod uMod = (UMod) o;
        return Objects.equal(mUUID, uMod.mUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mUUID);
    }

    @Override
    public String toString() {
        return "I'm UModule: [" + mAlias + " ; " + mUUID + " ; " + mLANIPAddress + "]";
    }
}
