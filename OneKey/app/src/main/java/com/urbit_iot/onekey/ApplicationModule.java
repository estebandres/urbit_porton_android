package com.urbit_iot.onekey;

import android.content.Context;
import android.preference.PreferenceManager;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.urbit_iot.onekey.data.source.lan.UModsService;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.dagger.DigestAuth;
import com.urbit_iot.onekey.util.networking.UrlHostSelectionInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Url;

/**
 * This is a Dagger module. We use this to pass in the Context dependency to the
 * {@link
 * com.urbit_iot.onekey.data.source.UModsRepositoryComponent}.
 */
@Module
public final class ApplicationModule {

    private final Context mContext;

    ApplicationModule(Context context) {
        mContext = context;
    }

    @Provides
    Context provideContext() {
        return mContext;
    }

    @Provides
    @Singleton
    RxSharedPreferences provideRxSharedPreferences(){
        return RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(this.mContext));
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
    @DigestAuth
    UrlHostSelectionInterceptor provideUrlHostSelectorInterceptor(){
        return new UrlHostSelectionInterceptor();
    }

    @Provides
    @Singleton
    @DigestAuth
    Credentials provideDigestAuthCredentials(){
        return new Credentials("urbit", "urbit");
    }

    @Provides
    @Singleton
    @DigestAuth
    OkHttpClient provideDigestAuthOkHttpClient(@DigestAuth Credentials credentials,
                                               @DigestAuth UrlHostSelectionInterceptor urlHostSelectionInterceptor){
        final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);
        final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
        return new OkHttpClient.Builder()
                .addInterceptor(urlHostSelectionInterceptor)
                .authenticator(new CachingAuthenticatorDecorator(digestAuthenticator,authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                .build();
    }

    @Provides
    @Singleton
    @DigestAuth
    Retrofit provideDigestAuthRetrofit(@DigestAuth OkHttpClient okHttpClient, Gson gson){
        return new Retrofit.Builder()
                .baseUrl(GlobalConstants.LAN_DEFAULT_URL)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
    }

    @Provides
    @Singleton
    @DigestAuth
    UModsService provideUModsService(@DigestAuth Retrofit retrofit){
        return retrofit.create(UModsService.class);
    }

    /*
    @Provides
    @Singleton
    @Named(GlobalConstants.DAGGER_APP_UUID_NAME)
    String provideAppUUID(RxSharedPreferences rxSharedPreferences){
        Preference<String> appUUIDPref = rxSharedPreferences.getString(GlobalConstants.SP_APP_UUID_KEY);
        if(appUUIDPref.isSet()){
            return appUUIDPref.get();
        } else {
            return null;
        }
    }
    */
}