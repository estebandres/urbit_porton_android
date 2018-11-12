package com.urbit_iot.porton.umods;

import com.google.common.base.Objects;

/**
 * Created by steve-urbit on 27/11/17.
 */

public abstract class UModViewModel {
    private String uModUUID;
    private String itemMainText;
    private String connectionTagText;
    private UModsFragment.UModViewModelColors connectionTagColor;
    private UModsFragment.UModViewModelColors connectionTagTextColor;
    private String gateStatusTagText;
    private UModsFragment.UModViewModelColors gateStatusTagColor;
    private UModsFragment.UModViewModelColors gateStatusTagTextColor;
    private String timeText;
    private boolean timeTextVisible;
    private boolean ongoingNotifIndicatorOn;
    private boolean ongoingNotifVisible;
    private String sliderText;
    private boolean sliderVisible;
    private boolean sliderEnabled;
    private boolean settingsButtonVisible;
    private UModsFragment.UModViewModelColors sliderBackgroundColor;
    private UModsFragment.UModViewModelColors sliderTextColor;
    private boolean moduleTagsVisible;


    public UModViewModel(String uModUUID, String itemMainText,
                         String connectionTagText,
                         UModsFragment.UModViewModelColors connectionTagColor,
                         UModsFragment.UModViewModelColors connectionTagTextColor,
                         String gateStatusTagText,
                         UModsFragment.UModViewModelColors gateStatusTagColor,
                         UModsFragment.UModViewModelColors gateStatusTagTextColor,
                         boolean moduleTagsVisible,
                         String timeText, boolean timeTextVisible,
                         boolean ongoingNotifIndicatorOn, boolean ongoingNotifVisible,
                         String sliderText, boolean sliderVisible, boolean sliderEnabled,
                         boolean settingsButtonVisible,
                         UModsFragment.UModViewModelColors sliderBackgroundColor,
                         UModsFragment.UModViewModelColors sliderTextColor) {
        this.uModUUID = uModUUID;
        this.itemMainText = itemMainText;
        this.connectionTagText = connectionTagText;
        this.connectionTagColor = connectionTagColor;
        this.connectionTagTextColor = connectionTagTextColor;
        this.gateStatusTagText = gateStatusTagText;
        this.gateStatusTagColor = gateStatusTagColor;
        this.gateStatusTagTextColor = gateStatusTagTextColor;
        this.timeText = timeText;
        this.timeTextVisible = timeTextVisible;
        this.ongoingNotifIndicatorOn = ongoingNotifIndicatorOn;
        this.ongoingNotifVisible = ongoingNotifVisible;
        this.sliderText = sliderText;
        this.sliderVisible = sliderVisible;
        this.sliderEnabled = sliderEnabled;
        this.settingsButtonVisible = settingsButtonVisible;
        this.sliderBackgroundColor = sliderBackgroundColor;
        this.sliderTextColor = sliderTextColor;
        this.moduleTagsVisible = moduleTagsVisible;
    }

    public abstract void onSlideCompleted(UModsContract.Presenter presenter);

    public abstract void onSettingsButtonClicked(UModsContract.Presenter presenter);

    public String getuModUUID() {
        return uModUUID;
    }

    public void setuModUUID(String uModUUID) {
        this.uModUUID = uModUUID;
    }

    public String getItemMainText() {
        return itemMainText;
    }

    public void setItemMainText(String itemMainText) {
        this.itemMainText = itemMainText;
    }

    public boolean isOngoingNotifIndicatorOn() {
        return ongoingNotifIndicatorOn;
    }

    public void setOngoingNotifIndicatorOn(boolean ongoingNotifIndicatorOn) {
        this.ongoingNotifIndicatorOn = ongoingNotifIndicatorOn;
    }

    public String getSliderText() {
        return sliderText;
    }

    public void setSliderText(String sliderText) {
        this.sliderText = sliderText;
    }

    public boolean isSliderVisible() {
        return sliderVisible;
    }

    public void setSliderVisible(boolean sliderVisible) {
        this.sliderVisible = sliderVisible;
    }

    public boolean isSettingsButtonVisible() {
        return settingsButtonVisible;
    }

    public void setSettingsButtonVisible(boolean settingsButtonVisible) {
        this.settingsButtonVisible = settingsButtonVisible;
    }


    public UModsFragment.UModViewModelColors getSliderBackgroundColor() {
        return sliderBackgroundColor;
    }

    public void setSliderBackgroundColor(UModsFragment.UModViewModelColors sliderBackgroundColor) {
        this.sliderBackgroundColor = sliderBackgroundColor;
    }

    public UModsFragment.UModViewModelColors getSliderTextColor() {
        return sliderTextColor;
    }

    public void setSliderTextColor(UModsFragment.UModViewModelColors sliderTextColor) {
        this.sliderTextColor = sliderTextColor;
    }

    public boolean isSliderEnabled() {
        return sliderEnabled;
    }

    public void setSliderEnabled(boolean sliderEnabled) {
        this.sliderEnabled = sliderEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UModViewModel uModModel = (UModViewModel) o;
        return Objects.equal(uModUUID, uModModel.uModUUID);
    }

    public String getConnectionTagText() {
        return connectionTagText;
    }

    public void setConnectionTagText(String connectionTagText) {
        this.connectionTagText = connectionTagText;
    }

    public UModsFragment.UModViewModelColors getConnectionTagColor() {
        return connectionTagColor;
    }

    public void setConnectionTagColor(UModsFragment.UModViewModelColors connectionTagColor) {
        this.connectionTagColor = connectionTagColor;
    }

    public String getGateStatusTagText() {
        return gateStatusTagText;
    }

    public void setGateStatusTagText(String gateStatusTagText) {
        this.gateStatusTagText = gateStatusTagText;
    }

    public UModsFragment.UModViewModelColors getGateStatusTagColor() {
        return gateStatusTagColor;
    }

    public void setGateStatusTagColor(UModsFragment.UModViewModelColors gateStatusTagColor) {
        this.gateStatusTagColor = gateStatusTagColor;
    }


    public boolean isOngoingNotifVisible() {
        return ongoingNotifVisible;
    }

    public void setOngoingNotifVisible(boolean ongoingNotifVisible) {
        this.ongoingNotifVisible = ongoingNotifVisible;
    }

    public String getTimeText() {
        return timeText;
    }

    public void setTimeText(String timeText) {
        this.timeText = timeText;
    }

    public boolean isTimeTextVisible() {
        return timeTextVisible;
    }

    public void setTimeTextVisible(boolean timeTextVisible) {
        this.timeTextVisible = timeTextVisible;
    }

    public UModsFragment.UModViewModelColors getConnectionTagTextColor() {
        return connectionTagTextColor;
    }

    public void setConnectionTagTextColor(UModsFragment.UModViewModelColors connectionTagTextColor) {
        this.connectionTagTextColor = connectionTagTextColor;
    }

    public UModsFragment.UModViewModelColors getGateStatusTagTextColor() {
        return gateStatusTagTextColor;
    }

    public void setGateStatusTagTextColor(UModsFragment.UModViewModelColors gateStatusTagTextColor) {
        this.gateStatusTagTextColor = gateStatusTagTextColor;
    }

    @Override
    public String toString() {
        return "UModViewModel{" +
                ", uModUUID='" + uModUUID + '\'' +
                ", itemMainText='" + itemMainText + '\'' +
                ", connectionTagText='" + connectionTagText + '\'' +
                ", connectionTagColor=" + connectionTagColor +
                ", gateStatusTagText='" + gateStatusTagText + '\'' +
                ", gateStatusTagColor=" + gateStatusTagColor +
                ", ongoingNotifIndicatorOn=" + ongoingNotifIndicatorOn +
                ", ongoingNotifVisible=" + ongoingNotifVisible +
                ", sliderText='" + sliderText + '\'' +
                ", sliderVisible=" + sliderVisible +
                ", sliderEnabled=" + sliderEnabled +
                ", settingsButtonVisible=" + settingsButtonVisible +
                ", sliderBackgroundColor=" + sliderBackgroundColor +
                ", sliderTextColor=" + sliderTextColor +
                '}';
    }

    public boolean isModuleTagsVisible() {
        return moduleTagsVisible;
    }

    public void setModuleTagsVisible(boolean moduleTagsVisible) {
        this.moduleTagsVisible = moduleTagsVisible;
    }
}
