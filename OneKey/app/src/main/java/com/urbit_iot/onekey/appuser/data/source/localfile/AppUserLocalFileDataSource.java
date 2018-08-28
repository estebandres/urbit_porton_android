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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private Preference<String> serializedAppUserPref;
    //both appUUID and appUUIDHash could be gotten from the deserialization of the appUser each time.
    private Preference<String> appUUIDPref;
    private Preference<String> appUserCredentialsHashPref;

    @Inject
    public AppUserLocalFileDataSource(@NonNull RxSharedPreferences rxSharedPreferences,
                                      @NonNull Gson gson){
        this.rxSharedPreferences = rxSharedPreferences;
        this.gson = gson;
        this.serializedAppUserPref = this.rxSharedPreferences.getString(GlobalConstants.SP_KEY__APPUSER);
        this.appUUIDPref = this.rxSharedPreferences.getString(GlobalConstants.SP_KEY__APP_UUID);
        this.appUserCredentialsHashPref = this.rxSharedPreferences.getString(GlobalConstants.SP_KEY__APP_UUID);
    }

    @Override
    public Observable<AppUser> createAppUser(AppUser appUser) {
        if (serializedAppUserPref.isSet()){
            try {
                AppUser storedAppUser = gson.fromJson(serializedAppUserPref.get(),AppUser.class);//TODO perform field validation over the storesAppUser
                return Observable.error(new Exception("AppUser already exists in the preferences."));
            } catch (Exception e){
                Log.d("appusr_file","App user entry on shared prefs but failed on deserialization.");
            }
        }
        String appUserString = this.gson.toJson(appUser);
        this.serializedAppUserPref.set(appUserString);
        return Observable.just(appUser);
    }

    //TODO call createAppUser(AppUser) to reduce code replication.
    @Override
    public Observable<AppUser> createAppUser(String appUserPhoneNumber) {
        AppUser newAppUser;
        if (serializedAppUserPref.isSet()){
            try {
                newAppUser = gson.fromJson(serializedAppUserPref.get(),AppUser.class);
                return Observable.error(new Exception("AppUser already exists in the preferences."));
            } catch (Exception e){
                Log.d("appusr_file","App user entry on shared prefs but failed on deserialization.");
            }
        }
        String appUUID = this.getAppUUID();
        String credentialsHash =
                this.getAppUserCredentialsHash(appUserPhoneNumber.replace("+","")
                + GlobalConstants.CREDENTIALS_REALM_SEPARATOR + appUUID);
        newAppUser = new AppUser(appUserPhoneNumber,appUUID,credentialsHash);
        String appUserString = this.gson.toJson(newAppUser);
        this.serializedAppUserPref.set(appUserString);
        return Observable.just(newAppUser);
    }

    @Override
    public Observable<AppUser> partiallyUpdateAppUser(AppUser appUser) {
        return null;
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


    //TODO analyse security issues. What happens if app data is deleted?
    private String getAppUUID() {
        String appUUIDStr;
        if(this.appUUIDPref.isSet()) {
            appUUIDStr = this.appUUIDPref.get();//To reduce the quantity of sharedPref lookups.
            if (!appUUIDStr.isEmpty() && appUUIDStr.length() == 36) {//TODO Add regex verification of Android.UUID
                return appUUIDStr;
            }
        }
        appUUIDStr = UUID.randomUUID().toString();
        appUUIDPref.set(appUUIDStr);
        Log.d("appusr_repo","Newly created AppUUID: " + appUUIDStr);
        return appUUIDStr;

    }

    private String getAppUserCredentialsHash(String appUserPlainTextCredentials){
        if (this.appUserCredentialsHashPref.isSet()) {
            String appUUIDHash = this.appUserCredentialsHashPref.get();//To reduce the quantity of sharedPref lookups.
            if (!appUUIDHash.isEmpty() && appUUIDHash.length() == 32) {//TODO Add regex verification of MD5
                return appUUIDHash;
            }
        }
        String hashedAppUUID = this.calculateMD5Hash(appUserPlainTextCredentials);
        appUserCredentialsHashPref.set(hashedAppUUID);
        return hashedAppUUID;
    }

    //TODO Handle exception in this method. Is there another way to generate a HASH if this fails?
    private String calculateMD5Hash(String message){
        String md5HashStr;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(message.getBytes());
            byte[] digest = md.digest();
            //BigInteger produces a number large enough to hold the translation of the byte[]
            // in the least significant portion e.g. {15,10} -> 0000...0F0A
            //That number can be represented as a string of hexadecimal numbers with two ciphers(chars) for each byte.
            String digestToStringOfHexas = String.format("%040x", new BigInteger(1, digest));
            //Since MD5 will produce a 128bits number i.e. 16 bytes or 16 hexadecimal numbers of
            // two ciphers each then the last 32 chars will correspond to the MD5 representation.
            md5HashStr = digestToStringOfHexas.substring(digestToStringOfHexas.length() - 32);
            return md5HashStr;
        } catch (NoSuchAlgorithmException e){
            md5HashStr = "12345678901234567890123456789012";//TODO improve alternative HASH generator
            return md5HashStr;
        }

    }
}