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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.urbit_iot.onekey.R;
import com.urbit_iot.onekey.usersxumod.UModUsersActivity;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main UI for the add task screen. Users can enter a task title and description.
 */
public class UModConfigFragment extends Fragment implements UModConfigContract.View {

    public static final String ARGUMENT_CONFIG_UMOD_ID = "EDIT_TASK_ID";
    public static final String ARGUMENT_UMOD_USERS = "UMOD_UUID";
    private static final int REQUEST_EDIT_TASK = 1;

    private UModConfigContract.Presenter mPresenter;

    private TextView mTitle;

    private TextView mDescription;

    public static UModConfigFragment newInstance() {
        return new UModConfigFragment();
    }

    public UModConfigFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.subscribe();
    }

    @Override
    public void setPresenter(@NonNull UModConfigContract.Presenter presenter) {
        mPresenter = checkNotNull(presenter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FloatingActionButton fab =
                (FloatingActionButton) getActivity().findViewById(R.id.fab_edit_task_done);
        fab.setImageResource(R.drawable.ic_done);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mPresenter.saveUMod(mTitle.getText().toString(), mDescription.getText().toString());
                mPresenter.getUModSystemInfo(mTitle.getText().toString());
            }
        });

        Button uModButton = (Button) getActivity().findViewById(R.id.users_button);
        uModButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.adminUModUsers();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.addtask_frag, container, false);
        mTitle = (TextView) root.findViewById(R.id.add_task_title);
        mDescription = (TextView) root.findViewById(R.id.add_task_description);

        setHasOptionsMenu(true);
        setRetainInstance(true);
        return root;
    }

    @Override
    public void showEmptyUModError() {
        Snackbar.make(mTitle, getString(R.string.empty_task_message), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void showUModsList() {
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    @Override
    public void setUModUUID(String title) {
        mTitle.setText(title);
    }

    @Override
    public void setUModIPAddress(String description) {
        mDescription.setText(description);
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void showEditUModUsers(@NonNull String taskId) {
        Intent intent = new Intent(getContext(), UModUsersActivity.class);
        intent.putExtra(ARGUMENT_UMOD_USERS, taskId);
        startActivityForResult(intent, REQUEST_EDIT_TASK);
    }
}
