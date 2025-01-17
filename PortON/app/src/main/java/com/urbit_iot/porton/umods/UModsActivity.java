package com.urbit_iot.porton.umods;

import android.os.Bundle;
import androidx.annotation.VisibleForTesting;
import com.google.android.material.navigation.NavigationView;
import androidx.test.espresso.IdlingResource;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.urbit_iot.porton.R;
import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.util.ActivityUtils;
import com.urbit_iot.porton.util.EspressoIdlingResource;

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


        PortONApplication portONApplication = (PortONApplication) getApplication();

        // Create the presenter
        DaggerUModsComponent.builder()
                .uModsRepositoryComponent(portONApplication.createUModsRepositoryComponentSingleton(appUserPhoneNumber,appUUID))
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
                menuItem -> {
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
    }
}