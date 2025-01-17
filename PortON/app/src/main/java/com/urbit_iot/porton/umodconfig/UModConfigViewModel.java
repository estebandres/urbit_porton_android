package com.urbit_iot.porton.umodconfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by steve-urbit on 04/02/18.
 */

public class UModConfigViewModel {
    private String uModUUID;
    private String connectionStatusText;
    private String aliasText;
    private String wifiSSIDText;
    private boolean adminLayoutVisible;
    private String uModSysInfoText;
    private String wifiPasswordText;
    private boolean wifiSettingsVisible;
    private boolean ongoingNotifSwitchChecked;
    private boolean locationSettingsLayoutVisible;
    private String locationText;
    private boolean updateButtonVisible;
    private boolean controlButtonsVisible;
    private List<String> wifiSsidsList;

    public UModConfigViewModel(String uModUUID,
                               String connectionStatusText,
                               String aliasText,
                               String wifiSSIDText,
                               boolean adminLayoutVisible,
                               String uModSysInfoText,
                               String wifiPasswordText,
                               boolean wifiSettingsVisible,
                               boolean ongoingNotifSwitchChecked,
                               boolean locationSettingsLayoutVisible,
                               String locationText,
                               boolean updateButtonVisible,
                               boolean controlButtonsVisible,
                               List<String> wifiSsidsList) {
        this.uModUUID = uModUUID;
        this.connectionStatusText = connectionStatusText;
        this.aliasText = aliasText;
        this.wifiSSIDText = wifiSSIDText;
        this.adminLayoutVisible = adminLayoutVisible;
        this.uModSysInfoText = uModSysInfoText;
        this.wifiPasswordText = wifiPasswordText;
        this.wifiSettingsVisible = wifiSettingsVisible;
        this.ongoingNotifSwitchChecked = ongoingNotifSwitchChecked;
        this.locationSettingsLayoutVisible = locationSettingsLayoutVisible;
        this.locationText = locationText;
        this.updateButtonVisible = updateButtonVisible;
        this.controlButtonsVisible = controlButtonsVisible;
        this.wifiSsidsList = wifiSsidsList;
    }

    public String getuModUUID() {
        return uModUUID;
    }

    public void setuModUUID(String uModUUID) {
        this.uModUUID = uModUUID;
    }

    public String getAliasText() {
        return aliasText;
    }

    public void setAliasText(String aliasText) {
        this.aliasText = aliasText;
    }

    public String getWifiSSIDText() {
        return wifiSSIDText;
    }

    public void setWifiSSIDText(String wifiSSIDText) {
        this.wifiSSIDText = wifiSSIDText;
    }

    public boolean isAdminLayoutVisible() {
        return adminLayoutVisible;
    }

    public void setAdminLayoutVisible(boolean adminLayoutVisible) {
        this.adminLayoutVisible = adminLayoutVisible;
    }

    public String getuModSysInfoText() {
        return uModSysInfoText;
    }

    public void setuModSysInfoText(String uModSysInfoText) {
        this.uModSysInfoText = uModSysInfoText;
    }

    public String getWifiPasswordText() {
        return wifiPasswordText;
    }

    public void setWifiPasswordText(String wifiPasswordText) {
        this.wifiPasswordText = wifiPasswordText;
    }

    public String getConnectionStatusText() {
        return connectionStatusText;
    }

    public void setConnectionStatusText(String connectionStatusText) {
        this.connectionStatusText = connectionStatusText;
    }

    public boolean isWifiSettingsVisible() {
        return wifiSettingsVisible;
    }

    public void setWifiSettingsVisible(boolean wifiSettingsVisible) {
        this.wifiSettingsVisible = wifiSettingsVisible;
    }

    public boolean isOngoingNotifSwitchChecked() {
        return ongoingNotifSwitchChecked;
    }

    public void setOngoingNotifSwitchChecked(boolean ongoingNotifSwitchChecked) {
        this.ongoingNotifSwitchChecked = ongoingNotifSwitchChecked;
    }

    public boolean isUpdateButtonVisible() {
        return updateButtonVisible;
    }

    public void setUpdateButtonVisible(boolean updateButtonVisible) {
        this.updateButtonVisible = updateButtonVisible;
    }

    public String getLocationText() {
        return locationText;
    }

    public void setLocationText(String locationText) {
        this.locationText = locationText;
    }

    public boolean isLocationSettingsLayoutVisible() {
        return locationSettingsLayoutVisible;
    }

    public void setLocationSettingsLayoutVisible(boolean locationSettingsLayoutVisible) {
        this.locationSettingsLayoutVisible = locationSettingsLayoutVisible;
    }

    public boolean isControlButtonsVisible() {
        return controlButtonsVisible;
    }

    public void setControlButtonsVisible(boolean controlButtonsVisible) {
        this.controlButtonsVisible = controlButtonsVisible;
    }

    public List<String> getWifiSsidsList() {
        return wifiSsidsList;
    }

    public void setWifiSsidsList(List<String> wifiSsidsList) {
        this.wifiSsidsList = wifiSsidsList;
    }
}
