package com.urbit_iot.onekey.umodusers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;


import com.urbit_iot.onekey.R;
import com.urbit_iot.onekey.OneKeyApplication;
import com.urbit_iot.onekey.umodconfig.UModConfigFragment;

import com.urbit_iot.onekey.util.ActivityUtils;
import com.urbit_iot.onekey.util.EspressoIdlingResource;

import javax.inject.Inject;

/**
 * Created by steve-urbit on 02/08/17.
 */

public class UModUsersActivity extends AppCompatActivity {
    private static final String CURRENT_FILTERING_KEY = "CURRENT_FILTERING_KEY";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    @Inject
    UModUsersPresenter uModUsersPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.umod_users_act);

        // Set up the toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.umod_users_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle(R.string.umod_users_bar_title);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String uModUUID = getIntent().getStringExtra(UModConfigFragment.ARGUMENT_UMOD_USERS);

        UModUsersFragment umodUsersFragment =
                (UModUsersFragment) getSupportFragmentManager().findFragmentById(R.id.appuser_content_frame);
        if (umodUsersFragment == null) {
            // Create the fragment
            umodUsersFragment = UModUsersFragment.newInstance();
            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), umodUsersFragment, R.id.appuser_content_frame);
        }

        OneKeyApplication oneKeyApplication = (OneKeyApplication) getApplication();
        // Create the presenter
        DaggerUModUsersComponent.builder()
                .uModsRepositoryComponent(oneKeyApplication.getUModsRepositoryComponentSingleton())
                .schedulerProviderComponent(oneKeyApplication.getSchedulerProviderComponentSingleton())
                .uModUsersPresenterModule(new UModUsersPresenterModule(umodUsersFragment)).build()
                .inject(this);

        uModUsersPresenter.setUModUUID(uModUUID);
        // Load previously saved state, if available.
        if (savedInstanceState != null) {
            UModUsersFilterType currentFiltering =
                    (UModUsersFilterType) savedInstanceState.getSerializable(CURRENT_FILTERING_KEY);
            uModUsersPresenter.setFiltering(currentFiltering);
        }

        //TODO: Check if this works correctly.
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            uModUsersPresenter.setContactsAccessGranted(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                uModUsersPresenter.setContactsAccessGranted(true);
            } else {
                uModUsersPresenter.setContactsAccessGranted(false);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(CURRENT_FILTERING_KEY, uModUsersPresenter.getFiltering());

        super.onSaveInstanceState(outState);
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

