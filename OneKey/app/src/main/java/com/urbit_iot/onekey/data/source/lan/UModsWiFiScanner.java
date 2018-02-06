package com.urbit_iot.onekey.data.source.lan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.github.pwittchen.reactivewifi.ReactiveWifi;
import com.urbit_iot.onekey.data.UMod;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by steve-urbit on 30/01/18.
 */

public class UModsWiFiScanner {
    private final Context appContext;

    public UModsWiFiScanner(Context appContext) {
        this.appContext = appContext;
    }

    public Observable<UMod> browseWiFiForUMods() {
        return scanWiFi(null);
    }

    public Observable<UMod> browseWiFiForUMod(String uModUUID) {
        return scanWiFi(uModUUID);
    }

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
                .map(new Func1<ScanResult, UMod>() {
                    @Override
                    public UMod call(ScanResult scanResult) {
                        UMod mappedUMod = new UMod(scanResult.SSID);
                        mappedUMod.setState(UMod.State.AP_MODE);
                        return mappedUMod;
                    }
                })
                .filter(new Func1<UMod, Boolean>() {
                    @Override
                    public Boolean call(UMod uMod) {
                        if (filterUModUUID != null){
                            return filterUModUUID.contentEquals(uMod.getUUID());
                        }
                        return true;
                    }
                });
    }
}