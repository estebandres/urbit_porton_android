package com.urbit_iot.onekey.data.source.lan;

import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.druk.rxdnssd.BonjourService;
import com.github.druk.rxdnssd.RxDnssd;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;
import com.urbit_iot.onekey.util.schedulers.SchedulerProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import rx.Completable;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class UModsDNSSDScanner {
    @NonNull
    private RxDnssd rxDnssd;

    private ReplaySubject<UMod> freshUModDnsScan;

    private AtomicBoolean scanInProgress;//TODO replace with atomicBoolean or a custom Lock that can be

    //lock by one thread and unlocked by other.
    /*
    public class MyLock implements Lock {

    private boolean isLocked = false;

    public synchronized void lock() throws InterruptedException {
        while (isLocked)
            wait();
        isLocked = true;
    }

    public synchronized void unlock() {
        if (!isLocked)
            throw new IllegalStateException();
        isLocked = false;
        notify();
    }

}
     */

    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    @Inject
    public UModsDNSSDScanner(@NonNull RxDnssd rxDnssd,
                             @NonNull BaseSchedulerProvider mSchedulerProvider){
        this.rxDnssd = rxDnssd;
        this.mSchedulerProvider = mSchedulerProvider;
        this.scanInProgress = new AtomicBoolean(false);
        this.freshUModDnsScan = ReplaySubject.create();

    }


    private String getUUIDFromDiscoveryServiceName(String serviceName){
        String uModUUID = "DEFAULTUUID";

        Pattern pattern = Pattern.compile(GlobalConstants.URBIT_PREFIX + GlobalConstants.DEVICE_UUID_REGEX);
        Matcher matcher = pattern.matcher(serviceName);

        if (matcher.find()){
            uModUUID = matcher.group(1);
        }
        return uModUUID;
    }

    //@RxLogObservable
    synchronized public Observable<UMod> browseLANForUMods(){
        return Observable.defer(() -> Observable.just(true)
                .map(aBoolean -> scanInProgress.compareAndSet(false,true))
                //TODO review synchronization scheme (petri net validation)
                .flatMap(mutexWasAcquired -> {
                    if (mutexWasAcquired){
                        Log.d("dnssd_scan", "AQC");
                        return singleScan();
                    } else {
                        Log.d("dnssd_scan", "NOT AQC");
                        if (freshUModDnsScan.hasCompleted()){
                            Log.d("dnssd_scan", "SUBJECT COMP");
                            freshUModDnsScan = ReplaySubject.create();
                        }
                        return freshUModDnsScan.asObservable()
                                .takeUntil(Observable.timer(5000L, TimeUnit.MILLISECONDS))
                                .doAfterTerminate(() -> scanInProgress.compareAndSet(true, false))
                                //Deals when the scanning is ended abruptly
                                .doOnUnsubscribe(() -> scanInProgress.compareAndSet(true, false));
                    }
                }));
    }

    synchronized public Observable<UMod> browseLANForUMod(final String uModUUID){
        return this.browseLANForUMods().filter(uMod -> uMod.getUUID().equals(uModUUID));
    }

    //@RxLogObservable
    //TODO as proof of concept we should try send a regular DNS query(A/AAAA) to 224.0.0.251:5353 (DNS server)
    public Observable<UMod> singleScan(){
        freshUModDnsScan = ReplaySubject.create();
        return Observable.defer(() -> rxDnssd.browse("_http._tcp.","local.")
                .compose(rxDnssd.resolve())
                .compose(rxDnssd.queryRecords())
                // The compositions switch the current thread to the main thread
                // so a new switch is needed in order to avoid
                .observeOn(mSchedulerProvider.io())
                .takeUntil(Observable.timer(5000L, TimeUnit.MILLISECONDS))
                .distinct()
                .filter(bonjourService ->
                        bonjourService != null
                                && !bonjourService.isLost()
                                //&& bonjourService.getServiceName() != null
                                && bonjourService.getServiceName().matches(GlobalConstants.URBIT_PREFIX + GlobalConstants.DEVICE_UUID_REGEX)
                                && bonjourService.getInet4Address() != null)
                .map(bonjourService -> {
                    Log.d("DNSSD_SCAN", "UMOD DISCOVER ON: " + Thread.currentThread().getName());
                    String uModUUID = getUUIDFromDiscoveryServiceName(bonjourService.getServiceName());
                    return new UMod(uModUUID,
                            bonjourService.getInet4Address().getHostAddress(),
                            true);
                })
                .flatMap(uMod ->
                        Observable.just(uMod)
                                .subscribeOn(mSchedulerProvider.io())
                                .flatMap(uMod1 ->
                                        testConnectionToModule(uMod1.getConnectionAddress())
                                                .andThen(Observable.just(uMod1))
                                                .onErrorResumeNext(Observable.empty())
                                )
                )
                //.doOnNext(uMod -> testConnectionToModule(uMod.getConnectionAddress()))
                .doOnNext(uMod -> {
                    Log.d("DNSSD_SCAN", "RESULT ON : " + Thread.currentThread().getName());
                    freshUModDnsScan.onNext(uMod);
                })
                .doAfterTerminate(() -> scanInProgress.compareAndSet(true, false))
                //When the scanning is ended abruptly
                .doOnUnsubscribe(() -> scanInProgress.compareAndSet(true, false)));
    }

    //Tries to connect to the module so the ARP table is updated and the future http request is faster.
    public Completable testConnectionToModule(String ipAddress) {
        return Completable.fromCallable(() -> {
            Socket clientSocket;
            clientSocket = new Socket();
            try {
                Log.d("DNSSD_SCAN", "TESTING CONN ON: " + Thread.currentThread().getName());
                clientSocket.connect(
                        new InetSocketAddress(
                                ipAddress,
                                GlobalConstants.UMOD__TCP_ECHO_PORT),
                        1500);
            } finally {
                clientSocket.close();
            }
            return true;
        });
    }
}
