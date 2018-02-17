package com.urbit_iot.onekey.data.source;

import android.content.Context;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;

import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdBindable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.polidea.rxandroidble.RxBleClient;

import com.urbit_iot.onekey.data.source.internet.FirmwareFileDownloader;
import com.urbit_iot.onekey.data.source.internet.UModsInternetDataSource;
import com.urbit_iot.onekey.data.source.lan.UModsBLEScanner;
import com.urbit_iot.onekey.data.source.lan.UModsDNSSDScanner;
import com.urbit_iot.onekey.data.source.lan.UModsLANDataSource;
import com.urbit_iot.onekey.data.source.lan.UModsService;
import com.urbit_iot.onekey.data.source.lan.UModsWiFiScanner;
import com.urbit_iot.onekey.data.source.local.UModsLocalDBDataSource;

import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.dagger.Internet;
import com.urbit_iot.onekey.util.dagger.Local;
import com.urbit_iot.onekey.util.dagger.LanOnly;
import com.urbit_iot.onekey.util.networking.UrlHostSelectionInterceptor;
import com.urbit_iot.onekey.util.retrofit2.RetrofitUtils;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * This is used by Dagger to inject the required arguments into the {@link UModsRepository}.
 */
@Module
public class UModsRepositoryModule {

    private String appUserPhoneNumber;
    private String appUUIDHash;

    //TODO replace private fields with the appUserRepository instance
    public UModsRepositoryModule(String appUserPhoneNumber, String appUUIDHash) {
        this.appUserPhoneNumber = appUserPhoneNumber;
        this.appUUIDHash = appUUIDHash;
    }

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
    UModsWiFiScanner provideUModsWiFiScanner(Context context){
        return new UModsWiFiScanner(context);
    }

    @Singleton
    @Provides
    @Local
    UModsDataSource provideUModsLocalDBDataSource(Context context, BaseSchedulerProvider schedulerProvider) {
        return new UModsLocalDBDataSource(context, schedulerProvider);
    }

    @Singleton
    @Provides
    @LanOnly
    UModsDataSource provideUModsRemoteDataSource(UModsDNSSDScanner uModsDNSSDScanner,
                                                 UModsBLEScanner uModsBLEScanner,
                                                 UModsWiFiScanner uModsWiFiScanner,
                                                 UrlHostSelectionInterceptor urlHostSelectionInterceptor,
                                                 @Named("default") UModsService defaultUModsService,
                                                 @Named("app_user") UModsService appUserUModsService) {
        return new UModsLANDataSource(uModsDNSSDScanner, uModsBLEScanner, uModsWiFiScanner, urlHostSelectionInterceptor,defaultUModsService, appUserUModsService);
    }

    @Singleton
    @Provides
    @Internet
    UModsDataSource provideUModsInternetDataSource(Context context){
        return new UModsInternetDataSource(new FirmwareFileDownloader(context));
    }
    //TODO separate those into ints own network module

    @Provides
    @Singleton
    Gson provideGsonInstance(){
        return  new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    /*
    Gson gson = new GsonBuilder()
     .registerTypeAdapter(Id.class, new IdTypeAdapter())
     .enableComplexMapKeySerialization()
     .serializeNulls()
     .setDateFormat(DateFormat.LONG)
     .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
     .setPrettyPrinting()
     .setVersion(1.0)
     .create();
     */

    @Provides
    @Singleton
    UrlHostSelectionInterceptor provideUrlHostSelectorInterceptor(){
        return new UrlHostSelectionInterceptor();
    }

    /*
    @Provides
    @Singleton
    @DigestAuth
    Credentials provideDigestAuthCredentials(){
        return new Credentials("urbit", "urbit");
    }
    */

    @Provides
    @Singleton
    @Named("default")
    OkHttpClient provideDigestAuthDefaultOkHttpClient(UrlHostSelectionInterceptor urlHostSelectionInterceptor){
        final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(new Credentials("urbit", "urbit"));
        final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
        return new OkHttpClient.Builder()
                .addInterceptor(urlHostSelectionInterceptor)
                .authenticator(new CachingAuthenticatorDecorator(digestAuthenticator,authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                .connectTimeout(1500L, TimeUnit.MILLISECONDS)
                .readTimeout(800L, TimeUnit.MILLISECONDS)
                .build();
    }

    @Provides
    @Singleton
    @Named("default")
    Retrofit provideDigestAuthDefaultRetrofit(@Named("default") OkHttpClient okHttpClient, Gson gson){
        return new Retrofit.Builder()
                .baseUrl(GlobalConstants.LAN_DEFAULT_URL)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
    }

    @Provides
    @Singleton
    @Named("default")
    UModsService provideDefaultUModsService(@Named("default") Retrofit retrofit){
        return retrofit.create(UModsService.class);
    }

    @Provides
    @Singleton
    @Named("app_user")
    OkHttpClient provideDigestAuthAppUserOkHttpClient(UrlHostSelectionInterceptor urlHostSelectionInterceptor){
        final Credentials credentials = new Credentials(this.appUserPhoneNumber,this.appUUIDHash);
        final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);
        final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
        return new OkHttpClient.Builder()
                .addInterceptor(urlHostSelectionInterceptor)
                .authenticator(new CachingAuthenticatorDecorator(digestAuthenticator,authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                .connectTimeout(1500L, TimeUnit.MILLISECONDS)
                .readTimeout(800L, TimeUnit.MILLISECONDS)
                .build();
    }

    @Provides
    @Singleton
    @Named("app_user")
    Retrofit provideDigestAuthAppUserRetrofit(@Named("app_user") OkHttpClient okHttpClient, Gson gson){
        return new Retrofit.Builder()
                .baseUrl(GlobalConstants.LAN_DEFAULT_URL)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
    }

    @Provides
    @Singleton
    @Named("app_user")
    UModsService provideAppUserUModsService(@Named("app_user") Retrofit retrofit){
        return retrofit.create(UModsService.class);
    }
}