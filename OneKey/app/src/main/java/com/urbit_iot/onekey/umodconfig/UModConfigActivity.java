package com.urbit_iot.onekey.umodconfig;

import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.urbit_iot.onekey.R;
import com.urbit_iot.onekey.OneKeyApplication;
import com.urbit_iot.onekey.util.ActivityUtils;
import com.urbit_iot.onekey.util.EspressoIdlingResource;

import javax.inject.Inject;

/**
 * Displays an add or edit task screen.
 */
public class UModConfigActivity extends AppCompatActivity {

    public static final int REQUEST_ADD_TASK = 1;

    @Inject
    UModConfigPresenter mAddEditTasksPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.umod_config_act);

        // Set up the toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        UModConfigFragment uModConfigFragment =
                (UModConfigFragment) getSupportFragmentManager().findFragmentById(
                        R.id.appuser_content_frame);

        String uModUUID;
        uModUUID = getIntent().getStringExtra(UModConfigFragment.ARGUMENT_CONFIG_UMOD_ID);

        if (uModConfigFragment == null) {
            uModConfigFragment = UModConfigFragment.newInstance();

            if (getIntent().hasExtra(UModConfigFragment.ARGUMENT_CONFIG_UMOD_ID)) {
                actionBar.setTitle(R.string.umod_config_bar_title);
                Bundle bundle = new Bundle();
                bundle.putString(UModConfigFragment.ARGUMENT_CONFIG_UMOD_ID, uModUUID);
                uModConfigFragment.setArguments(bundle);
            } else {
                actionBar.setTitle(R.string.add_task);
            }

            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    uModConfigFragment, R.id.appuser_content_frame);
        }

        OneKeyApplication oneKeyApplication = (OneKeyApplication) getApplication();
        // Create the presenter
        DaggerUModConfigComponent.builder()
                .uModConfigPresenterModule(new UModConfigPresenterModule(uModConfigFragment, uModUUID))
                .uModsRepositoryComponent(oneKeyApplication.getUModsRepositoryComponentSingleton())
                .schedulerProviderComponent(oneKeyApplication.getSchedulerProviderComponentSingleton())
                .build()
                .inject(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAddEditTasksPresenter.unsubscribe();
        Log.d("STEVE","UModConfig onPause");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d("STEVE","UModConfig onPostResume");
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
