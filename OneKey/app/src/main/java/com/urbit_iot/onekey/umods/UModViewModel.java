package com.urbit_iot.onekey.umods;

import android.graphics.Color;

import com.google.common.base.Objects;

/**
 * Created by steve-urbit on 27/11/17.
 */

public abstract class UModViewModel {
    private String uModUUID;
    private UModsPresenter presenter;
    private String itemMainText;
    private String itemLowerText;
    private boolean checkboxChecked;
    private boolean checkboxVisible;
    private String buttonText;
    private boolean buttonVisible;
    private boolean itemOnClickListenerEnabled;
    private Color cardBackgroundColor;

    public UModViewModel(String uModUUID, UModsPresenter presenter, String itemMainText,
                         String itemLowerText, boolean checkboxChecked, boolean checkboxVisible,
                         String buttonText, boolean buttonVisible,
                         boolean itemOnClickListenerEnabled) {
        this.uModUUID = uModUUID;
        this.presenter = presenter;
        this.itemMainText = itemMainText;
        this.itemLowerText = itemLowerText;
        this.checkboxChecked = checkboxChecked;
        this.checkboxVisible = checkboxVisible;
        this.buttonText = buttonText;
        this.buttonVisible = buttonVisible;
        this.itemOnClickListenerEnabled = itemOnClickListenerEnabled;
    }

    public abstract void onButtonClicked();

    public abstract void onCheckBoxClicked(Boolean cbChecked);

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

    public boolean isCheckboxChecked() {
        return checkboxChecked;
    }

    public void setCheckboxChecked(boolean checkboxChecked) {
        this.checkboxChecked = checkboxChecked;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public boolean isButtonVisible() {
        return buttonVisible;
    }

    public void setButtonVisible(boolean buttonVisible) {
        this.buttonVisible = buttonVisible;
    }

    public boolean isItemOnClickListenerEnabled() {
        return itemOnClickListenerEnabled;
    }

    public void setItemOnClickListenerEnabled(boolean itemOnClickListenerEnabled) {
        this.itemOnClickListenerEnabled = itemOnClickListenerEnabled;
    }

    public Color getCardBackgroundColor() {
        return cardBackgroundColor;
    }

    public void setCardBackgroundColor(Color cardBackgroundColor) {
        this.cardBackgroundColor = cardBackgroundColor;
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
                "presenter=" + presenter +
                ", itemMainText='" + itemMainText + '\'' +
                ", itemLowerText='" + itemLowerText + '\'' +
                ", checkboxChecked=" + checkboxChecked +
                ", buttonText='" + buttonText + '\'' +
                ", buttonVisible=" + buttonVisible +
                ", itemOnClickListenerEnabled=" + itemOnClickListenerEnabled +
                ", cardBackgroundColor=" + cardBackgroundColor +
                '}';
    }

    public boolean isCheckboxVisible() {
        return checkboxVisible;
    }

    public void setCheckboxVisible(boolean checkboxVisible) {
        this.checkboxVisible = checkboxVisible;
    }
}
