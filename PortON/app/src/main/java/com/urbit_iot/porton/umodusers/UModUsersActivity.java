package com.urbit_iot.porton.umodusers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.test.espresso.IdlingResource;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;


import com.urbit_iot.porton.R;
import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.umodconfig.UModConfigFragment;

import com.urbit_iot.porton.util.ActivityUtils;
import com.urbit_iot.porton.util.EspressoIdlingResource;

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

        PortONApplication portONApplication = (PortONApplication) getApplication();
        // Create the presenter
        DaggerUModUsersComponent.builder()
                .uModsRepositoryComponent(portONApplication.getUModsRepositoryComponentSingleton())
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

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("USERS_ACTIVITY","ON STOP!");
        uModUsersPresenter.unsubscribe();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("USERS_ACTIVITY","ON PAUSE!");
        uModUsersPresenter.unsubscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("USERS_ACTIVITY","ON DESTROY!");
        uModUsersPresenter.unsubscribe();
    }

    @Override
    protected void onResume(){
        super.onResume();
        //Log.d("USERS_ACTIVITY","ON RESUME!");
        //uModUsersPresenter.unsubscribe(); COMMENTED SO THE RPC EXECUTION DOESNT GET INTERRUPTED!
    }
}

