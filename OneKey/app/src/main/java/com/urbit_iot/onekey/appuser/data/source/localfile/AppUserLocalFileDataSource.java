package com.urbit_iot.onekey.appuser.data.source.localfile;

import android.support.annotation.NonNull;
import android.util.Log;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.gson.Gson;
import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.appuser.data.source.AppUserDataSource;
import com.urbit_iot.onekey.util.GlobalConstants;

import java.io.FileNotFoundException;
import java.util.UUID;

import javax.inject.Inject;

import rx.Observable;

/**
 * Created by andresteve07 on 11/14/17.
 */

public class AppUserLocalFileDataSource implements AppUserDataSource {

    @NonNull
    private RxSharedPreferences rxSharedPreferences;
    @NonNull
    private Gson gson;

    //Preference<Boolean> userRegistered;
    private Preference<String> serializedAppUserPref;
    private Preference<String> appUUIDPref;

    @Inject
    public AppUserLocalFileDataSource(@NonNull RxSharedPreferences rxSharedPreferences,
                                      @NonNull Gson gson){
        this.rxSharedPreferences = rxSharedPreferences;
        this.gson = gson;
        //this.userRegistered = this.rxSharedPreferences.getBoolean(GlobalConstants.SP_APPUSER_REGISTERED_KEY);
        this.serializedAppUserPref = this.rxSharedPreferences.getString(GlobalConstants.SP_SERIALIZED_APPUSER_KEY);
        this.appUUIDPref = this.rxSharedPreferences.getString(GlobalConstants.SP_APP_UUID_KEY);
    }

    @Override
    public Observable<AppUser> saveAppUser(AppUser appUser) {
        if(!serializedAppUserPref.isSet()){
            Log.d("appusr_repo",appUser.toString());
            String appUserString = this.gson.toJson(appUser);
            Log.d("appusr_repo",appUserString);
            this.serializedAppUserPref.set(appUserString);
            return Observable.just(appUser);
        } else {
            return Observable.error(new Exception("AppUser already exists in the preferences."));
        }
    }

    @Override
    public Observable<AppUser> getAppUser() {
            if(serializedAppUserPref.isSet() && !serializedAppUserPref.get().isEmpty()){
                AppUser appUser = gson.fromJson(serializedAppUserPref.get(),AppUser.class);
                return Observable.just(appUser);
            } else {
                return Observable.error(new FileNotFoundException("AppUser could not be retrieve from the preferences or is an empty String."));
            }
    }

    @Override
    public Observable<String> getAppUUID() {
        if(this.appUUIDPref.isSet() && !this.appUUIDPref.get().isEmpty()){
            Log.d("appusr_repo_sp","AppUUID found on the preferences: " + appUUIDPref.get());
        } else {
            String appUserStr = UUID.randomUUID().toString();
            appUUIDPref.set(appUserStr);
            Log.d("appusr_repo","Newly created AppUUID: " + appUserStr);
        }
        return Observable.just(appUUIDPref.get());
    }
}