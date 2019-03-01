package com.urbit_iot.porton.data.source.lan;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.util.Log;

import com.github.druk.rxdnssd.RxDnssd;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.util.GlobalConstants;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import rx.Completable;
import rx.Observable;
import rx.subjects.ReplaySubject;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class UModsDNSSDScanner {
    @NonNull
    private RxDnssd rxDnssd;

    private ReplaySubject<UMod> freshUModDnsScan;
    @VisibleForTesting
    boolean scanInProgress;
    @VisibleForTesting
    Semaphore scanMutex;
    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    @Inject
    public UModsDNSSDScanner(@NonNull RxDnssd rxDnssd,
                             @NonNull BaseSchedulerProvider mSchedulerProvider){
        this.rxDnssd = rxDnssd;
        this.mSchedulerProvider = mSchedulerProvider;
        this.scanInProgress = false;
        this.freshUModDnsScan = ReplaySubject.create();
        this.scanMutex = new Semaphore(1);

    }


    String getUUIDFromDiscoveryServiceName(String serviceName){
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
        return Observable.defer(() -> {
            try {
                scanMutex.acquire();
            } catch (InterruptedException e) {
                return Observable.error(e);

            }
            if (!scanInProgress){
                Log.d("dnssd_scan", "singleScan");
                scanInProgress = true;
                scanMutex.release();
                return singleScan();
            } else {
                scanMutex.release();
                Log.d("dnssd_scan", "listen for results");
                return freshUModDnsScan.asObservable()
                        .takeUntil(Observable.timer(5000L, TimeUnit.MILLISECONDS,mSchedulerProvider.computation()));
            }
        });
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
                .compose(rxDnssd.queryIPRecords())
                // The compositions switch the current thread to the main thread
                // so a new switch is needed in order to avoid
                .observeOn(mSchedulerProvider.io())
                .takeUntil(Observable.timer(5000L, TimeUnit.MILLISECONDS,mSchedulerProvider.computation()))
                .distinct()
                .filter(bonjourService ->
                        bonjourService != null
                                && !bonjourService.isLost()
                                //&& bonjourService.getServiceName() != null
                                && bonjourService.getServiceName()
                                .matches(GlobalConstants.URBIT_PREFIX
                                        + GlobalConstants.DEVICE_UUID_REGEX)
                                && bonjourService.getInet4Address() != null)
                .map(bonjourService -> {
                    Log.d("DNSSD_SCAN", "UMOD DISCOVER ON: " + Thread.currentThread().getName());
                    String uModUUID = getUUIDFromDiscoveryServiceName(bonjourService.getServiceName());
                    return new UMod(uModUUID,
                            bonjourService.getInet4Address().getHostAddress(),
                            true);
                })
                .doOnNext(uMod -> {
                    Log.d("DNSSD_SCAN", "RESULT ON : " + Thread.currentThread().getName());
                    tryToConnect(uMod);
                    freshUModDnsScan.onNext(uMod);
                })
                //takeUntil always unsubscribes the chain but also when the scanning is ended abruptly.
                .doOnUnsubscribe(() -> {
                   scanMutex.acquireUninterruptibly();
                   scanInProgress = false;
                   scanMutex.release();
                })
        );
    }

    void tryToConnect(UMod uMod){
        testTcpConnection(uMod.getConnectionAddress())
                .subscribe(() -> Log.d("DNSSD_SCAN", "TESTING CONN SUCCEED FOR: "
                                + uMod.getUUID() + "@" + uMod.getConnectionAddress()
                                + " ON: " + Thread.currentThread().getName())
                , throwable -> Log.e("DNSSD_SCAN", "TESTING CONN FAILED FOR: "
                                + uMod.getUUID() + "@" + uMod.getConnectionAddress()
                                + " CAUSE: " + throwable.getMessage() + " TYPE: "
                                + throwable.getClass().getSimpleName()
                                + Thread.currentThread().getName()));

    }
    //Tries to connect to the module so the ARP table is updated and the future http request is faster.
    Completable testTcpConnection(String ipAddress) {
        return Completable.fromCallable(() -> {
            Socket clientSocket;
            clientSocket = new Socket();
            try {
                clientSocket.connect(
                        new InetSocketAddress(
                                ipAddress,
                                GlobalConstants.UMOD__TCP_ECHO_PORT),
                        1500);
            } finally {
                clientSocket.close();
            }
            return true;
        }).subscribeOn(mSchedulerProvider.io());
    }
}
