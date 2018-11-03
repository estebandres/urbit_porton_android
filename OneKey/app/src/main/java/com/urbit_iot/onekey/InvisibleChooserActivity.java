package com.urbit_iot.onekey;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.gson.Gson;
import com.urbit_iot.onekey.appuser.AppUserActivity;
import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.umods.UModsActivity;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.loggly.SteveLogglyTimberTree;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

/**
 * Created by andresteve07 on 12/7/17.
 */

public class InvisibleChooserActivity extends AppCompatActivity {
    @Inject
    RxSharedPreferences rxSharedPreferences;

    @Inject
    @Named("app_user")
    Gson gsonInstance;

    @Inject
    SteveLogglyTimberTree logglyTimberTree;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OneKeyApplication oneKeyApplication = (OneKeyApplication) getApplication();

        DaggerChooserComponent.builder()
                .applicationModule(oneKeyApplication.getApplicationModule())
                .schedulerProviderComponent(oneKeyApplication.getSchedulerProviderComponentSingleton())
                .build()
                .inject(this);

        //Preference<Boolean> userRegistered = rxSharedPreferences.getBoolean(GlobalConstants.SP_APPUSER_REGISTERED_KEY,false);

        Preference<String> serializedAppUser = rxSharedPreferences.getString(GlobalConstants.SP_KEY__APPUSER);

        Intent firstActivityIntent;

        //TODO: Should I use some Intent FLAGS?
        if(serializedAppUser.isSet() && !serializedAppUser.get().isEmpty()){
            Log.d("chooser", "USER_DATA: " + serializedAppUser.get());
            AppUser appUser = gsonInstance.fromJson(serializedAppUser.get(),AppUser.class);

            firstActivityIntent = new Intent(this, UModsActivity.class);
            firstActivityIntent.putExtra(UModsActivity.APP_USER_NAME,appUser.getUserName());
            firstActivityIntent.putExtra(UModsActivity.APP_UUID,appUser.getAppUUID());

            //Bugfender.setDeviceString("user.phone", appUser.getUserName());
            //Bugfender.setDeviceString("user.appuid", appUser.getAppUUID());

            //final LogglyTree logglyTree = new LogglyTree(GlobalConstants.LOGGLY_TOKEN);

            logglyTimberTree.tag("username." + appUser.getUserName()
                    + "," + "appuid." + appUser.getAppUUID());
            Timber.plant(logglyTimberTree);

            Crashlytics.setUserIdentifier(appUser.getAppUUID());
            //Crashlytics.setUserEmail("user@fabric.io");
            Crashlytics.setUserName(appUser.getUserName());


        } else {
            firstActivityIntent = new Intent(this, AppUserActivity.class);
        }

        startActivity(firstActivityIntent);
        finish();
    }

}
