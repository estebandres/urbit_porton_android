package com.urbit_iot.onekey.data.source.lan;

import android.content.Context;
import android.util.Log;

import com.fernandocejas.frodo.annotation.RxLogObservable;
import com.github.druk.rxdnssd.BonjourService;
import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdBindable;
import com.urbit_iot.onekey.data.UMod;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class UModsDNSSDScanner {
    private RxDnssd rxDnssd;

    public UModsDNSSDScanner(RxDnssd rxDnssd){
        this.rxDnssd = rxDnssd;
    }

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
                                String uModUUID = "DEFAULTUUID";
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
}
