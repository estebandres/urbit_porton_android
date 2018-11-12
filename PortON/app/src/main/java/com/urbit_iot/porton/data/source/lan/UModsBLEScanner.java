package com.urbit_iot.porton.data.source.lan;


import android.util.Log;

import com.fernandocejas.frodo.annotation.RxLogObservable;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.util.GlobalConstants;

import java.util.concurrent.TimeUnit;

import rx.Observable;
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
    @RxLogObservable
    public Observable<UMod> bleScanForUMods(){
        return Observable.defer(new Func0<Observable<UMod>>() {
            @Override
            public Observable<UMod> call() {
                return mRxBleClient.observeStateChanges()
                        .switchMap(new Func1<RxBleClient.State, Observable<ScanResult>>() {
                            @Override
                            public Observable<ScanResult> call(RxBleClient.State state) {
                                switch (state) {
                                    case READY:
                                        // everything should work
                                        return mRxBleClient.scanBleDevices(
                                                new ScanSettings.Builder()
                                                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                                        .build(),
                                                new ScanFilter.Builder()
                                                        .build());
                                    case BLUETOOTH_NOT_AVAILABLE:
                                        // basically no functionality will work here
                                    case LOCATION_PERMISSION_NOT_GRANTED:
                                        // scanning and connecting will not work
                                    case BLUETOOTH_NOT_ENABLED:
                                        // scanning and connecting will not work
                                    case LOCATION_SERVICES_NOT_ENABLED:
                                        // scanning will not work
                                    default:
                                        return Observable.empty();
                                }
                            }
                        })
                        .takeUntil(Observable.timer(4000L, TimeUnit.MILLISECONDS))
                        .distinct()
                        .doOnError(new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e("ble_scan", "" + throwable.getMessage());
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
                                return scanResult.getBleDevice().getName() != null && scanResult.getBleDevice().getName().matches(GlobalConstants.URBIT_PREFIX + GlobalConstants.DEVICE_UUID_REGEX);
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
