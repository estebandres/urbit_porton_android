package com.urbit_iot.onekey.data.source.lan;

import android.util.Log;

import com.fernandocejas.frodo.annotation.RxLogObservable;
import com.github.druk.rxdnssd.RxDnssd;
import com.urbit_iot.onekey.data.UMod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class UModsDNSSDScanner {
    private RxDnssd rxDnssd;
    private Map<String, UMod> mCachedUMods;
    private PublishSubject<UMod> freshUModDnsScan;
    private PublishSubject<Long> uModDnsScanTrigger;
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
    public UModsDNSSDScanner(RxDnssd rxDnssd){
        this.mCachedUMods = new LinkedHashMap<>();
        this.rxDnssd = rxDnssd;
        this.scanInProgress = new AtomicBoolean(false);
        this.freshUModDnsScan = PublishSubject.create();
        this.uModDnsScanTrigger = PublishSubject.create();
        //TODO reconsider continuous dnssd discovery. When do I need to update constantly??
        //this.continuousBrowseLANForUMods();
        /*
        Observable.interval(10, TimeUnit.SECONDS)
                .startWith(1L)
                .subscribeOn(Schedulers.io())
                .doOnNext(n -> singleScan())
                .onErrorResumeNext(Observable.just(1234L))
                .subscribe();
        */
        //TODO unsubscribe when application is destroyed

    }

    private void continuousBrowseLANForUMods(){
        rxDnssd.browse("_http._tcp.","local.")
                .compose(rxDnssd.resolve())
                .compose(rxDnssd.queryRecords())
                .filter(bonjourService -> {
                    //TODO improve filter using regex
                    return bonjourService != null
                            && bonjourService.getHostname() != null
                            && bonjourService.getHostname().contains("urbit")
                            && bonjourService.getInet4Address() != null;
                })
                .doOnNext(bonjourService -> {
                    String uModUUID = getUUIDFromDiscoveryHostName(bonjourService.getHostname());
                    if (bonjourService.isLost()){
                        //Log.d("dnssd_cont","LOST " + uModUUID);
                        mCachedUMods.remove(uModUUID);
                    } else {
                        //Log.d("dnssd_cont","FOUND " + uModUUID);
                        mCachedUMods.put(uModUUID, new UMod(uModUUID,
                                bonjourService.getInet4Address().getHostAddress(),
                                true));
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    private String getUUIDFromDiscoveryHostName(String hostName){
        String uModUUID = "DEFAULTUUID";

        Pattern pattern = Pattern.compile("urbit-(.*?)\\.local\\.");
        Matcher matcher = pattern.matcher(hostName);

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


    //Keeps browsing the LAN for 5 seconds maximum.
    //@RxLogObservable
    /*
    public void singleScan(){
        this.mCachedUMods.clear();
        rxDnssd.browse("_http._tcp.","local.")
            .compose(rxDnssd.resolve())
            .compose(rxDnssd.queryRecords())
            .takeUntil(Observable.timer(5000L, TimeUnit.MILLISECONDS))
            .distinct()
            .filter(bonjourService -> {
                //TODO improve filter using regex
                return bonjourService != null
                        && bonjourService.getHostname() != null
                        && bonjourService.getHostname().contains("urbit")
                        && bonjourService.getInet4Address() != null;
            })
            .doOnNext(bonjourService -> {
                String uModUUID = getUUIDFromDiscoveryHostName(bonjourService.getHostname());
                if (bonjourService.isLost()){
                    //Log.d("dnssd_cont","LOST " + uModUUID);
                    mCachedUMods.remove(uModUUID);
                } else {
                    //Log.d("dnssd_cont","FOUND " + uModUUID);
                    mCachedUMods.put(uModUUID, new UMod(uModUUID,
                            bonjourService.getInet4Address().getHostAddress(),
                            true));
                }
            })
            .subscribe();
    }
    */
    //@RxLogObservable
    public Observable<UMod> singleScan(){
        return rxDnssd.browse("_http._tcp.","local.")
                .compose(rxDnssd.resolve())
                .compose(rxDnssd.queryRecords())
                .takeUntil(Observable.timer(6000L, TimeUnit.MILLISECONDS))
                .distinct()
                .filter(bonjourService ->
                        bonjourService != null
                        && !bonjourService.isLost()
                        && bonjourService.getHostname() != null
                        && bonjourService.getHostname().contains("urbit")//TODO improve filter using regex
                        && bonjourService.getInet4Address() != null)
                .map(bonjourService -> {
                    String uModUUID = getUUIDFromDiscoveryHostName(bonjourService.getHostname());
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

    /*
    //Keeps browsing the LAN for 4 seconds maximum.
    //@RxLogObservable
    public Observable<UMod> browseLANForUMods(){
        return Observable.defer(new Func0<Observable<UMod>>() {
            @Override
            public Observable<UMod> call() {
                return rxDnssd.browse("_http._tcp.","local.")
                        .compose(rxDnssd.resolve())
                        .compose(rxDnssd.queryRecords())
                        //.take(10)
                        .takeUntil(Observable.timer(4000L, TimeUnit.MILLISECONDS))
                        .distinct()
                        .filter(new Func1<BonjourService, Boolean>() {
                            @Override
                            public Boolean call(BonjourService bonjourService) {
                                //TODO improve filter using regex
                                return bonjourService.getHostname() != null && bonjourService.getHostname().contains("urbit");
                            }
                        })
                        .map(new Func1<BonjourService,UMod>(){
                            public UMod call(BonjourService discovery){
                                Pattern pattern = Pattern.compile("urbit-(.*?)\\.local\\.");
                                Matcher matcher = pattern.matcher(discovery.getHostname());
                                String uModUUID = "DEFAULTUUID";
                                if (matcher.find()){
                                    uModUUID = matcher.group(1);
                                }
                                return new UMod(uModUUID,
                                        discovery.getInet4Address().getHostAddress(),
                                        true);
                            }
                        });
            }
        });
    }
    */


    /*
    //TODO as proof of concept we should try send a regular DNS query(A/AAAA) to 224.0.0.251:5353 (DNS server)
    //there are several java android libraries that could work well. But it seems that mongoose os only implements the advertise method.
    public Observable<UMod> browseLANForUMod(final String uModUUID){
        return Observable.defer(new Func0<Observable<UMod>>() {
            @Override
            public Observable<UMod> call() {
                return rxDnssd.browse("_http._tcp.","local.")
                        .compose(rxDnssd.resolve())
                        .compose(rxDnssd.queryRecords())
                        .distinct()
                        .takeUntil(Observable.timer(4000L, TimeUnit.MILLISECONDS))
                        .filter(new Func1<BonjourService, Boolean>() {
                            @Override
                            public Boolean call(BonjourService bonjourService) {
                                return bonjourService != null
                                        && bonjourService.getHostname() != null
                                        && bonjourService.getHostname().contains(uModUUID)
                                        && bonjourService.getInet4Address() != null;
                            }
                        })
                        .take(1)
                        .doOnError(new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e("dns-sd_scan", throwable.getMessage());
                            }
                        })
                        .doOnNext(new Action1<BonjourService>() {
                            @Override
                            public void call(BonjourService bonjourService) {
                                Log.d("dns-sd_scan",bonjourService.getHostname() + " - " + bonjourService.getInet4Address().getHostAddress());
                            }
                        })
                        .map(new Func1<BonjourService,UMod>(){
                            public UMod call(BonjourService discovery){
                                Pattern pattern = Pattern.compile("urbit-(.*?)\\.local\\.");
                                Matcher matcher = pattern.matcher(discovery.getHostname());
                                String uModUUID = " DEFAULTUUID";
                                if (matcher.find()){
                                    uModUUID = matcher.group(1);
                                }
                                return new UMod(uModUUID,
                                        discovery.getInet4Address().getHostAddress(),
                                        true);
                            }
                        })
                        .doOnCompleted(new Action0() {
                            @Override
                            public void call() {
                                Log.d("dns-sd", "single browse finished.");
                            }
                        });
            }
        });
    }
    */
}
