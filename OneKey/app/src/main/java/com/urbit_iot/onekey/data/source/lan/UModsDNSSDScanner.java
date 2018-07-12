package com.urbit_iot.onekey.data.source.lan;

import android.os.Build;
import android.util.Log;

import com.github.druk.rxdnssd.BonjourService;
import com.github.druk.rxdnssd.RxDnssd;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.util.GlobalConstants;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class UModsDNSSDScanner {
    private RxDnssd rxDnssd;
    private Map<String, UMod> mCachedUMods;
    private ReplaySubject<UMod> freshUModDnsScan;
    private PublishSubject<Long> uModDnsScanTrigger;
    private AtomicBoolean scanInProgress;//TODO replace with atomicBoolean or a custom Lock that can be
    private Subscription serviceProbeRegistration;
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
    public UModsDNSSDScanner(RxDnssd rxDnssd){
        this.mCachedUMods = new LinkedHashMap<>();
        this.rxDnssd = rxDnssd;
        this.scanInProgress = new AtomicBoolean(false);
        this.freshUModDnsScan = ReplaySubject.create();
        this.uModDnsScanTrigger = PublishSubject.create();
        //TODO unsubscribe when application is destroyed
        this.serviceProbeRegistration = this.registerProbeService();

    }

    private Subscription registerProbeService(){
        BonjourService bonjourService = new BonjourService
                .Builder(0, 0, Build.DEVICE, "_urbitclientprobe._tcp", null)
                .port(59328)
                .build();
        return this.rxDnssd.register(bonjourService)
                .observeOn(Schedulers.io())
                .subscribe(service -> { }, throwable -> {
                    Log.e("DNSSD", "Error: ", throwable);
                });
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
        //Log.d("dnssd_scan", "BROWSE ALL THREAD: " + Thread.currentThread().getName() + "  scanInProgress: " + scanInProgress.get());
        //return Observable.from(this.mCachedUMods.values());
        return Observable.just(true)
                .map(aBoolean -> scanInProgress.compareAndSet(false,true))
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
                                .doOnError(throwable -> scanInProgress.compareAndSet(true, false))
                                .doOnUnsubscribe(() -> scanInProgress.compareAndSet(true, false))
                                .doOnTerminate(() -> scanInProgress.compareAndSet(true, false))
                                .doOnCompleted(() -> scanInProgress.compareAndSet(true, false));
                    }
                });
    }

    synchronized public Observable<UMod> browseLANForUMod(final String uModUUID){
        /*
        UMod cachedUMod = this.mCachedUMods.get(uModUUID);
        if (cachedUMod == null){
            return Observable.empty();
        } else {
            return Observable.just(cachedUMod);
        }
        */
        return Observable.just(true)
                .map(aBoolean -> scanInProgress.compareAndSet(false,true))
                .flatMap(mutexWasAcquired -> {
                    if (mutexWasAcquired){
                        Log.d("dnssd_scan", "AQC");
                        return singleScan()
                                .filter(uMod -> uMod.getUUID().contains(uModUUID));//TODO improve filter using regex
                    } else {
                        Log.d("dnssd_scan", "NOT AQC");
                        if (freshUModDnsScan.hasCompleted()){
                            Log.d("dnssd_scan", "SUBJECT COMP");
                            freshUModDnsScan = ReplaySubject.create();
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

    //@RxLogObservable
    //TODO as proof of concept we should try send a regular DNS query(A/AAAA) to 224.0.0.251:5353 (DNS server)
    public Observable<UMod> singleScan(){
        freshUModDnsScan = ReplaySubject.create();
        return rxDnssd.browse("_http._tcp.","local.")
                .compose(rxDnssd.resolve())
                .compose(rxDnssd.queryRecords())
                .takeUntil(Observable.timer(6000L, TimeUnit.MILLISECONDS))
                .distinct()
                .filter(bonjourService ->
                        bonjourService != null
                        && !bonjourService.isLost()
                        //&& bonjourService.getServiceName() != null
                        && bonjourService.getServiceName().matches(GlobalConstants.URBIT_PREFIX + GlobalConstants.DEVICE_UUID_REGEX)
                        && bonjourService.getInet4Address() != null)
                .map(bonjourService -> {
                    String uModUUID = getUUIDFromDiscoveryServiceName(bonjourService.getServiceName());
                    return new UMod(uModUUID,
                            bonjourService.getInet4Address().getHostAddress(),
                            true);
                })
                .doOnNext(uMod -> freshUModDnsScan.onNext(uMod))
                .doOnError(throwable -> scanInProgress.compareAndSet(true, false))
                .doOnUnsubscribe(() -> scanInProgress.compareAndSet(true, false))
                .doOnTerminate(() -> scanInProgress.compareAndSet(true, false))
                .doOnCompleted(() -> scanInProgress.compareAndSet(true, false));
    }
}
