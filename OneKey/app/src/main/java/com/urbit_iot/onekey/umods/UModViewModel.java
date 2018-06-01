package com.urbit_iot.onekey.umods;

import com.google.common.base.Objects;
import com.urbit_iot.onekey.data.UMod;

/**
 * Created by steve-urbit on 27/11/17.
 */

public abstract class UModViewModel {
    private UMod uMod;
    private String uModUUID;
    private UModsPresenter presenter;
    private String itemMainText;
    private String itemLowerText;
    private boolean ongoingNotifIndicatorOn;
    private boolean ongoingNotifVisible;
    private String sliderText;
    private boolean sliderVisible;
    private boolean sliderEnabled;
    private boolean itemOnClickListenerEnabled;
    private UModsFragment.UModViewModelColors itemBackgroundColor;
    private UModsFragment.UModViewModelColors lowerTextColor;
    private UModsFragment.UModViewModelColors sliderBackgroundColor;
    private UModsFragment.UModViewModelColors sliderTextColor;


    public UModViewModel(UMod uMod, String uModUUID, UModsPresenter presenter, String itemMainText,
                         String itemLowerText, boolean ongoingNotifIndicatorOn, boolean ongoingNotifVisible,
                         String sliderText, boolean sliderVisible,
                         boolean sliderEnabled, boolean itemOnClickListenerEnabled,
                         UModsFragment.UModViewModelColors lowerTextColor,
                         UModsFragment.UModViewModelColors sliderBackgroundColor,
                         UModsFragment.UModViewModelColors sliderTextColor) {
        this.uMod = uMod;
        this.uModUUID = uModUUID;
        this.presenter = presenter;
        this.itemMainText = itemMainText;
        this.itemLowerText = itemLowerText;
        this.ongoingNotifIndicatorOn = ongoingNotifIndicatorOn;
        this.ongoingNotifVisible = ongoingNotifVisible;
        this.sliderText = sliderText;
        this.sliderVisible = sliderVisible;
        this.sliderEnabled = sliderEnabled;
        this.itemOnClickListenerEnabled = itemOnClickListenerEnabled;
        this.lowerTextColor = lowerTextColor;
        this.sliderBackgroundColor = sliderBackgroundColor;
        this.sliderTextColor = sliderTextColor;
    }

    public abstract void onSlideCompleted();

    public abstract void onButtonToggled(Boolean toggleState);

    public abstract void onItemClicked();

    public String getuModUUID() {
        return uModUUID;
    }

    public void setuModUUID(String uModUUID) {
        this.uModUUID = uModUUID;
    }

    public UModsPresenter getPresenter() {
        return presenter;
    }

    public void setPresenter(UModsPresenter presenter) {
        this.presenter = presenter;
    }

    public String getItemMainText() {
        return itemMainText;
    }

    public void setItemMainText(String itemMainText) {
        this.itemMainText = itemMainText;
    }

    public String getItemLowerText() {
        return itemLowerText;
    }

    public void setItemLowerText(String itemLowerText) {
        this.itemLowerText = itemLowerText;
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

    public boolean isItemOnClickListenerEnabled() {
        return itemOnClickListenerEnabled;
    }

    public void setItemOnClickListenerEnabled(boolean itemOnClickListenerEnabled) {
        this.itemOnClickListenerEnabled = itemOnClickListenerEnabled;
    }

    public UMod getuMod() {
        return uMod;
    }

    public void setuMod(UMod uMod) {
        this.uMod = uMod;
    }


    public UModsFragment.UModViewModelColors getItemBackgroundColor() {
        return itemBackgroundColor;
    }

    public void setItemBackgroundColor(UModsFragment.UModViewModelColors itemBackgroundColor) {
        this.itemBackgroundColor = itemBackgroundColor;
    }

    public UModsFragment.UModViewModelColors getLowerTextColor() {
        return lowerTextColor;
    }

    public void setLowerTextColor(UModsFragment.UModViewModelColors lowerTextColor) {
        this.lowerTextColor = lowerTextColor;
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

    @Override
    public String toString() {
        return "UModViewModel{" +
                "uModHashCode=" + uMod.hashCode() +
                ", uModUUID='" + uModUUID + '\'' +
                ", presenterHashCode=" + presenter.hashCode() +
                ", itemMainText='" + itemMainText + '\'' +
                ", itemLowerText='" + itemLowerText + '\'' +
                ", ongoingNotifIndicatorOn=" + ongoingNotifIndicatorOn +
                ", ongoingNotifVisible=" + ongoingNotifVisible +
                ", sliderText='" + sliderText + '\'' +
                ", sliderVisible=" + sliderVisible +
                ", sliderEnabled=" + sliderEnabled +
                ", itemOnClickListenerEnabled=" + itemOnClickListenerEnabled +
                ", itemBackgroundColor=" + itemBackgroundColor +
                ", lowerTextColor=" + lowerTextColor +
                ", sliderBackgroundColor=" + sliderBackgroundColor +
                ", sliderTextColor=" + sliderTextColor +
                '}';
    }

    public boolean isOngoingNotifVisible() {
        return ongoingNotifVisible;
    }

    public void setOngoingNotifVisible(boolean ongoingNotifVisible) {
        this.ongoingNotifVisible = ongoingNotifVisible;
    }
}
