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

import com.urbit_iot.onekey.umodconfig.domain.usecase.GetUMod;
import com.urbit_iot.onekey.umodconfig.domain.usecase.SaveUMod;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;
import com.urbit_iot.onekey.util.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.NoSuchElementException;

import rx.Observable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link UModConfigPresenter}.
 */
public class UModConfigPresenterTest {

    @Mock
    private UModsRepository mTasksRepository;

    @Mock
    private UModConfigContract.View mAddEditTaskView;

    private BaseSchedulerProvider mSchedulerProvider;

    private UModConfigPresenter mUModConfigPresenter;

    @Before
    public void setupMocksAndView() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this);

        mSchedulerProvider = new ImmediateSchedulerProvider();

        // The presenter wont't update the view unless it's active.
        when(mAddEditTaskView.isActive()).thenReturn(true);
    }

    @Test
    public void saveNewTaskToRepository_showsSuccessMessageUi() {
        // Get a reference to the class under test
        mUModConfigPresenter = givenEditTaskPresenter("1");

        // When the presenter is asked to save a task
        mUModConfigPresenter.saveUMod("New Task Title", "Some Task Description");

        // Then a task is saved in the repository and the view updated
        verify(mTasksRepository).saveTask(any(UMod.class)); // saved to the model
        verify(mAddEditTaskView).showUModsList(); // shown in the UI
    }


    @Test
    public void saveTask_emptyTaskShowsErrorUi() {
        // Get a reference to the class under test
        mUModConfigPresenter = givenEditTaskPresenter(null);

        // When the presenter is asked to save an empty task
        mUModConfigPresenter.saveUMod("", "");

        // Then an empty not error is shown in the UI
        verify(mAddEditTaskView).showEmptyUModError();
    }

    @Test
    public void saveExistingTaskToRepository_showsSuccessMessageUi() {
        // Get a reference to the class under test
        mUModConfigPresenter = givenEditTaskPresenter("1");

        // When the presenter is asked to save an existing task
        mUModConfigPresenter.saveUMod("New Task Title", "Some Task Description");

        // Then a task is saved in the repository and the view updated
        verify(mTasksRepository).saveTask(any(UMod.class)); // saved to the model
        verify(mAddEditTaskView).showUModsList(); // shown in the UI
    }

    @Test
    public void populateTask_callsRepoAndUpdatesViewOnSuccess() {
        UMod testTask = new UMod("TITLE", "DESCRIPTION");
        when(mTasksRepository.getTask(testTask.getId())).thenReturn(Observable.just(testTask));

        // Get a reference to the class under test
        mUModConfigPresenter = givenEditTaskPresenter(testTask.getId());

        // When the presenter is asked to populate an existing task
        mUModConfigPresenter.populateUMod();

        // Then the task repository is queried and the view updated
        verify(mTasksRepository).getTask(eq(testTask.getId()));

        verify(mAddEditTaskView).setUModUUID(testTask.getTitle());
        verify(mAddEditTaskView).setUModIPAddress(testTask.getDescription());
    }

    @Test
    public void populateTask_callsRepoAndUpdatesViewOnError() {
        UMod testTask = new UMod("TITLE", "DESCRIPTION");
        when(mTasksRepository.getTask(testTask.getId())).thenReturn(
                Observable.<UMod>error(new NoSuchElementException()));

        // Get a reference to the class under test
        mUModConfigPresenter = givenEditTaskPresenter(testTask.getId());

        // When the presenter is asked to populate an existing task
        mUModConfigPresenter.populateUMod();

        // Then the task repository is queried and the view updated
        verify(mTasksRepository).getTask(eq(testTask.getId()));

        verify(mAddEditTaskView).showEmptyUModError();
        verify(mAddEditTaskView, never()).setUModUUID(testTask.getTitle());
        verify(mAddEditTaskView, never()).setUModIPAddress(testTask.getDescription());
    }

    private UModConfigPresenter givenEditTaskPresenter(String taskId) {

        GetUMod getUMod = new GetUMod(mTasksRepository, mSchedulerProvider);
        SaveUMod saveUMod = new SaveUMod(mTasksRepository, mSchedulerProvider);

        return new UModConfigPresenter(taskId, mAddEditTaskView, getUMod, saveUMod);
    }
}
