package com.urbit_iot.porton.umodconfig;

import android.os.Bundle;
import androidx.annotation.VisibleForTesting;
import androidx.test.espresso.IdlingResource;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;

import com.urbit_iot.porton.R;
import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.util.ActivityUtils;
import com.urbit_iot.porton.util.EspressoIdlingResource;

import javax.inject.Inject;

/**
 * Displays an add or edit task screen.
 */
public class UModConfigActivity extends AppCompatActivity {

    public static final int REQUEST_ADD_TASK = 1;

    @Inject
    UModConfigPresenter mUModConfigsPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.umod_config_act);

        // Set up the toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.umod_config_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(R.string.umod_config_bar_title);
        }

        UModConfigFragment uModConfigFragment =
                (UModConfigFragment) getSupportFragmentManager().findFragmentById(
                        R.id.appuser_content_frame);

        String uModUUID;
        uModUUID = getIntent().getStringExtra(UModConfigFragment.ARGUMENT_CONFIG_UMOD_ID);

        if (uModConfigFragment == null) {
            uModConfigFragment = UModConfigFragment.newInstance();

            actionBar.setTitle(R.string.umod_config_bar_title);
            Bundle bundle = new Bundle();
            bundle.putString(UModConfigFragment.ARGUMENT_CONFIG_UMOD_ID, uModUUID);
            uModConfigFragment.setArguments(bundle);

            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    uModConfigFragment, R.id.appuser_content_frame);
        }

        PortONApplication portONApplication = (PortONApplication) getApplication();
        // Create the presenter
        DaggerUModConfigComponent.builder()
                .uModConfigPresenterModule(new UModConfigPresenterModule(uModConfigFragment, uModUUID))
                .uModsRepositoryComponent(portONApplication.getUModsRepositoryComponentSingleton())
                .build()
                .inject(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUModConfigsPresenter.unsubscribe();
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

    @Override
    protected void onStop() {
        super.onStop();
        mUModConfigsPresenter.unsubscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUModConfigsPresenter.unsubscribe();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mUModConfigsPresenter.unsubscribe();
    }
}
