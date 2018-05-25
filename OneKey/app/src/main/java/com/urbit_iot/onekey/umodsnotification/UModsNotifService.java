package com.urbit_iot.onekey.umodsnotification;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.urbit_iot.onekey.OneKeyApplication;
import com.urbit_iot.onekey.util.GlobalConstants;



import javax.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;

public class UModsNotifService extends Service{
    public static String UMOD_UUID = "UMOD_UUID";

    private boolean serviceWasAlreadyStarted = false;

    @Inject
    UModsNotifPresenter mPresenter;

    private NotificationViewsHandler mNotificationViewsHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SERVICE", "created");
        this.mNotificationViewsHandler = new NotificationViewsHandler(this);
        OneKeyApplication oneKeyApplication = (OneKeyApplication) getApplication();
        DaggerUModsNotifComponent.builder()
                .uModsRepositoryComponent(oneKeyApplication.getUModsRepositoryComponentSingleton())
                .schedulerProviderComponent(oneKeyApplication.getSchedulerProviderComponentSingleton())
                .uModsNotifPresenterModule(new UModsNotifPresenterModule(this.mNotificationViewsHandler))
                .build()
                .inject(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mPresenter.unsubscribe();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SERVICE", "StartCommand" + intent.getAction() + Thread.currentThread().getName());
        Observable.just(intent)
                .doOnNext(this::processActionPendingIntent)
                .subscribeOn(Schedulers.io())
                .subscribe();
        return START_STICKY;
    }

    private void processActionPendingIntent(Intent intent){
        String action = intent.getAction();
        if (action==null){
            action="";
        }
        Log.d("SERVICE", "Process Intent" + Thread.currentThread().getName());

        String uModUUID = null;

        switch (action){
            case GlobalConstants.ACTION.STARTFOREGROUND:
                //Toast.makeText(this, "STARTED!!", Toast.LENGTH_SHORT).show();
                if (!this.serviceWasAlreadyStarted){
                    this.mPresenter.subscribe();
                    this.serviceWasAlreadyStarted = true;
                }
                break;
            case GlobalConstants.ACTION.TRIGGER:
                uModUUID = intent.getStringExtra(UMOD_UUID);
                this.mPresenter.triggerUMod(uModUUID);
                //Toast.makeText(this, "ACTION UMOD " + uModUUID, Toast.LENGTH_SHORT).show();
                break;
            case GlobalConstants.ACTION.REQUEST_ACCESS:
                uModUUID = intent.getStringExtra(UMOD_UUID);
                this.mPresenter.triggerUMod(uModUUID);
                //Toast.makeText(this, "ACTION UMOD " + uModUUID, Toast.LENGTH_SHORT).show();
                break;
            case GlobalConstants.ACTION.UNLOCK:
                //Toast.makeText(this, "UNLOCKED", Toast.LENGTH_SHORT).show();
                this.mPresenter.lockUModOperation();
                break;
            case GlobalConstants.ACTION.BACK_UMOD:
                this.mPresenter.previousUMod();
                //Toast.makeText(this, "BACK UMOD", Toast.LENGTH_SHORT).show();
                break;
            case GlobalConstants.ACTION.NEXT_UMOD:
                this.mPresenter.nextUMod();
                //Toast.makeText(this, "NEXT UMOD", Toast.LENGTH_SHORT).show();
                break;
            case GlobalConstants.ACTION.UPDATE_UMODS:
                //Toast.makeText(this, "UPDATE UMOD", Toast.LENGTH_SHORT).show();
                this.mPresenter.loadUMods(true);
                break;
            case GlobalConstants.ACTION.WIFI_CONNECTED:
                this.mPresenter.wifiIsOn();
                break;
            case GlobalConstants.ACTION.WIFI_UNUSABLE:
                Log.d("UMOD_SERVICE", "LAUNCH WIFI SETTINGS");
                this.mPresenter.wifiIsOff();
                break;
            case GlobalConstants.ACTION.LAUNCH_WIFI_SETTINGS:
                Log.d("UMOD_SERVICE", "LAUNCH WIFI SETTINGS");
                Intent closeNotificationsTreyIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                sendBroadcast(closeNotificationsTreyIntent);
                Intent androidWifiSettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                androidWifiSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(androidWifiSettingsIntent);
                break;
        }
    }

    /*
    private boolean wifiState(){
        final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

    }
    */
}
