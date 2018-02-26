package com.urbit_iot.onekey.usersxumod;

import com.urbit_iot.onekey.data.rpc.GetUsersRPC;

/**
 * Created by andresteve07 on 26/02/18.
 */

public abstract class UModUserViewModel {
    private GetUsersRPC.UserResult userResult;
    private UModUsersPresenter presenter;
    private String itemMainText;
    private String buttonText;
    private boolean checkboxChecked;
    private boolean checkboxVisible;

    public UModUserViewModel(
            GetUsersRPC.UserResult userResult,
            UModUsersPresenter presenter,
            String itemMainText,
            String buttonText,
            boolean checkboxChecked, boolean checkboxVisible) {
        this.userResult = userResult;
        this.presenter = presenter;
        this.itemMainText = itemMainText;
        this.buttonText = buttonText;
        this.checkboxChecked = checkboxChecked;
        this.checkboxVisible = checkboxVisible;
    }

    public abstract void onButtonClicked();

    public abstract void onCheckBoxClicked(Boolean cbChecked);

    public String getItemMainText() {
        return itemMainText;
    }

    public void setItemMainText(String itemMainText) {
        this.itemMainText = itemMainText;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public boolean isCheckboxChecked() {
        return checkboxChecked;
    }

    public void setCheckboxChecked(boolean checkboxChecked) {
        this.checkboxChecked = checkboxChecked;
    }
    public UModUsersPresenter getPresenter() {
        return presenter;
    }

    public void setPresenter(UModUsersPresenter presenter) {
        this.presenter = presenter;
    }

    public boolean isCheckboxVisible() {
        return checkboxVisible;
    }

    public void setCheckboxVisible(boolean checkboxVisible) {
        this.checkboxVisible = checkboxVisible;
    }

    public GetUsersRPC.UserResult getUserResult() {
        return userResult;
    }

    public void setUserResult(GetUsersRPC.UserResult userResult) {
        this.userResult = userResult;
    }
}
