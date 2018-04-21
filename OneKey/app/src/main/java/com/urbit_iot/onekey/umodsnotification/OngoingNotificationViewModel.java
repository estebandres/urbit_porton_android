package com.urbit_iot.onekey.umodsnotification;

/**
 * Created by andresteve07 on 5/7/18.
 */

public class OngoingNotificationViewModel {
    public String mainText;
    public String secondaryText;
    public String lastTimeActioned;
    public String notificationBackgroundColor;

    public enum NotificationType{
        TRIGGER_TYPE,
        REQUEST_TYPE,
        UNCONNECT_TYPE,
        NO_RESULTS_TYPE,
    }
}
