package com.urbit_iot.onekey.appuser.data.source;

import android.content.Context;

import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.urbit_iot.onekey.appuser.data.source.localfile.AppUserLocalFileDataSource;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.dagger.Local;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This is used by Dagger to inject the required arguments into the {@link AppUserRepository}.
 */
@Module
public class AppUserRepositoryModule {

    @Provides
    @Singleton
    Gson provideGsonInstance(){
        return  new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    @Singleton
    @Provides
    @Local
    AppUserDataSource provideAppUserLocalDataSource(RxSharedPreferences rxSharedPreferences,
                                                    Gson gson) {
        return new AppUserLocalFileDataSource(rxSharedPreferences, gson);
    }

}