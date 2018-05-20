package com.urbit_iot.onekey.appuser;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.github.pwittchen.reactivewifi.AccessRequester;
import com.urbit_iot.onekey.OneKeyApplication;
import com.urbit_iot.onekey.R;
import com.urbit_iot.onekey.util.ActivityUtils;
import com.urbit_iot.onekey.util.EspressoIdlingResource;

import javax.inject.Inject;

/**
 * Created by andresteve07 on 11/9/17.
 */

public class AppUserActivity extends AppCompatActivity {

    public static final int APP_PERMISSIONS_REQUEST_FINE_AND_COARSE = 666;

    @Inject
    AppUserPresenter mAppUserPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appuser_act);

        // Set up the toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        AppUserFragment appUserFragment =
                (AppUserFragment) getSupportFragmentManager().
                        findFragmentById(R.id.appuser_content_frame);

        if (appUserFragment == null) {
            appUserFragment = AppUserFragment.newInstance();
        }

        ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                appUserFragment, R.id.appuser_content_frame);

        OneKeyApplication oneKeyApplication = (OneKeyApplication) getApplication();
        if (!AccessRequester.isLocationEnabled(getApplicationContext())){
            AccessRequester.requestLocationAccess(this);
        }

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, APP_PERMISSIONS_REQUEST_FINE_AND_COARSE);
        }

        //Create Presenter and solve dependencies
        DaggerAppUserComponent.builder()
                .appUserPresenterModule(new AppUserPresenterModule(appUserFragment))
                .appUserRepositoryComponent(oneKeyApplication.getAppUserRepositoryComponentSingleton())
                .schedulerProviderComponent(oneKeyApplication.getSchedulerProviderComponentSingleton())
                .build()
                .inject(this);
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @VisibleForTesting
    public IdlingResource getCountingIdlingResource() {
        return EspressoIdlingResource.getIdlingResource();
    }
}