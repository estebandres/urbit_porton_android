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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Strings;
import com.urbit_iot.onekey.umodconfig.domain.usecase.GetUMod;
import com.urbit_iot.onekey.umodconfig.domain.usecase.SaveUMod;
import com.urbit_iot.onekey.data.UMod;

import javax.inject.Inject;

import rx.Subscriber;

/**
 * Listens to user actions from the UI ({@link UModConfigFragment}), retrieves the data and
 * updates
 * the UI as required.
 */
public class UModConfigPresenter implements UModConfigContract.Presenter {

    private final UModConfigContract.View mAddTaskView;

    private final GetUMod mGetUMod;

    private final SaveUMod mSaveUMod;

    @Nullable
    private String mTaskId;

    /**
     * Dagger strictly enforces that arguments not marked with {@code @Nullable} are not injected
     * with {@code @Nullable} values.
     */
    @Inject
    public UModConfigPresenter(@Nullable String taskId,
                               @NonNull UModConfigContract.View addTaskView,
                               @NonNull GetUMod getUMod, @NonNull SaveUMod saveUMod) {
        mTaskId = taskId;
        mAddTaskView = addTaskView;
        mGetUMod = getUMod;
        mSaveUMod = saveUMod;
    }

    /**
     * Method injection is used here to safely reference {@code this} after the object is created.
     * For more information, see Java Concurrency in Practice.
     */
    @Inject
    void setupListeners() {
        mAddTaskView.setPresenter(this);
    }

    @Override
    public void subscribe() {
        if (mTaskId != null) {
            populateUMod();
        }
    }

    @Override
    public void unsubscribe() {
        mGetUMod.unsubscribe();
        mSaveUMod.unsubscribe();
    }

    @Override
    public void saveTask(String title, String description) {
        if (isNewTask()) {
            createTask(title, description);
        } else {
            updateTask(title, description);
        }
    }

    @Override
    public void populateUMod() {
        if (mTaskId == null) {
            throw new RuntimeException("populateUMod() was called but task is new.");
        }
        mGetUMod.execute(new GetUMod.RequestValues(mTaskId), new Subscriber<GetUMod.ResponseValues>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                showEmptyTaskError();
            }

            @Override
            public void onNext(GetUMod.ResponseValues response) {
                showTask(response.getUMod());
            }
        });
    }

    @Override
    public void adminUModUsers() {
        if (Strings.isNullOrEmpty(mTaskId)) {
            mAddTaskView.showEmptyUModError();
            return;
        }
        mAddTaskView.showEditUModUsers(mTaskId);
    }

    private void showTask(UMod task) {
        // The view may not be able to handle UI updates anymore
        if (mAddTaskView.isActive()) {
            mAddTaskView.setUModUUID(task.getUUID());
            mAddTaskView.setUModIPAddress(task.getLANIPAddress());
        }
    }

    private void showSaveError() {
        // Show error, log, etc.
    }

    private void showEmptyTaskError() {
        // The view may not be able to handle UI updates anymore
        if (mAddTaskView.isActive()) {
            mAddTaskView.showEmptyUModError();
        }
    }

    private boolean isNewTask() {
        return mTaskId == null;
    }

    private void createTask(String title, String description) {
        UMod newTask = new UMod(title, description,true);
        if (newTask.isEmpty()) {
            mAddTaskView.showEmptyUModError();
        } else {
            mSaveUMod.execute(new SaveUMod.RequestValues(newTask), new Subscriber<SaveUMod.ResponseValues>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    showSaveError();
                }

                @Override
                public void onNext(SaveUMod.ResponseValues responseValues) {
                    mAddTaskView.showUModsList();
                }
            });
        }
    }

    private void updateTask(String title, String description) {
        if (mTaskId == null) {
            throw new RuntimeException("updateTask() was called but task is new.");
        }
        UMod newTask = new UMod(title, description, true);
        mSaveUMod.execute(new SaveUMod.RequestValues(newTask), new Subscriber<SaveUMod.ResponseValues>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                showSaveError();
            }

            @Override
            public void onNext(SaveUMod.ResponseValues responseValues) {
                // After an edit, go back to the list.
                mAddTaskView.showUModsList();
            }
        });
    }
}
