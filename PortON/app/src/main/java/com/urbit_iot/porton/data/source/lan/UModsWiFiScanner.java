package com.urbit_iot.porton.data.source.lan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Pair;

import com.github.pwittchen.reactivewifi.ReactiveWifi;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.umodconfig.UModConfigFragment;
import com.urbit_iot.porton.util.GlobalConstants;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * Created by steve-urbit on 30/01/18.
 */

public class UModsWiFiScanner {
    @NonNull
    private final Context appContext;
    private boolean scanInProgress;
    private Semaphore scanMutex;
    private PublishSubject<UMod> freshUModDnsScan;
    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    @Inject
    public UModsWiFiScanner(@NonNull Context appContext,
                            @NonNull BaseSchedulerProvider mSchedulerProvider) {
        this.appContext = appContext;
        this.mSchedulerProvider = mSchedulerProvider;
        this.scanInProgress = false;
        this.freshUModDnsScan = PublishSubject.create();
        this.scanMutex = new Semaphore(1);
    }

    synchronized public Observable<UMod> browseWiFiForUMods() {
        return Observable.defer(() -> {
            try {
                scanMutex.acquire();
            } catch (InterruptedException e) {
                return Observable.error(e);

            }
            if (!scanInProgress){
                Log.d("wifi_scan", "singleScan");
                scanInProgress = true;
                scanMutex.release();
                return observableWiFiScanResults();
            } else {
                scanMutex.release();
                Log.d("wifi_scan", "listen for results");
                return freshUModDnsScan.asObservable()
                        .takeUntil(Observable.timer(5000L, TimeUnit.MILLISECONDS));
            }
        });
    }

    synchronized public Observable<UMod> browseWiFiForUMod(String uModUUID) {
        return this.browseWiFiForUMods().filter(uMod -> uMod.getUUID().equals(uModUUID));

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
                .doOnUnsubscribe(() -> {
                    scanMutex.acquireUninterruptibly();
                    scanInProgress = false;
                    scanMutex.release();
                });
    }


    public Observable<List<Pair<String,Integer>>> listOfNearbySSIDs(){
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
                //.doOnNext(scanResult -> Log.d("WIFI_SCAN","RESULTADO: " + scanResult.SSID + "  " + scanResult.level))
                .distinct(scanResult -> scanResult.SSID)
                .filter(scanResult -> scanResult.level > -86)
                .filter(scanResult -> !scanResult.SSID.matches(GlobalConstants.URBIT_PREFIX + GlobalConstants.DEVICE_UUID_REGEX) )
                .takeUntil(Observable.timer(5000L, TimeUnit.MILLISECONDS))
                .map(scanResult -> new Pair<>(scanResult.SSID, scanResult.level))
                .toList();
    }
}