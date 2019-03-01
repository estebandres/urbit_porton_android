package com.urbit_iot.porton.appuser;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.VisibleForTesting;
import androidx.test.espresso.IdlingResource;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.pwittchen.reactivewifi.AccessRequester;
import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.R;
import com.urbit_iot.porton.util.ActivityUtils;
import com.urbit_iot.porton.util.EspressoIdlingResource;

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
        /*
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        */

        AppUserFragment appUserFragment =
                (AppUserFragment) getSupportFragmentManager().
                        findFragmentById(R.id.appuser_content_frame);

        if (appUserFragment == null) {
            appUserFragment = AppUserFragment.newInstance();
        }

        ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                appUserFragment, R.id.appuser_content_frame);

        PortONApplication portONApplication = (PortONApplication) getApplication();
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
                .appUserRepositoryComponent(portONApplication.getAppUserRepositoryComponentSingleton())
                .schedulerProviderComponent(portONApplication.getSchedulerProviderComponentSingleton())
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

    @Override
    protected void onStop() {
        super.onStop();
        mAppUserPresenter.unsubscribe();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAppUserPresenter.unsubscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAppUserPresenter.unsubscribe();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mAppUserPresenter.unsubscribe();
    }
}