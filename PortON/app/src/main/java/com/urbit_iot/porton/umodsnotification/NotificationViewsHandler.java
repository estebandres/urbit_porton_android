package com.urbit_iot.porton.umodsnotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.urbit_iot.porton.R;
import com.urbit_iot.porton.umods.UModsActivity;
import com.urbit_iot.porton.util.GlobalConstants;

import java.util.Random;


/**
 * Created by andresteve07 on 4/20/18.
 */

public class NotificationViewsHandler implements UModsNotifContract.View{

    private UModsNotifContract.Presenter mPresenter;
    private RemoteViews controlCollapsedViews;
    private RemoteViews noUModsFoundCollapsedViews;
    private RemoteViews unconnectedPhoneCollapsedViews;

    private RemoteViews noConfiguredUModsCollapsedViews;
    private RemoteViews allUModsAreNotifDisabledCollapsedViews;

    public static final String CHANNEL_ID = GlobalConstants.NOTIFICATIONS_CHANNEL_ID;
    private String currentChannelID;
    //private RemoteViews expandedViews;

    private Notification mNotification;
    //private Notification.Builder mNotificationBuilder;
    private NotificationManager notificationManager;
    private boolean lockState;
    //private DisplayMetrics metrics;

    private String uModUUID;
    private String uModAlias;
    private String intentAction;

    @NonNull
    private Context mContext;

    public NotificationViewsHandler(@NonNull Context mContext) {
        this.mContext = mContext;
        this.notificationManager = (NotificationManager) this.mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        this.setupControlCollapsedView();
        this.setupNoUModsFoundCollapsedView();
        this.setupUnconnectedPhoneCollapsedViews();
        this.setupNoConfiguredUModsRemoteViews();
        this.setupAllUModsAreNotifDisabledRemoteViews();
        this.setNotificationViews(this.controlCollapsedViews);

        this.lockState = true;
        /*
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            this.metrics = mContext.getResources().getDisplayMetrics();
        */

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            this.currentChannelID = this.createChannel();
        }

    }

    private String createChannel() {
        CharSequence channelName = this.mContext.getString(R.string.channel_name);
        String channelDescription = this.mContext.getString(R.string.channel_description);
        int importance = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_ID, channelName, importance);
            //notificationChannel.setSound(null,null);
            notificationChannel.setDescription(channelDescription);
            this.notificationManager.createNotificationChannel(notificationChannel);
        }
        return CHANNEL_ID;
    }

    private void setupAllUModsAreNotifDisabledRemoteViews() {
        this.allUModsAreNotifDisabledCollapsedViews = new RemoteViews(getPackageName(),
                R.layout.ongoing_notification__notif_disabled_for_all_umods);
        Intent openAppIntent = new Intent(this.mContext, UModsActivity.class);

        openAppIntent.setAction(GlobalConstants.ACTION.MAIN);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(this.mContext, 0,
                openAppIntent, 0);

        this.allUModsAreNotifDisabledCollapsedViews.setOnClickPendingIntent(R.id.ongoing_notification__notification_layout, openAppPendingIntent);

    }

    private void setupNoConfiguredUModsRemoteViews() {
        this.noConfiguredUModsCollapsedViews = new RemoteViews(getPackageName(),
                R.layout.ongoing_notification__no_configured_umods);

        Intent updateUModsIntent = new Intent(this.mContext, UModsNotifService.class);
        updateUModsIntent.setAction(GlobalConstants.ACTION.REFRESH_UMODS);
        PendingIntent updateUModsPendingIntent = PendingIntent.getService(this.mContext, 0,
                updateUModsIntent, 0);

        this.noConfiguredUModsCollapsedViews.setOnClickPendingIntent(R.id.notif_search_button, updateUModsPendingIntent);


    }



    /*
    @Override
    public boolean isWiFiConnected(){
        //WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                return activeNetworkInfo.isConnected();
            } else {
                return false;
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo != null){
                return networkInfo.isConnected();
            } else {
                return false;
            }
        }
    }
    */

    /*
    public boolean isWiFiConnected(){
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
        return wifiManager != null && wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
    }
    */

    public boolean isWiFiConnected(){
        return ConnectivityReceiver.isNetworkAvailable(this.mContext);
    }


    @Override
    public void showLoadProgress() {
        this.controlCollapsedViews.setTextViewText(R.id.progress_reason_text,
                this.mContext.getResources().getString(R.string.notif_loading_progress_subtext));
        this.showProgressView();
    }

    @Override
    public void showAllUModsAreNotifDisabled() {
        this.setNotificationViews(this.allUModsAreNotifDisabledCollapsedViews);
    }

    @Override
    public void showNoConfiguredUMods() {
        this.setNotificationViews(this.noConfiguredUModsCollapsedViews);
    }

    @Override
    public void showTriggerProgress() {
        this.controlCollapsedViews.setTextViewText(R.id.progress_reason_text,
                this.mContext.getResources().getString(R.string.notif_triggering_progress_subtext));
        this.showProgressView();
    }

    @Override
    public void showAccessRequestProgress() {
        this.controlCollapsedViews.setTextViewText(R.id.progress_reason_text,
                this.mContext.getResources().getString(R.string.notif_accessing_progress_subtext));
        this.showProgressView();
    }

    private void showProgressView(){
        this.controlCollapsedViews.setViewVisibility(R.id.progress_layout, View.VISIBLE);
        this.controlCollapsedViews.setViewVisibility(R.id.notif_control_buttons, View.GONE);
        updateNotification();
    }

    @Override
    public void hideProgressView() {
        this.controlCollapsedViews.setViewVisibility(R.id.progress_layout, View.GONE);
        this.controlCollapsedViews.setViewVisibility(R.id.notif_control_buttons, View.VISIBLE);
        updateNotification();
    }

    private void setupUnconnectedPhoneCollapsedViews() {
        this.unconnectedPhoneCollapsedViews = new RemoteViews(getPackageName(),
                R.layout.ongoing_notification__unconnected_phone);
        Intent launchWifiIntent = new Intent(this.mContext, UModsNotifService.class);
        launchWifiIntent.setAction(GlobalConstants.ACTION.LAUNCH_WIFI_SETTINGS);
        PendingIntent launchWifiPendingIntent = PendingIntent.getService(this.mContext, 0,
                launchWifiIntent, 0);

        this.unconnectedPhoneCollapsedViews.setOnClickPendingIntent(R.id.notif_wifi_button, launchWifiPendingIntent);
    }

    private void setupNoUModsFoundCollapsedView() {
        this.noUModsFoundCollapsedViews = new RemoteViews(getPackageName(),
                R.layout.ongoing_notification__no_umods_found);

        Intent updateUModsIntent = new Intent(this.mContext, UModsNotifService.class);
        updateUModsIntent.setAction(GlobalConstants.ACTION.REFRESH_UMODS);
        PendingIntent updateUModsPendingIntent = PendingIntent.getService(this.mContext, 0,
                updateUModsIntent, 0);

        this.noUModsFoundCollapsedViews.setOnClickPendingIntent(R.id.notif_search_button, updateUModsPendingIntent);
    }

    private void buildNotification(){

    }

    private void buildNotificationByViewModel(){

    }

    @Override
    public void toggleLockState(){
        Log.d("notif_view", "TOGGLE");
        this.lockState = !this.lockState;
    }

    /*
    private void setImage(RemoteViews remoteView, @IdRes int imageViewId, @DrawableRes int imageId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            remoteView.setImageViewResource(imageViewId, imageId);
        } else {
            remoteView.setImageViewBitmap(
                    imageViewId,
                    BitmapUtil.toBitmap(ContextCompat.getDrawable(this.mContext, imageId), metrics)
            );
        }
    }
    */

    @Override
    public void setTitleText(String newTitle) {
        this.controlCollapsedViews.setTextViewText(R.id.notif_title_text, newTitle);
        updateNotification();
    }

    @Override
    public void setSecondaryText(String newText) {

    }

    @Override
    public void showUnlockedView() {
        setLockState(false);
        enableOperationButton();
        controlCollapsedViews.setImageViewResource(R.id.notif_lock_button,R.drawable.ic_lock_open_cyan_24dp);
        //Bitmap unlockedIconBitmap = BitmapFactory.decodeResource(this.mContext.getResources(), R.drawable.ic_lock_open_gray_24dp);
        //controlCollapsedViews.setImageViewBitmap(R.id.notif_lock_button,unlockedIconBitmap);
        //controlCollapsedViews.setViewVisibility(R.id.notif_lock_button, View.INVISIBLE);
        updateNotification();
    }

    @Override
    public void showLockedView() {
        //Bitmap unlockedIconBitmap = BitmapFactory.decodeResource(this.mContext.getResources(), R.drawable.ic_lock_open_gray_24dp);
        //controlCollapsedViews.setImageViewBitmap(R.id.notif_lock_button,unlockedIconBitmap);
        //controlCollapsedViews.setImageViewResource(R.id.notif_lock_button,R.drawable.lock_unlock_icon);
        setLockState(true);
        disableOperationButton();
        controlCollapsedViews.setImageViewResource(R.id.notif_lock_button,R.drawable.ic_lock_outline_white_24dp);
        updateNotification();
    }

    @Override
    public void enableOperationButton() {
        Intent actionUModIntent = new Intent(this.mContext, UModsNotifService.class);
        actionUModIntent.setAction(this.getIntentAction());
        actionUModIntent.putExtra(UModsNotifService.UMOD_UUID,this.uModUUID);

        PendingIntent actionUModPendingIntent = PendingIntent.getService(this.mContext, (int) System.currentTimeMillis(),
                actionUModIntent, 0);
        controlCollapsedViews.setImageViewResource(R.id.notif_action_button, R.drawable.ic_eject_white_24dp);
        controlCollapsedViews.setOnClickPendingIntent(R.id.notif_action_button, actionUModPendingIntent);
        //this.notificationManager.notify(GlobalConstants.NOTIFICATION_ID.FOREGROUND_SERVICE,this.mNotification);
    }

    @Override
    public void disableOperationButton() {

        Intent actionUModIntent = new Intent(this.mContext, UModsNotifService.class);
        PendingIntent actionUModPendingIntent = PendingIntent.getService(this.mContext, (int) System.currentTimeMillis(),
                actionUModIntent, 0);
        //TODO change icon with setImageIcon...Icon.createWithResource...
        controlCollapsedViews.setImageViewResource(R.id.notif_action_button, R.drawable.ic_eject_gray_24dp);
        controlCollapsedViews.setOnClickPendingIntent(R.id.notif_action_button, actionUModPendingIntent);
        //this.notificationManager.notify(GlobalConstants.NOTIFICATION_ID.FOREGROUND_SERVICE,this.mNotification);
        /*
        controlCollapsedViews.setViewVisibility(R.id.notif_action_button, View.INVISIBLE);
        this.notificationManager.notify(GlobalConstants.NOTIFICATION_ID.FOREGROUND_SERVICE,this.mNotification);
        */
    }

    @Override
    public void showUnconnectedPhone() {
        Log.d("NOTIF", "UNCONNECTED");
        this.setNotificationViews(this.unconnectedPhoneCollapsedViews);
    }

    @Override
    public void showNoUModsFound() {
        this.setNotificationViews(this.noUModsFoundCollapsedViews);
    }

    @Override
    public void showSingleUModControl() {

    }

    private String getPackageName(){
        //return this.getClass().getPackage().getName();
        return this.mContext.getPackageName();
    }

    private void setUModUUIDAndAlias(String uModUUID, String uModAlias){
        this.uModAlias = uModAlias;
        this.uModUUID = uModUUID;
    }

    private void changeMainTextAndSetDumbActionIntent(String uModUUID, String uModAlias){
        this.setUModUUIDAndAlias(uModUUID, uModAlias);

        Intent actionUModIntent = new Intent(this.mContext, UModsNotifService.class);
        //actionUModIntent.putExtra(UModsNotifService.UMOD_UUID,uModUUID);
        //actionUModIntent.setAction(GlobalConstants.ACTION.ACTION_UMOD);
        PendingIntent actionUModPendingIntent = PendingIntent.getService(this.mContext, (int) System.currentTimeMillis(),
                actionUModIntent, 0);

        this.mNotification.contentView = this.controlCollapsedViews;
        this.controlCollapsedViews.setTextViewText(R.id.notif_title_text, this.uModAlias);
        this.controlCollapsedViews.setOnClickPendingIntent(R.id.notif_action_button, actionUModPendingIntent);
        //this.controlCollapsedViews.setImageViewResource(R.id.notif_action_button, iconViewId);
        updateNotification();
    }

    private void updateNotification(){
        this.notificationManager.notify(GlobalConstants.NOTIFICATION_ID,this.mNotification);
    }

    private void setupControlCollapsedView(){
        this.controlCollapsedViews = new RemoteViews(getPackageName(),
                R.layout.ongoing_notification__umod_control_dark);

        Intent openAppIntent = new Intent(this.mContext, UModsActivity.class);
        openAppIntent.setAction(GlobalConstants.ACTION.MAIN);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(this.mContext, 0,
                openAppIntent, 0);


        Intent updateUModsIntent = new Intent(this.mContext, UModsNotifService.class);
        updateUModsIntent.setAction(GlobalConstants.ACTION.REFRESH_UMODS);
        PendingIntent updateUModsPendingIntent = PendingIntent.getService(this.mContext, 0,
                updateUModsIntent, 0);

        Intent actionUModIntent = new Intent(this.mContext, UModsNotifService.class);
        actionUModIntent.setAction(GlobalConstants.ACTION.ACTION_UMOD);
        PendingIntent actionUModPendingIntent = PendingIntent.getService(this.mContext, 0,
                actionUModIntent, 0);

        Intent nextUModIntent = new Intent(this.mContext, UModsNotifService.class);
        nextUModIntent.setAction(GlobalConstants.ACTION.NEXT_UMOD);
        PendingIntent nextUModPendingIntent = PendingIntent.getService(this.mContext, 0,
                nextUModIntent, 0);


        Intent backUModIntent = new Intent(this.mContext, UModsNotifService.class);
        backUModIntent.setAction(GlobalConstants.ACTION.BACK_UMOD);
        PendingIntent backUModPendingIntent = PendingIntent.getService(this.mContext, 0,
                backUModIntent, 0);


        Intent unlockUModIntent = new Intent(this.mContext, UModsNotifService.class);
        unlockUModIntent.setAction(GlobalConstants.ACTION.UNLOCK);
        PendingIntent unlockUModPendingIntent = PendingIntent.getService(this.mContext, 0,
                unlockUModIntent, 0);

        /*
        Intent closeIntent = new Intent(this.mContext, UModsNotifService.class);
        closeIntent.setAction(GlobalConstants.ACTION.STOPFOREGROUND);
        PendingIntent pcloseIntent = PendingIntent.getService(this.mContext, 0,
                closeIntent, 0);
         */

        this.controlCollapsedViews.setOnClickPendingIntent(R.id.notif_next_button, nextUModPendingIntent);

        this.controlCollapsedViews.setOnClickPendingIntent(R.id.notif_update_button, updateUModsPendingIntent);

        this.controlCollapsedViews.setOnClickPendingIntent(R.id.notif_back_button, backUModPendingIntent);

        this.controlCollapsedViews.setOnClickPendingIntent(R.id.notif_lock_button, unlockUModPendingIntent);

        this.controlCollapsedViews.setOnClickPendingIntent(R.id.notif_big_icon, openAppPendingIntent);

        //this.controlCollapsedViews.setOnClickPendingIntent(R.id.notif_action_button, actionUModPendingIntent);

        //controlCollapsedViews.setOnClickPendingIntent(R.id.status_bar_collapse, pcloseIntent);
    }

    private void setNotificationViews(RemoteViews views){
        Intent notificationIntent = new Intent(this.mContext, UModsActivity.class);
        notificationIntent.setAction(GlobalConstants.ACTION.MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this.mContext, 0,
                notificationIntent, 0);

        /*
        this.mNotification = new Notification.Builder(this.mContext).build();
        mNotification.contentView = views;
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        mNotification.icon = R.drawable.ic_app_logo__white_uncircled_untexted;
        mNotification.visibility = Notification.VISIBILITY_PUBLIC;
        mNotification.priority = Notification.PRIORITY_MAX;
        //mNotification.contentIntent = pendingIntent;
        */
        Notification.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(this.mContext,this.currentChannelID);
        } else {
            notificationBuilder = new Notification.Builder(this.mContext);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.mNotification =  notificationBuilder
                    .setSmallIcon(R.drawable.ic_notification_bar_logo_white)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContent(views)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .build();
        } else {
            this.mNotification =  notificationBuilder
                    .setSmallIcon(R.drawable.ic_notification_bar_logo_white)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContent(views)
                    .setPriority(Notification.PRIORITY_MAX)
                    .build();
        }
        dispatchNotification();
    }

    @Override
    public void showUModSelectAndControl() {

    }

    @Override
    public void showSelectionControls() {
        this.controlCollapsedViews.setViewVisibility(R.id.notif_navigation_buttons, View.VISIBLE);
        updateNotification();
    }

    @Override
    public void hideSelectionControls() {
        this.controlCollapsedViews.setViewVisibility(R.id.notif_navigation_buttons, View.GONE);
        updateNotification();
    }

    public void dispatchNotification(){
        ((Service) this.mContext).startForeground(GlobalConstants.NOTIFICATION_ID, this.mNotification);
    }

    @Override
    public void showRequestAccessView(String uModUUID, String uModAlias) {
        //TODO change layout to reflect request access behaviour
        this.setIntentAction(GlobalConstants.ACTION.TRIGGER);
        this.changeMainTextAndSetDumbActionIntent(uModUUID, uModAlias);
    }

    @Override
    public void showTriggerView(String uModUUID, String uModAlias) {
        //TODO change layout to reflect request access behaviour
        this.setIntentAction(GlobalConstants.ACTION.TRIGGER);
        this.changeMainTextAndSetDumbActionIntent(uModUUID, uModAlias);
    }

    @Override
    public void setPresenter(UModsNotifContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public boolean getLockState() {
        return this.lockState;
    }

    @Override
    public void setLockState(boolean lockState) {
        this.lockState = lockState;
    }


    public String getIntentAction() {
        return intentAction;
    }

    public void setIntentAction(String intentAction) {
        this.intentAction = intentAction;
    }
}
/*
public static class PreO {

        public static void createNotification(Service context) {
            // Create Pending Intents.
            PendingIntent piLaunchMainActivity = getLaunchActivityPI(context);
            PendingIntent piStopService = getStopServicePI(context);

            // Action to stop the service.
            NotificationCompat.Action stopAction =
                    new NotificationCompat.Action.Builder(
                            STOP_ACTION_ICON,
                            getNotificationStopActionText(context),
                            piStopService)
                            .build();

            // Create a mNotification.
            Notification mNotification =
                    new NotificationCompat.Builder(context)
                            .setContentTitle(getNotificationTitle(context))
                            .setContentText(getNotificationContent(context))
                            .setSmallIcon(SMALL_ICON)
                            .setContentIntent(piLaunchMainActivity)
                            .addAction(stopAction)
                            .setStyle(new NotificationCompat.BigTextStyle())
                            .build();

            context.startForeground(ONGOING_NOTIFICATION_ID, mNotification);
        }
    }

    public static class O {

        public static final String CHANNEL_ID = String.valueOf(getRandomNumber());

        public static void createNotification(Service context) {
            String channelId = createChannel(context);
            Notification mNotification = buildNotification(context, channelId);
            context.startForeground(ONGOING_NOTIFICATION_ID, mNotification);
        }

        private static Notification buildNotification(Service context, String channelId) {
            // Create Pending Intents.
            PendingIntent piLaunchMainActivity = getLaunchActivityPI(context);
            PendingIntent piStopService = getStopServicePI(context);

            // Action to stop the service.
            Notification.Action stopAction =
                    new Notification.Action.Builder(
                            STOP_ACTION_ICON,
                            getNotificationStopActionText(context),
                            piStopService)
                            .build();

            // Create a mNotification.
            return new Notification.Builder(context, channelId)
                    .setContentTitle(getNotificationTitle(context))
                    .setContentText(getNotificationContent(context))
                    .setSmallIcon(SMALL_ICON)
                    .setContentIntent(piLaunchMainActivity)
                    .setActions(stopAction)
                    .setStyle(new Notification.BigTextStyle())
                    .build();
        }

        @NonNull
        private static String createChannel(Service context) {
            // Create a channel.
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            CharSequence channelName = "Playback channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_ID, channelName, importance);
            notificationManager.createNotificationChannel(notificationChannel);
            return CHANNEL_ID;
        }
    }
 */