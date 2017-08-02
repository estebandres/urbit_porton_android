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

package com.urbit_iot.onekey.addedittask;

import com.urbit_iot.onekey.addedittask.domain.usecase.GetTask;
import com.urbit_iot.onekey.addedittask.domain.usecase.SaveTask;
import com.urbit_iot.onekey.data.Task;
import com.urbit_iot.onekey.data.source.TasksRepository;
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
 * Unit tests for the implementation of {@link AddEditTaskPresenter}.
 */
public class AddEditTaskPresenterTest {

    @Mock
    private TasksRepository mTasksRepository;

    @Mock
    private AddEditTaskContract.View mAddEditTaskView;

    private BaseSchedulerProvider mSchedulerProvider;

    private AddEditTaskPresenter mAddEditTaskPresenter;

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
        mAddEditTaskPresenter = givenEditTaskPresenter("1");

        // When the presenter is asked to save a task
        mAddEditTaskPresenter.saveTask("New Task Title", "Some Task Description");

        // Then a task is saved in the repository and the view updated
        verify(mTasksRepository).saveTask(any(Task.class)); // saved to the model
        verify(mAddEditTaskView).showTasksList(); // shown in the UI
    }


    @Test
    public void saveTask_emptyTaskShowsErrorUi() {
        // Get a reference to the class under test
        mAddEditTaskPresenter = givenEditTaskPresenter(null);

        // When the presenter is asked to save an empty task
        mAddEditTaskPresenter.saveTask("", "");

        // Then an empty not error is shown in the UI
        verify(mAddEditTaskView).showEmptyTaskError();
    }

    @Test
    public void saveExistingTaskToRepository_showsSuccessMessageUi() {
        // Get a reference to the class under test
        mAddEditTaskPresenter = givenEditTaskPresenter("1");

        // When the presenter is asked to save an existing task
        mAddEditTaskPresenter.saveTask("New Task Title", "Some Task Description");

        // Then a task is saved in the repository and the view updated
        verify(mTasksRepository).saveTask(any(Task.class)); // saved to the model
        verify(mAddEditTaskView).showTasksList(); // shown in the UI
    }

    @Test
    public void populateTask_callsRepoAndUpdatesViewOnSuccess() {
        Task testTask = new Task("TITLE", "DESCRIPTION");
        when(mTasksRepository.getTask(testTask.getId())).thenReturn(Observable.just(testTask));

        // Get a reference to the class under test
        mAddEditTaskPresenter = givenEditTaskPresenter(testTask.getId());

        // When the presenter is asked to populate an existing task
        mAddEditTaskPresenter.populateTask();

        // Then the task repository is queried and the view updated
        verify(mTasksRepository).getTask(eq(testTask.getId()));

        verify(mAddEditTaskView).setTitle(testTask.getTitle());
        verify(mAddEditTaskView).setDescription(testTask.getDescription());
    }

    @Test
    public void populateTask_callsRepoAndUpdatesViewOnError() {
        Task testTask = new Task("TITLE", "DESCRIPTION");
        when(mTasksRepository.getTask(testTask.getId())).thenReturn(
                Observable.<Task>error(new NoSuchElementException()));

        // Get a reference to the class under test
        mAddEditTaskPresenter = givenEditTaskPresenter(testTask.getId());

        // When the presenter is asked to populate an existing task
        mAddEditTaskPresenter.populateTask();

        // Then the task repository is queried and the view updated
        verify(mTasksRepository).getTask(eq(testTask.getId()));

        verify(mAddEditTaskView).showEmptyTaskError();
        verify(mAddEditTaskView, never()).setTitle(testTask.getTitle());
        verify(mAddEditTaskView, never()).setDescription(testTask.getDescription());
    }

    private AddEditTaskPresenter givenEditTaskPresenter(String taskId) {

        GetTask getTask = new GetTask(mTasksRepository, mSchedulerProvider);
        SaveTask saveTask = new SaveTask(mTasksRepository, mSchedulerProvider);

        return new AddEditTaskPresenter(taskId, mAddEditTaskView, getTask, saveTask);
    }
}
