package com.urbit_iot.onekey.data.source.lan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.github.pwittchen.reactivewifi.ReactiveWifi;
import com.urbit_iot.onekey.data.UMod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by steve-urbit on 30/01/18.
 */

public class UModsWiFiScanner {
    private final Context appContext;
    private Map<String, UMod> mCachedAPModeUMods;

    public UModsWiFiScanner(Context appContext) {
        this.appContext = appContext;
        this.mCachedAPModeUMods = new LinkedHashMap<>();
        this.continuousWiFiScann();
    }

    synchronized public Observable<UMod> browseWiFiForUMods() {
        //return scanWiFi(null);
        return Observable.from(this.mCachedAPModeUMods.values());
    }

    synchronized public Observable<UMod> browseWiFiForUMod(String uModUUID) {
        //return scanWiFi(uModUUID);
        UMod cachedUMod = this.mCachedAPModeUMods.get(uModUUID);
        if (cachedUMod == null){
            return Observable.empty();
        } else {
            return Observable.just(cachedUMod);
        }
    }


    //@RxLogObservable
    private Observable<UMod> scanWiFi(final String filterUModUUID) {
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
                .flatMap(new Func1<List<ScanResult>, Observable<ScanResult>>() {
                    @Override
                    public Observable<ScanResult> call(List<ScanResult> scanResults) {
                        return Observable.from(scanResults);
                    }
                })
                .distinct(new Func1<ScanResult, Object>() {
                    @Override
                    public Object call(ScanResult scanResult) {
                        return scanResult.SSID;
                    }
                })
                .filter(new Func1<ScanResult, Boolean>() {
                    @Override
                    public Boolean call(ScanResult scanResult) {
                        return scanResult.level > -75;
                    }
                })
                .takeUntil(Observable.timer(4000L, TimeUnit.MILLISECONDS))
                .filter(new Func1<ScanResult, Boolean>() {
                    @Override
                    public Boolean call(ScanResult scanResult) {
                        //TODO improve filter using regex
                        return scanResult.SSID.contains("urbit");
                    }
                })
                .map(new Func1<ScanResult, UMod>() {
                    @Override
                    public UMod call(ScanResult scanResult) {
                        Pattern pattern = Pattern.compile("urbit_(.*?)$");
                        Matcher matcher = pattern.matcher(scanResult.SSID);
                        String uModUUID = "DEFAULTUUID";
                        if (matcher.find()){
                            uModUUID = matcher.group(1);
                        }
                        UMod mappedUMod = new UMod(uModUUID);
                        mappedUMod.setAlias(scanResult.SSID);
                        mappedUMod.setState(UMod.State.AP_MODE);
                        return mappedUMod;
                    }
                })
                .filter(new Func1<UMod, Boolean>() {
                    @Override
                    public Boolean call(UMod uMod) {
                        Log.d("wifi_scan", "Filter:" + filterUModUUID + "UMOD: " + uMod.getUUID() + "  " + uMod.getAlias());
                        if (filterUModUUID != null){
                            return filterUModUUID.contentEquals(uMod.getUUID());
                        }
                        return true;
                    }
                });
    }

    private void continuousWiFiScann(){
        observableWiFiScanResults()
                .doOnNext(uMod -> mCachedAPModeUMods.put(uMod.getUUID(),uMod))
                .subscribeOn(Schedulers.io())
                .subscribe();
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
                .flatMap(scanResults -> {
                    mCachedAPModeUMods.clear();
                    return Observable.from(scanResults);})
                .distinct((Func1<ScanResult, Object>) scanResult -> scanResult.SSID)
                .filter(scanResult -> scanResult.level > -75)
                .filter(scanResult -> {
                    //TODO improve filter using regex
                    return scanResult.SSID.contains("urbit");
                })
                .map(scanResult -> {
                    Pattern pattern = Pattern.compile("urbit_(.*?)$");
                    Matcher matcher = pattern.matcher(scanResult.SSID);
                    String uModUUID = "DEFAULTUUID";
                    if (matcher.find()){
                        uModUUID = matcher.group(1);
                    }
                    UMod mappedUMod = new UMod(uModUUID);
                    mappedUMod.setAlias(scanResult.SSID);
                    mappedUMod.setState(UMod.State.AP_MODE);
                    return mappedUMod;
                });
    }
}