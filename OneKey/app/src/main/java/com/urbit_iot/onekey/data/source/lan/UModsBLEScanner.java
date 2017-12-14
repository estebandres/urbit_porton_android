package com.urbit_iot.onekey.data.source.lan;


import android.util.Log;

import com.github.druk.rxdnssd.BonjourService;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;
import com.urbit_iot.onekey.data.UMod;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class UModsBLEScanner {
    private RxBleClient mRxBleClient;
    //private Subscription mScanSubscription;//Should be kept when the stream is killed on activities/fragments life cycles.

    public UModsBLEScanner(RxBleClient mRxBleClient) {
        this.mRxBleClient = mRxBleClient;
    }

    //Scans for BLE devices for 4 seconds.
    public Observable<UMod> bleScanForUMods(){
        return Observable.defer(new Func0<Observable<UMod>>() {
            @Override
            public Observable<UMod> call() {
                return mRxBleClient.scanBleDevices(
                        new ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                .build(),
                        new ScanFilter.Builder()
                                .build())
                        .takeUntil(Observable.timer(4000L, TimeUnit.MILLISECONDS))
                        .distinct()
                        .doOnError(new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e("ble_scan", throwable.getMessage());
                            }
                        })
                        .doOnNext(new Action1<ScanResult>() {
                            @Override
                            public void call(ScanResult scanResult) {
                                Log.d("ble_scan",scanResult.toString());
                            }
                        })
                        .filter(new Func1<ScanResult, Boolean>() {
                            @Override
                            public Boolean call(ScanResult scanResult) {
                                return scanResult.getBleDevice().getName() != null && scanResult.getBleDevice().getName().contains("urbit");
                            }
                        })
                        .map(new Func1<ScanResult, UMod>() {
                            @Override
                            public UMod call(ScanResult scanResult) {
                                return new UMod(scanResult.getBleDevice().getName(),
                                        scanResult.getBleDevice().getMacAddress());
                            }
                        });
            }
        });
    }
}
