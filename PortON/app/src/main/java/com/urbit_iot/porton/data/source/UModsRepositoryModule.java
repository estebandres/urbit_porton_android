package com.urbit_iot.porton.data.source;

import android.content.Context;
import android.util.Log;

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

import com.urbit_iot.porton.data.source.gps.LocationService;
import com.urbit_iot.porton.data.source.internet.FirmwareFileDownloader;
import com.urbit_iot.porton.data.source.internet.PahoClientRxWrap;
import com.urbit_iot.porton.data.source.internet.SimplifiedUModMqttService;
import com.urbit_iot.porton.data.source.internet.UModMqttServiceContract;
import com.urbit_iot.porton.data.source.internet.UModsInternetDataSource;
import com.urbit_iot.porton.data.source.lan.UModsBLEScanner;
import com.urbit_iot.porton.data.source.lan.UModsDNSSDScanner;
import com.urbit_iot.porton.data.source.lan.UModsLANDataSource;
import com.urbit_iot.porton.data.source.lan.UModsService;
import com.urbit_iot.porton.data.source.lan.UModsTCPScanner;
import com.urbit_iot.porton.data.source.lan.UModsWiFiScanner;
import com.urbit_iot.porton.data.source.local.UModsLocalDBDataSource;

import com.urbit_iot.porton.util.GlobalConstants;
import com.urbit_iot.porton.util.dagger.Internet;
import com.urbit_iot.porton.util.dagger.Local;
import com.urbit_iot.porton.util.dagger.LanOnly;
import com.urbit_iot.porton.util.networking.UrlHostSelectionInterceptor;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * This is used by Dagger to inject the required arguments into the {@link UModsRepository}.
 */
@Module
public class UModsRepositoryModule {

    private String appUserName;
    private String appUUID;

    //TODO replace private fields with the appUserRepository instance
    public UModsRepositoryModule(String appUserName, String appUUID) {
        this.appUserName = appUserName;
        this.appUUID = appUUID;
    }

    @Singleton
    @Provides
    UModsDNSSDScanner provideUModsDNSSD(Context context, BaseSchedulerProvider mSchedulerProvider){
        RxDnssd rxDnssd = new RxDnssdBindable(context);
        return new UModsDNSSDScanner(rxDnssd, mSchedulerProvider);
    }

    @Singleton
    @Provides
    UModsBLEScanner provideUModsBLEScanner(Context context){
        RxBleClient rxBleClient= RxBleClient.create(context);
        return new UModsBLEScanner(rxBleClient);
    }

    @Singleton
    @Provides
    UModsWiFiScanner provideUModsWiFiScanner(Context context, BaseSchedulerProvider mSchedulerProvider){
        return new UModsWiFiScanner(context, mSchedulerProvider);
    }

    @Singleton
    @Provides
    UModsTCPScanner provideUModsTCPScanner(Context context, BaseSchedulerProvider schedulerProvider){
        return new UModsTCPScanner(context, schedulerProvider);
    }

    @Singleton
    @Provides
    LocationService provideLocationService(Context context, BaseSchedulerProvider mSchedulerProvider){
        return new LocationService(context,mSchedulerProvider);
    }

    @Singleton
    @Provides
    @Local
    UModsDataSource provideUModsLocalDBDataSource(Context context, BaseSchedulerProvider schedulerProvider) {
        return new UModsLocalDBDataSource(context, schedulerProvider);
    }

    @Singleton
    @Provides
    PhoneConnectivity providePhoneConnectivityInfo(Context context){
        return new PhoneConnectivity(context);
    }

    @Singleton
    @Provides
    @LanOnly
    UModsDataSource provideUModsRemoteDataSource(UModsDNSSDScanner uModsDNSSDScanner,
                                                 UModsBLEScanner uModsBLEScanner,
                                                 UModsWiFiScanner uModsWiFiScanner,
                                                 UrlHostSelectionInterceptor urlHostSelectionInterceptor,
                                                 @Named("default") UModsService defaultUModsService,
                                                 @Named("app_user") UModsService appUserUModsService,
                                                 UModsTCPScanner mUModsTCPScanner) {
        return new UModsLANDataSource(uModsDNSSDScanner, uModsBLEScanner, uModsWiFiScanner, urlHostSelectionInterceptor,defaultUModsService, appUserUModsService, mUModsTCPScanner);
        //return new FakeUModsLANDataSource();
    }

    @Singleton
    @Provides
    @Internet
    UModsDataSource provideUModsInternetDataSource(Context context, UModMqttServiceContract uModMqttService){
        return new UModsInternetDataSource(new FirmwareFileDownloader(context), uModMqttService, this.appUserName);
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
    Dispatcher provideDispatcher(){
        /*
        ExecutorService exec = new ThreadPoolExecutor(
                20,
                20,
                1,
                TimeUnit.HOURS,
                new LinkedBlockingQueue<>());
        return new Dispatcher(exec);
        */
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(10);
        return dispatcher;
    }

    @Provides
    @Singleton
    @Named("default")
    OkHttpClient provideDigestAuthDefaultOkHttpClient(UrlHostSelectionInterceptor urlHostSelectionInterceptor, Dispatcher dispatcher){
        final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(new Credentials("urbit", "urbit"));
        final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
        return new OkHttpClient.Builder()
                .addInterceptor(urlHostSelectionInterceptor)
                .authenticator(new CachingAuthenticatorDecorator(digestAuthenticator,authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                .dispatcher(dispatcher)
                .connectTimeout(4000L, TimeUnit.MILLISECONDS)
                //.readTimeout(5000L, TimeUnit.MILLISECONDS)
                //.writeTimeout(5000L,TimeUnit.MILLISECONDS)
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
        final Credentials credentials = new Credentials(this.appUserName,this.appUUID);
        final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);
        final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
        return new OkHttpClient.Builder()
                .addInterceptor(urlHostSelectionInterceptor)
                .authenticator(new CachingAuthenticatorDecorator(digestAuthenticator,authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                .connectTimeout(4000L, TimeUnit.MILLISECONDS)
                //.readTimeout(5000L, TimeUnit.MILLISECONDS)
                //.writeTimeout(5000L,TimeUnit.MILLISECONDS)
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

    /*
    @Provides
    @Singleton
    ObservableMqttClient provideObservableMqttClient(){
        MqttAsyncClient asyncClient = null;
        MemoryPersistence persistence = new MemoryPersistence();

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        //TODO analyse the better options for the use case when we need constant connection. Hint: MqttCallbackExtended
        mqttConnectOptions.setAutomaticReconnect(true);//TODO is it better or connection reattempt when communicating??
        mqttConnectOptions.setCleanSession(false);
        //mqttConnectOptions.setConnectionTimeout(5);

        try {
            asyncClient = new MqttAsyncClient(
                    "tcp://"
                            + GlobalConstants.MQTT_BROKER__IP_ADDRESS
                            +":"
                            + GlobalConstants.MQTT_BROKER__PORT,
                    this.appUserName,persistence);
        } catch (Exception mqttExc){
            mqttExc.printStackTrace();
            Log.e("provideMqtt", "" + mqttExc.getMessage());
            if (mqttExc instanceof MqttException) {
                Log.e("provideMqtt","message"
                        + ((MqttException) mqttExc).getMessage()
                        + "\n reason "
                        + ((MqttException) mqttExc).getReasonCode()
                        + "\n cause " + ((MqttException) mqttExc).getCause());
            }
        }
        if (asyncClient == null){
            return null;
        } else {
            return PahoObservableMqttClient.builder(asyncClient)
                    .setConnectOptions(mqttConnectOptions)
                    .build();
        }
    }
    */

    /*
    @Provides
    @Singleton
    UModMqttService provideUModMqttService(ObservableMqttClient observableMqttClient, Gson gson){
        return new UModMqttService(observableMqttClient,gson,this.appUserName);
    }
    */

    /*
    @Provides
    @Singleton
    AndroidPahoClientRxWrap provideClientRxWrap(Context context){
        MqttAndroidClient mqttAndroidClient = new MqttAndroidClient(
                context,
                "tcp://"
                        + GlobalConstants.MQTT_BROKER__IP_ADDRESS
                        +":"
                        + GlobalConstants.MQTT_BROKER__PORT,
                MqttClient.generateClientId());
        AndroidPahoClientRxWrap mqttClientRxWrapper = new AndroidPahoClientRxWrap(mqttAndroidClient);
        mqttAndroidClient.setCallback(mqttClientRxWrapper);
        return mqttClientRxWrapper;
    }
    */

    @Provides
    @Singleton
    PahoClientRxWrap providePahoClientRxWrap(BaseSchedulerProvider schedulerProvider){
        MqttAsyncClient asyncClient = null;
        MemoryPersistence memoryPersistence = new MemoryPersistence();
        try {
             asyncClient = new MqttAsyncClient(
                    "tcp://" + GlobalConstants.MQTT_BROKER__IP_ADDRESS +":" +
                            GlobalConstants.MQTT_BROKER__PORT,
                     this.appUserName
                     ,memoryPersistence);
        } catch (MqttException mqttExc) {
            Log.e("providePahoClientRxWrap","message"
                    + mqttExc.getMessage()
                    + "\n reason "
                    + mqttExc.getReasonCode()
                    + "\n cause " + mqttExc.getCause());
        }
        if (asyncClient == null){
            return null;
        } else {
            PahoClientRxWrap pahoClientRxWrap = new PahoClientRxWrap(asyncClient, schedulerProvider);
            asyncClient.setCallback(pahoClientRxWrap);
            return pahoClientRxWrap;
        }
    }


    @Provides
    @Singleton
    UModMqttServiceContract provideUModMqttServiceContract(PahoClientRxWrap clientRxWrap, Gson gson, BaseSchedulerProvider schedulerProvider){
        return new SimplifiedUModMqttService(clientRxWrap,this.appUserName,gson, schedulerProvider);
    }
}