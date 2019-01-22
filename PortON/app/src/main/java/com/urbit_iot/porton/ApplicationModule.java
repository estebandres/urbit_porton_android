package com.urbit_iot.porton;

import android.content.Context;
import android.preference.PreferenceManager;

import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.urbit_iot.porton.appuser.data.source.AppUserDataSource;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.appuser.data.source.localfile.AppUserLocalFileDataSource;
import com.urbit_iot.porton.util.GlobalConstants;
import com.urbit_iot.porton.util.dagger.Local;
import com.urbit_iot.porton.util.loggly.SteveLogglyTimberTree;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This is a Dagger module. We use this to pass in the Context dependency to the
 * {@link
 * com.urbit_iot.porton.data.source.UModsRepositoryComponent}.
 */
@Module
public class ApplicationModule {

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

    @Provides
    @Singleton
    @Named("app_user")
    Gson provideGsonInstance(){
        return  new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    @Provides
    @Singleton
    SteveLogglyTimberTree provideSteveLogglyTimberTree(RxSharedPreferences rxSharedPreferences,
                                                       @Named("app_user")Gson gsonInstance,
                                                       BaseSchedulerProvider mSchedulerProvider){
        return new SteveLogglyTimberTree(GlobalConstants.LOGGLY_TOKEN, rxSharedPreferences, gsonInstance, mSchedulerProvider);
    }

    @Singleton
    @Provides
    @Local
    AppUserDataSource provideAppUserLocalDataSource(RxSharedPreferences rxSharedPreferences,
                                                    @Named("app_user") Gson gson) {
        return new AppUserLocalFileDataSource(rxSharedPreferences, gson);
    }

    @Provides
    @Singleton
    AppUserRepository provideAppUserRepository(@Local AppUserDataSource appUserDataSource){
        return new AppUserRepository(appUserDataSource);
    }

    /*
    @Provides
    @Singleton
    @Named(GlobalConstants.DAGGER_APP_UUID_NAME)
    String provideAppUUID(RxSharedPreferences rxSharedPreferences){
        Preference<String> appUUIDPref = rxSharedPreferences.getString(GlobalConstants.SP_KEY__APP_UUID);
        if(appUUIDPref.isSet()){
            return appUUIDPref.get();
        } else {
            return null;
        }
    }
    */
}