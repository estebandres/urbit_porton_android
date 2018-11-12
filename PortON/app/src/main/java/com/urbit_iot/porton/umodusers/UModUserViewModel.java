package com.urbit_iot.porton.umodusers;

import com.urbit_iot.porton.data.rpc.GetUsersRPC;

/**
 * Created by andresteve07 on 26/02/18.
 */

public abstract class UModUserViewModel {
    public enum LevelIcon{
        ADMIN_CROWN,
        REGULAR_UNLOCK,
        TEMPORAL_CLOCK
    }

    public enum LevelButtonImage{
        FULL_CROWN,
        CROSSED_CROWN
    }

    private GetUsersRPC.UserResult userResult;
    private UModUsersPresenter presenter;
    private String itemMainText;
    private boolean deleteButtonVisible;
    private boolean acceptButtonVisible;
    private boolean levelButtonVisible;
    private LevelButtonImage levelButtonImage;
    private boolean levelIconVisible;
    private LevelIcon levelIcon;

    public UModUserViewModel(GetUsersRPC.UserResult userResult,
                             UModUsersPresenter presenter,
                             String itemMainText,
                             boolean deleteButtonVisible,
                             boolean acceptButtonVisible,
                             boolean levelButtonVisible,
                             LevelButtonImage levelButtonImage,
                             boolean levelIconVisible,
                             LevelIcon levelIcon) {
        this.userResult = userResult;
        this.presenter = presenter;
        this.itemMainText = itemMainText;
        this.deleteButtonVisible = deleteButtonVisible;
        this.acceptButtonVisible = acceptButtonVisible;
        this.levelButtonVisible = levelButtonVisible;
        this.levelButtonImage = levelButtonImage;
        this.levelIconVisible = levelIconVisible;
        this.levelIcon = levelIcon;
    }

    public abstract void onAcceptButtonClicked();

    public abstract void onLevelButtonClicked();

    public abstract void onDeleteButtonClicked();

    public String getItemMainText() {
        return itemMainText;
    }

    public void setItemMainText(String itemMainText) {
        this.itemMainText = itemMainText;
    }

    public UModUsersPresenter getPresenter() {
        return presenter;
    }

    public void setPresenter(UModUsersPresenter presenter) {
        this.presenter = presenter;
    }

    public GetUsersRPC.UserResult getUserResult() {
        return userResult;
    }

    public void setUserResult(GetUsersRPC.UserResult userResult) {
        this.userResult = userResult;
    }

    public boolean isDeleteButtonVisible() {
        return deleteButtonVisible;
    }

    public void setDeleteButtonVisible(boolean deleteButtonVisible) {
        this.deleteButtonVisible = deleteButtonVisible;
    }

    public boolean isAcceptButtonVisible() {
        return acceptButtonVisible;
    }

    public void setAcceptButtonVisible(boolean acceptButtonVisible) {
        this.acceptButtonVisible = acceptButtonVisible;
    }

    public boolean isLevelButtonVisible() {
        return levelButtonVisible;
    }

    public void setLevelButtonVisible(boolean levelButtonVisible) {
        this.levelButtonVisible = levelButtonVisible;
    }

    public LevelButtonImage getLevelButtonImage() {
        return levelButtonImage;
    }

    public void setLevelButtonImage(LevelButtonImage levelButtonImage) {
        this.levelButtonImage = levelButtonImage;
    }

    public boolean isLevelIconVisible() {
        return levelIconVisible;
    }

    public void setLevelIconVisible(boolean levelIconVisible) {
        this.levelIconVisible = levelIconVisible;
    }

    public LevelIcon getLevelIcon() {
        return levelIcon;
    }

    public void setLevelIcon(LevelIcon levelIcon) {
        this.levelIcon = levelIcon;
    }
}
