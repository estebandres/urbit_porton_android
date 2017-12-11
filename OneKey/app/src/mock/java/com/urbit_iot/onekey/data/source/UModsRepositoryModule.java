package com.urbit_iot.onekey.data.source;

import android.content.Context;

import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdBindable;
import com.polidea.rxandroidble.RxBleClient;
import com.urbit_iot.onekey.data.source.lan.UModsBLEScanner;
import com.urbit_iot.onekey.data.source.lan.UModsDNSSDScanner;
import com.urbit_iot.onekey.data.source.lan.UModsLANDataSource;
import com.urbit_iot.onekey.data.source.lan.UModsService;
import com.urbit_iot.onekey.data.source.local.UModsLocalDBDataSource;
import com.urbit_iot.onekey.util.dagger.DigestAuth;
import com.urbit_iot.onekey.util.dagger.Local;
import com.urbit_iot.onekey.util.dagger.Remote;
import com.urbit_iot.onekey.util.networking.UrlHostSelectionInterceptor;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This is used by Dagger to inject the required arguments into the {@link UModsRepository}.
 */
@Module
public class UModsRepositoryModule {


    @Singleton
    @Provides
    UModsDNSSDScanner provideUModsDNSSD(Context context){
        RxDnssd rxDnssd = new RxDnssdBindable(context);
        return new UModsDNSSDScanner(rxDnssd);
    }

    @Singleton
    @Provides
    UModsBLEScanner provideUModsBLEScanner(Context context){
        RxBleClient rxBleClient= RxBleClient.create(context);
        return new UModsBLEScanner(rxBleClient);
    }

    @Singleton
    @Provides
    @Local
    UModsDataSource provideUModsLocalDBDataSource(Context context, BaseSchedulerProvider schedulerProvider) {
        return new UModsLocalDBDataSource(context, schedulerProvider);
    }

    @Singleton
    @Provides
    @Remote
    UModsDataSource provideUModsRemoteDataSource(UModsDNSSDScanner uModsDNSSDScanner,
                                                 UModsBLEScanner uModsBLEScanner,
                                                 @DigestAuth UModsService uModsService,
                                                 @DigestAuth UrlHostSelectionInterceptor urlHostSelectionInterceptor) {
        return new UModsLANDataSource(uModsDNSSDScanner, uModsBLEScanner, uModsService, urlHostSelectionInterceptor);
    }
}