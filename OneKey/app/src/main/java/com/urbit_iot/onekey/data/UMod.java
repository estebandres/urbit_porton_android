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

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.urbit_iot.onekey.util.GlobalConstants;

import java.util.Date;

/**
 * Immutable model class for an UMod.
 */
public final class UMod {

    @Nullable
    public Location getuModLocation() {
        return uModLocation;
    }

    public void setuModLocation(@Nullable Location uModLocation) {
        this.uModLocation = uModLocation;
    }

    @Nullable
    public String getLocationAddressString() {
        return locationAddressString;
    }

    public void setLocationAddressString(@Nullable String locationAddressString) {
        this.locationAddressString = locationAddressString;
    }

    public enum State {
        AP_MODE(0),
        STATION_MODE(1),
        OTA_UPDATE(2),
        FACTORY_RESET(3),
        REBOOTING(4),
        BLE_MODE(5),
        UNKNOWN(6);
        private final Integer stateID;
        private static SparseArray<State> map = new SparseArray<>();

        static {
            for (State stateEnum : State.values()) {
                map.put(stateEnum.stateID, stateEnum);
            }
        }
        State(Integer stateID){
            this.stateID = stateID;
        }
        public Integer getStateID(){
            return this.stateID;
        }
        public static State from(int value) {
            return map.get(value);
        }
    }

    public enum UModSource{
        CACHE(0),
        LOCAL_DB(1),
        BLE_SCAN(2),
        DNS_SD_BROWSE(3),
        LAN_SCAN(4),
        WEB(3),
        MQTT_SCAN(5);
        private final Integer sourceID;
        UModSource(Integer sourceID) {
            this.sourceID = sourceID;
        }
        public Integer getSourceID(){
            return this.sourceID;
        }
    }

    @NonNull
    private final String uModUUID;

    @NonNull
    private UModUser.Level appUserLevel;

    @NonNull
    private State state;

    @NonNull
    private boolean lanOperationEnabled;

    @NonNull
    private boolean ongoingNotificationEnabled;

    //TODO change to LocalDate when java8 is supported
    @NonNull
    private Date lastUpdateDate;

    //------------------------------------------

    @Nullable
    private String alias;

    @Nullable
    private String mqttResponseTopic;

    @Nullable
    private  String macAddress;

    @Nullable
    private String connectionAddress;

    @Nullable
    private String wifiSSID;

    @Nullable
    private UModSource uModSource;

    @Nullable
    private String uModLastReport;

    @Nullable
    private String productUUID;

    @Nullable
    private String hwVersion;

    @Nullable
    private String swVersion;

    private boolean isOpen;//It could be determined by the umodule state. Can an umodule respond to a request while updating??

    //private boolean admitsRinging;
    @Nullable
    private Location uModLocation;

    @Nullable
    private String locationAddressString;


    /**
     * Use this constructor to create a new umod when the data is retrieve from dns-sd and TXT contains isOpen.
     * @param uModUUID
     * @param connectionAddress
     */
    public UMod(@NonNull String uModUUID,
                @Nullable String connectionAddress,
                boolean isOpen) {
        this.uModUUID = uModUUID;
        this.alias = uModUUID;
        this.lanOperationEnabled = true;
        this.ongoingNotificationEnabled =  false;
        this.connectionAddress = connectionAddress;
        this.state = State.STATION_MODE;
        this.appUserLevel = UModUser.Level.UNAUTHORIZED;
        this.isOpen = isOpen;
        this.lastUpdateDate = new Date();
    }

    /**
     * Use this constructor to create a new umod when the data is retrieve from BLE scanning.
     * @param uModUUID
     * @param bleHwAddress
     */
    public UMod(@NonNull String uModUUID, @Nullable String bleHwAddress) {
        this.uModUUID = uModUUID;
        this.alias = uModUUID;
        this.lanOperationEnabled = false;
        this.ongoingNotificationEnabled =  false;
        this.connectionAddress = bleHwAddress;
        this.state = State.AP_MODE;
        this.appUserLevel = UModUser.Level.UNAUTHORIZED;
        this.isOpen = true;
        this.lastUpdateDate = new Date();
    }

    /**
     * Use this constructor when the data is retrieved from WiFi AP scanner.
     * @param moduleUUID
     */
    public UMod(@NonNull String moduleUUID) {
        this.uModUUID = moduleUUID;
        this.alias = uModUUID;
        this.lanOperationEnabled = false;
        this.ongoingNotificationEnabled =  false;
        this.isOpen = true;
        this.lastUpdateDate = new Date();
    }

    /**
     * Use this constructor to create a new umod when the data is retrieve from dns-sd and the
     * response includes the TXT fields.
     * @param uModUUID module UUID.
     * @param alias module alias would be shown in UI.
     * @param connectionAddress LAN IP address to direct RPCs.
     * @param uModLastReport
     * @param productUUID
     * @param hwVersion
     * @param swVersion
     * @param isOpen
     */
    public UMod(@NonNull String uModUUID,
                @Nullable String alias,
                @Nullable String connectionAddress,
                @Nullable String uModLastReport,
                @Nullable String productUUID,
                @Nullable String hwVersion,
                @Nullable String swVersion,
                boolean isOpen) {
        this.uModUUID = uModUUID;
        this.ongoingNotificationEnabled =  false;
        this.alias = alias;
        this.lanOperationEnabled = false;
        this.connectionAddress = connectionAddress;
        this.state = State.STATION_MODE;
        this.uModLastReport = uModLastReport;
        this.productUUID = productUUID;
        this.hwVersion = hwVersion;
        this.swVersion = swVersion;
        this.isOpen = isOpen;
        this.lastUpdateDate = new Date();
    }

    /**
     * Use this constructor for the DB entries translations.
     * @param uuid
     * @param alias
     * @param connectionAddress
     * @param uModState
     * @param userLevel
     * @param ongoingNotifEnabled
     * @param macAddress
     * @param lastReport
     * @param productUUID
     * @param hwVersion
     * @param swVersion
     */
    public UMod(@NonNull String uuid,
                @Nullable String alias,
                @NonNull boolean lanOperationEnabled,
                @Nullable String wifiSSID,
                @Nullable String connectionAddress,
                @NonNull State uModState,
                @NonNull UModUser.Level userLevel,
                boolean ongoingNotifEnabled,
                @Nullable String macAddress,
                @Nullable String lastReport,
                @Nullable String productUUID,
                @Nullable String hwVersion,
                @Nullable String swVersion,
                @Nullable Location uModLocation,
                @Nullable String locationAddressString,
                @NonNull Date lastUpdateDate){
        this.uModUUID = uuid;
        this.ongoingNotificationEnabled =  ongoingNotifEnabled;
        this.alias = alias;
        this.lanOperationEnabled = lanOperationEnabled;
        this.wifiSSID = wifiSSID;
        this.connectionAddress = connectionAddress;
        this.state = uModState;
        this.appUserLevel = userLevel;
        this.ongoingNotificationEnabled = ongoingNotifEnabled;
        this.macAddress = macAddress;
        this.uModLastReport = lastReport;
        this.productUUID = productUUID;
        this.hwVersion = hwVersion;
        this.swVersion = swVersion;
        this.uModLocation = uModLocation;
        this.locationAddressString = locationAddressString;
        this.lastUpdateDate = lastUpdateDate;
    }

    @Nullable
    public String getMqttResponseTopic() {
        return mqttResponseTopic;
    }

    public void setMqttResponseTopic(@Nullable String username) {
        this.mqttResponseTopic = GlobalConstants.URBIT_PREFIX + this.uModUUID + "/response/" + username;
    }

    public String getUModRequestTopic(){
        return GlobalConstants.URBIT_PREFIX + this.uModUUID + "/request";
    }

    @Nullable
    public UModSource getuModSource() {
        return uModSource;
    }

    public void setuModSource(@Nullable UModSource uModSource) {
        this.uModSource = uModSource;
    }

    /**
     * Evaluates if the lastUpdateDate is older than a Day
     * @return true if older then a Day (24hs)
     */
    public boolean isOldRegister(){
        long OUTDATED_THRESHOLD = 86400000L;
        return Math.abs((new Date()).getTime() - this.lastUpdateDate.getTime()) >= OUTDATED_THRESHOLD;
    }

    @NonNull
    public String getUUID() {
        return uModUUID;
    }

    public void setAlias(@Nullable String mAlias) {
        this.alias = mAlias;
    }

    @Nullable
    public String getAlias() {
        return alias;
    }

    @Nullable
    public String getNameForList() {
        if (!Strings.isNullOrEmpty(alias)) {
            return alias;
        } else {
            return uModUUID;
        }
    }

    @Nullable
    public boolean isOngoingNotificationEnabled() {
        return ongoingNotificationEnabled;
    }

    public void setOngoingNotificationStatus(Boolean notificationStatus){
        this.ongoingNotificationEnabled = notificationStatus;
    }

    public void enableOngoingNotification() {
        this.ongoingNotificationEnabled = true;
    }

    public void disableOngoingNotification() {
        this.ongoingNotificationEnabled = false;
    }

    @Nullable
    public String getConnectionAddress() {
        return connectionAddress;
    }

    public void setConnectionAddress(@Nullable String connectionAddress) {
        this.connectionAddress = connectionAddress;
    }

    public void updatemLANIPAddress(@Nullable String mLANIPAddress) {
        if(mLANIPAddress != null){
            this.connectionAddress = mLANIPAddress;
        }
    }

    @Nullable
    public String getWifiSSID() {
        return wifiSSID;
    }

    public void setWifiSSID(@Nullable String wifiSSID) {
        this.wifiSSID = wifiSSID;
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
        return this.getState() == State.AP_MODE;
    }

    public void setInAPMode(@Nullable boolean inAPMode) {
    }

    @NonNull
    public UModUser.Level getAppUserLevel() {
        return appUserLevel;
    }

    public void setAppUserLevel(@NonNull UModUser.Level appUserLevel) {
        this.appUserLevel = appUserLevel;
    }

    @Nullable
    public State getState() {
        return state;
    }

    public void setState(@Nullable State state) {
        this.state = state;
    }

    @Nullable
    public String getuModLastReport() {
        return uModLastReport;
    }

    public void setuModLastReport(@Nullable String uModLastReport) {
        this.uModLastReport = uModLastReport;
    }


    @Nullable
    public String getProductUUID() {
        return productUUID;
    }

    @Nullable
    public String getHWVersion() {
        return hwVersion;
    }

    @Nullable
    public String getSWVersion() {
        return swVersion;
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
        this.hwVersion = mHWVersion;
    }

    public void setSWVersion(@Nullable String mSWVersion) {
        this.swVersion = mSWVersion;
    }

    @NonNull
    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(@NonNull boolean open) {
        isOpen = open;
    }

    public boolean isEmpty(){
        return Strings.isNullOrEmpty(uModUUID) &&
                Strings.isNullOrEmpty(connectionAddress);
    }

    public boolean belongsToAppUser(){
        return this.getAppUserLevel() != UModUser.Level.UNAUTHORIZED && this.getAppUserLevel() != UModUser.Level.INVITED;
    }

    public boolean canBeTriggeredByAppUser(){
        return this.getAppUserLevel() == UModUser.Level.ADMINISTRATOR || this.getAppUserLevel() == UModUser.Level.AUTHORIZED;
    }

    @Nullable
    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(@Nullable String macAddress) {
        this.macAddress = macAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UMod uMod = (UMod) o;
        return Objects.equal(uModUUID, uMod.uModUUID);
    }

    @NonNull
    public boolean isLanOperationEnabled() {
        return lanOperationEnabled;
    }

    public void setLanOperationEnabled(@NonNull boolean lanOperationEnabled) {
        this.lanOperationEnabled = lanOperationEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uModUUID);
    }

    @Override
    public String toString() {
        return "UMod{" +
                "uModUUID='" + uModUUID + '\'' +
                ", appUserLevel=" + appUserLevel +
                ", state=" + state +
                ", ongoingNotificationEnabled=" + ongoingNotificationEnabled +
                ", lastUpdateDate=" + lastUpdateDate +
                ", alias='" + alias + '\'' +
                ", connectionAddress='" + connectionAddress + '\'' +
                ", wifiSSID='" + wifiSSID + '\'' +
                ", uModSource=" + uModSource +
                ", uModLastReport='" + uModLastReport + '\'' +
                ", productUUID='" + productUUID + '\'' +
                ", hwVersion='" + hwVersion + '\'' +
                ", swVersion='" + swVersion + '\'' +
                ", isOpen=" + isOpen +
                '}';
    }
}
