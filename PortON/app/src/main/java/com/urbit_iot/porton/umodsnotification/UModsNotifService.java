package com.urbit_iot.porton.umodsnotification;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.util.GlobalConstants;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;


import javax.inject.Inject;

import rx.Observable;

public class UModsNotifService extends Service{
    public static String UMOD_UUID = "UMOD_UUID";
    public static boolean SERVICE_IS_ALIVE = false;

    private boolean serviceWasAlreadyStarted = false;

    @Inject
    BaseSchedulerProvider mSchedulerProvider;

    @Inject
    UModsNotifPresenter mPresenter;

    private ConnectivityReceiver connectivityReceiver;
    private NotificationViewsHandler mNotificationViewsHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SERVICE", "created");
        this.mNotificationViewsHandler = new NotificationViewsHandler(this);
        this.connectivityReceiver = new ConnectivityReceiver();
        PortONApplication portONApplication = (PortONApplication) getApplication();
        DaggerUModsNotifComponent.builder()
                .uModsRepositoryComponent(portONApplication.getUModsRepositoryComponentSingleton())
                .uModsNotifPresenterModule(new UModsNotifPresenterModule(this.mNotificationViewsHandler))
                .build()
                .inject(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        //registerReceiver(this.connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(this.connectivityReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mPresenter.unsubscribe();
        unregisterReceiver(this.connectivityReceiver);
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
                .subscribeOn(mSchedulerProvider.io())
                .subscribe();
        return START_NOT_STICKY;
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
                    SERVICE_IS_ALIVE = true;
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
            case GlobalConstants.ACTION.REFRESH_UMODS:
                //Toast.makeText(this, "UPDATE UMOD", Toast.LENGTH_SHORT).show();
                this.mPresenter.loadUMods(true);
                break;
            case GlobalConstants.ACTION.REFRESH_ON_CACHED:
                this.mPresenter.loadUMods(false);
                break;
            case GlobalConstants.ACTION.WIFI_CONNECTED:
                this.mPresenter.wifiIsOn();
                break;
            case GlobalConstants.ACTION.WIFI_UNUSABLE:
                Log.d("UMOD_SERVICE", "NO CONNECTION");
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
            case GlobalConstants.ACTION.SHUTDOWN_SERVICE:
                Log.d("UMOD_SERVICE", "SHUTTING DOWN SERVICE");
                SERVICE_IS_ALIVE = false;
                shutServiceDown();
                break;
        }
    }

    private void shutServiceDown() {
        stopSelf();
    }

    /*
    private boolean wifiState(){
        final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

    }
    */
}
