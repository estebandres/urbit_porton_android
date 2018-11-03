package com.urbit_iot.onekey.data.source.lan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.github.pwittchen.reactivewifi.ReactiveWifi;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;
import com.urbit_iot.onekey.util.schedulers.SchedulerProvider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by steve-urbit on 30/01/18.
 */

public class UModsWiFiScanner {
    @NonNull
    private final Context appContext;
    private AtomicBoolean scanInProgress;
    private PublishSubject<UMod> freshUModDnsScan;
    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    @Inject
    public UModsWiFiScanner(@NonNull Context appContext,
                            @NonNull BaseSchedulerProvider mSchedulerProvider) {
        this.appContext = appContext;
        this.mSchedulerProvider = mSchedulerProvider;
        this.scanInProgress = new AtomicBoolean(false);
        this.freshUModDnsScan = PublishSubject.create();
    }

    synchronized public Observable<UMod> browseWiFiForUMods() {
        return Observable.just(true)
                .map(aBoolean -> scanInProgress.compareAndSet(false,true))
                .flatMap(mutexWasAcquired -> {
                    if (mutexWasAcquired){
                        Log.d("wifi_scan", "AQC");
                        return observableWiFiScanResults();
                    } else {
                        Log.d("wifi_scan", "NOT AQC");
                        if (freshUModDnsScan.hasCompleted()){
                            Log.d("wifi_scan", "SUBJECT COMP");
                            freshUModDnsScan = PublishSubject.create();
                        }
                        return freshUModDnsScan.asObservable()
                                .takeUntil(Observable.timer(5000L, TimeUnit.MILLISECONDS))
                                .doOnError(throwable -> scanInProgress.compareAndSet(true, false))
                                .doOnUnsubscribe(() -> scanInProgress.compareAndSet(true, false))
                                .doOnTerminate(() -> scanInProgress.compareAndSet(true, false))
                                .doOnCompleted(() -> scanInProgress.compareAndSet(true, false));
                    }
                });
    }

    synchronized public Observable<UMod> browseWiFiForUMod(String uModUUID) {
        return Observable.just(true)
                .map(aBoolean -> scanInProgress.compareAndSet(false,true))
                .flatMap(mutexWasAcquired -> {
                    if (mutexWasAcquired){
                        Log.d("wifi_scan", "AQC");
                        return observableWiFiScanResults()
                                .filter(uMod -> uMod.getUUID().contains(uModUUID));//TODO improve filter using regex
                    } else {
                        Log.d("wifi_scan", "NOT AQC");
                        if (freshUModDnsScan.hasCompleted()){
                            Log.d("wifi_scan", "SUBJECT COMP");
                            freshUModDnsScan = PublishSubject.create();
                        }
                        return freshUModDnsScan.asObservable()
                                .filter(uMod -> uMod.getUUID().contains(uModUUID))//TODO improve filter using regex
                                .takeUntil(Observable.timer(5000L, TimeUnit.MILLISECONDS))
                                .doOnError(throwable -> scanInProgress.compareAndSet(true, false))
                                .doOnUnsubscribe(() -> scanInProgress.compareAndSet(true, false))
                                .doOnTerminate(() -> scanInProgress.compareAndSet(true, false))
                                .doOnCompleted(() -> scanInProgress.compareAndSet(true, false));
                    }
                });

    }

    private Observable<UMod> observableWiFiScanResults(){
        if (ActivityCompat.checkSelfPermission(this.appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this.appContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return Observable.error(new Exception("Unsatisfied WiFi permissions"));
        }
        return ReactiveWifi.observeWifiAccessPoints(this.appContext)
                .observeOn(mSchedulerProvider.io())
                .flatMap(Observable::from)
                .distinct((Func1<ScanResult, Object>) scanResult -> scanResult.SSID)
                .filter(scanResult -> scanResult.level > -75)
                .filter(scanResult -> scanResult.SSID.matches(GlobalConstants.URBIT_PREFIX + GlobalConstants.DEVICE_UUID_REGEX))
                .takeUntil(Observable.timer(5000L, TimeUnit.MILLISECONDS))
                .map(scanResult -> {
                    Pattern pattern = Pattern.compile(GlobalConstants.URBIT_PREFIX + GlobalConstants.DEVICE_UUID_REGEX);
                    Matcher matcher = pattern.matcher(scanResult.SSID);
                    String uModUUID = "DEFAULTUUID";
                    if (matcher.find()){
                        uModUUID = matcher.group(1);
                    }
                    UMod mappedUMod = new UMod(uModUUID);
                    mappedUMod.setAlias(scanResult.SSID);
                    mappedUMod.setConnectionAddress(GlobalConstants.AP_DEFAULT_IP_ADDRESS);
                    mappedUMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                    mappedUMod.setState(UMod.State.AP_MODE);
                    return mappedUMod;
                })
                .doOnNext(uMod -> freshUModDnsScan.onNext(uMod))
                .doOnError(throwable -> scanInProgress.compareAndSet(true, false))
                .doOnUnsubscribe(() -> scanInProgress.compareAndSet(true, false))
                .doOnTerminate(() -> scanInProgress.compareAndSet(true, false))
                .doOnCompleted(() -> scanInProgress.compareAndSet(true, false));
    }
}