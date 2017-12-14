/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        setContentView(R.layout.addtask_act);

        // Set up the toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        UModConfigFragment uModConfigFragment =
                (UModConfigFragment) getSupportFragmentManager().findFragmentById(
                        R.id.appuser_content_frame);

        String taskId = getIntent().getStringExtra(uModConfigFragment.ARGUMENT_CONFIG_UMOD_ID);

        if (uModConfigFragment == null) {
            uModConfigFragment = uModConfigFragment.newInstance();

            if (getIntent().hasExtra(uModConfigFragment.ARGUMENT_CONFIG_UMOD_ID)) {
                actionBar.setTitle(R.string.edit_task);
                Bundle bundle = new Bundle();
                bundle.putString(uModConfigFragment.ARGUMENT_CONFIG_UMOD_ID, taskId);
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
                .uModConfigPresenterModule(new UModConfigPresenterModule(uModConfigFragment, taskId))
                .uModsRepositoryComponent(oneKeyApplication.getUModsRepositoryComponentSingleton())
                .schedulerProviderComponent(oneKeyApplication.getSchedulerProviderComponentSingleton())
                .build()
                .inject(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("STEVE","UMods onPause");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d("STEVE","UMods onPostResume");
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
