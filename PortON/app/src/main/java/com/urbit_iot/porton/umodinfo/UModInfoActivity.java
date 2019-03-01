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

package com.urbit_iot.porton.umodinfo;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.urbit_iot.porton.R;
import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.util.ActivityUtils;

import javax.inject.Inject;

/**
 * Displays task details screen.
 */
public class UModInfoActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "TASK_ID";

    @Inject
    UModInfoPresenter mUModInfoPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.taskdetail_act);

        // Set up the toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        // Get the requested task id
        String taskId = getIntent().getStringExtra(EXTRA_TASK_ID);

        UModInfoFragment UModInfoFragment = (UModInfoFragment) getSupportFragmentManager()
                .findFragmentById(R.id.appuser_content_frame);

        if (UModInfoFragment == null) {
            UModInfoFragment = UModInfoFragment.newInstance(taskId);

            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    UModInfoFragment, R.id.appuser_content_frame);
        }

        PortONApplication portONApplication = (PortONApplication) getApplication();
        // Create the presenter
        DaggerUModInfoComponent.builder()
                .uModInfoPresenterModule(new UModInfoPresenterModule(UModInfoFragment, taskId))
                .uModsRepositoryComponent(portONApplication.getUModsRepositoryComponentSingleton())
                .build()
                .inject(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
