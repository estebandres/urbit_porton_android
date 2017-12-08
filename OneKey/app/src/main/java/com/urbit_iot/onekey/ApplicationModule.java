package com.urbit_iot.onekey;

import android.content.Context;
import android.preference.PreferenceManager;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.urbit_iot.onekey.util.GlobalConstants;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

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

    @Provides
    @Singleton
    Gson provideGsonInstance(){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        return gsonBuilder.create();
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