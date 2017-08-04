package com.urbit_iot.onekey.usersxumod;

import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;


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
                .uModsRepositoryComponent(oneKeyApplication.getUModsRepositoryComponent())
                .schedulerProviderComponent(oneKeyApplication.getSchedulerProviderComponent())
                .uModUsersPresenterModule(new UModUsersPresenterModule(umodUsersFragment)).build()
                .inject(this);

        uModUsersPresenter.setUModUUID(uModUUID);
        // Load previously saved state, if available.
        if (savedInstanceState != null) {
            UModUsersFilterType currentFiltering =
                    (UModUsersFilterType) savedInstanceState.getSerializable(CURRENT_FILTERING_KEY);
            uModUsersPresenter.setFiltering(currentFiltering);
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

