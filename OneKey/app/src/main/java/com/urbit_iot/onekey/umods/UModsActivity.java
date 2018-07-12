package com.urbit_iot.onekey.umods;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.NavigationView;
import android.support.test.espresso.IdlingResource;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;


import com.github.pwittchen.reactivewifi.AccessRequester;
import com.urbit_iot.onekey.R;
import com.urbit_iot.onekey.OneKeyApplication;
import com.urbit_iot.onekey.statistics.StatisticsActivity;
import com.urbit_iot.onekey.umodsnotification.UModsNotifService;
import com.urbit_iot.onekey.util.ActivityUtils;
import com.urbit_iot.onekey.util.EspressoIdlingResource;
import com.urbit_iot.onekey.util.GlobalConstants;

import javax.inject.Inject;

/**
 * Created by steve-urbit on 02/08/17.
 */

public class UModsActivity extends AppCompatActivity {
    private static final String CURRENT_FILTERING_KEY = "CURRENT_FILTERING_KEY";
    public static final String APP_USER_NAME = "APP_USER_NAME";
    public static final String APP_UUID = "APP_UUID";

    private static final int REQUEST_APP_USER = 1;
    private DrawerLayout mDrawerLayout;

    @Inject
    UModsPresenter mUModsPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.umods_act);

        // Set up the toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.umods_toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_menu);
            ab.setDisplayHomeAsUpEnabled(true);
        }


        // Set up the navigation drawer.
        mDrawerLayout = (DrawerLayout) findViewById(R.id.umods_drawer_layout);
        mDrawerLayout.setStatusBarBackground(R.color.colorPrimaryDark);
        NavigationView navigationView = (NavigationView) findViewById(R.id.umods_nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }

        String appUserPhoneNumber = getIntent().getStringExtra(APP_USER_NAME);
        String appUUID = getIntent().getStringExtra(APP_UUID);

        UModsFragment umodsFragment =
                (UModsFragment) getSupportFragmentManager().findFragmentById(R.id.umods_content_frame);
        if (umodsFragment == null) {
            // Create the fragment
            umodsFragment = UModsFragment.newInstance();
            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), umodsFragment, R.id.umods_content_frame);
        }


        OneKeyApplication oneKeyApplication = (OneKeyApplication) getApplication();

        // Create the presenter
        DaggerUModsComponent.builder()
                .uModsRepositoryComponent(oneKeyApplication.createUModsRepositoryComponentSingleton(appUserPhoneNumber,appUUID))
                .schedulerProviderComponent(oneKeyApplication.getSchedulerProviderComponentSingleton())
                .uModsPresenterModule(new UModsPresenterModule(umodsFragment))
                .build()
                .inject(this);

        // Load previously saved state, if available.
        if (savedInstanceState != null) {
            UModsFilterType currentFiltering =
                    (UModsFilterType) savedInstanceState.getSerializable(CURRENT_FILTERING_KEY);
            mUModsPresenter.setFiltering(currentFiltering);
        }
    }

    public void fatalError(String message, Throwable exception){
        Log.e("umods_act", message);
        this.finishAffinity();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(CURRENT_FILTERING_KEY, mUModsPresenter.getFiltering());

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Open the navigation drawer when the home icon is selected from the toolbar.
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.list_navigation_menu_item:
                                // Do nothing, we're already on that screen
                                break;
                                /*
                            case R.id.statistics_navigation_menu_item:
                                Intent intent =
                                        new Intent(UModsActivity.this, StatisticsActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                break;
                                */
                            default:
                                break;
                        }
                        // Close the navigation drawer when an item is selected.
                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });
    }

    @VisibleForTesting
    public IdlingResource getCountingIdlingResource() {
        return EspressoIdlingResource.getIdlingResource();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUModsPresenter.unsubscribe();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUModsPresenter.unsubscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUModsPresenter.unsubscribe();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mUModsPresenter.unsubscribe();
        mUModsPresenter.subscribe();
    }
}