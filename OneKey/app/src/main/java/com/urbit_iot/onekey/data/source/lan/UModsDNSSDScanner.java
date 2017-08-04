package com.urbit_iot.onekey.data.source.lan;

import android.content.Context;
import android.util.Log;

import com.github.druk.rxdnssd.BonjourService;
import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdBindable;
import com.urbit_iot.onekey.data.UMod;

import java.util.concurrent.TimeUnit;

import rx.Observable;
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

    public Observable<UMod> browseLANForUMods(){
        return Observable.defer(new Func0<Observable<UMod>>() {
            @Override
            public Observable<UMod> call() {
                return rxDnssd.browse("_http._tcp.","local.")
                        .compose(rxDnssd.resolve())
                        .compose(rxDnssd.queryRecords())
                        .distinct()
                        .doOnError(new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e("dns-sd_scan", throwable.getMessage());
                            }
                        })
                        .doOnNext(new Action1<BonjourService>() {
                            @Override
                            public void call(BonjourService bonjourService) {
                                Log.d("STEVEE_dns",bonjourService.toString());
                            }
                        })
                        //.timeout(5, TimeUnit.SECONDS)
                        //.onErrorResumeNext(Observable.<BonjourService>empty())
                        .filter(new Func1<BonjourService, Boolean>() {
                            @Override
                            public Boolean call(BonjourService bonjourService) {
                                return bonjourService.getHostname().contains("mos");
                            }
                        })
                        .map(new Func1<BonjourService,UMod>(){
                            public UMod call(BonjourService discovery){
                                return new UMod(discovery.getHostname(),
                                        discovery.getInet4Address().toString());
                            }
                        });
            }
        });
    }
}
