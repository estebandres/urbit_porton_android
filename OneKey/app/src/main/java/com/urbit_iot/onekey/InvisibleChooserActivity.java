package com.urbit_iot.onekey;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.gson.Gson;
import com.urbit_iot.onekey.appuser.AppUserActivity;
import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.umods.UModsActivity;
import com.urbit_iot.onekey.util.GlobalConstants;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by andresteve07 on 12/7/17.
 */

public class InvisibleChooserActivity extends AppCompatActivity {
    @Inject
    RxSharedPreferences rxSharedPreferences;

    @Inject
    @Named("app_user")
    Gson gsonInstance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OneKeyApplication oneKeyApplication = (OneKeyApplication) getApplication();

        DaggerChooserComponent.builder()
                .applicationModule(oneKeyApplication.getApplicationModule())
                .build()
                .inject(this);

        //Preference<Boolean> userRegistered = rxSharedPreferences.getBoolean(GlobalConstants.SP_APPUSER_REGISTERED_KEY,false);

        Preference<String> serializedAppUser = rxSharedPreferences.getString(GlobalConstants.SP_SERIALIZED_APPUSER_KEY);

        Intent firstActivityIntent;

        //TODO: Should I use some Intent FLAGS?
        if(serializedAppUser.isSet() && !serializedAppUser.get().isEmpty()){
            Log.d("chooser", "USER_DATA: " + serializedAppUser.get());
            AppUser appUser = gsonInstance.fromJson(serializedAppUser.get(),AppUser.class);
            firstActivityIntent = new Intent(this, UModsActivity.class);
            firstActivityIntent.putExtra(UModsActivity.APP_USER_PHONE_NUMBER,appUser.getPhoneNumber());
            firstActivityIntent.putExtra(UModsActivity.APP_UUID,appUser.getAppUUID());
        } else {
            firstActivityIntent = new Intent(this, AppUserActivity.class);
        }

        startActivity(firstActivityIntent);
        finish();
    }

}
