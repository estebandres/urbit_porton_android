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

package com.urbit_iot.onekey.umodinfo;

import com.urbit_iot.onekey.umodconfig.domain.usecase.DeleteUMod;
import com.urbit_iot.onekey.umodconfig.domain.usecase.GetUModAndUpdateInfo;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;
import com.urbit_iot.onekey.util.schedulers.ImmediateSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link UModInfoPresenter}
 */
public class UModInfoPresenterTest {

    public static final String TITLE_TEST = "title";

    public static final String DESCRIPTION_TEST = "description";

    public static final String INVALID_TASK_ID = "";

    public static final UMod ACTIVE_TASK = new UMod(TITLE_TEST, DESCRIPTION_TEST);

    public static final UMod COMPLETED_TASK = new UMod(TITLE_TEST, DESCRIPTION_TEST, true);

    @Mock
    private UModsRepository mTasksRepository;

    @Mock
    private UModInfoContract.View mTaskDetailView;

    private BaseSchedulerProvider mSchedulerProvider;

    private UModInfoPresenter mUModInfoPresenter;

    @Before
    public void setup() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this);

        mSchedulerProvider = new ImmediateSchedulerProvider();

        // The presenter won't update the view unless it's active.
        when(mTaskDetailView.isActive()).thenReturn(true);
    }

    @Test
    public void getActiveTaskFromRepositoryAndLoadIntoView() {
        // When tasks presenter is asked to open a task
        mUModInfoPresenter = givenTaskDetailPresenter(ACTIVE_TASK.getId());
        setTaskAvailable(ACTIVE_TASK);
        mUModInfoPresenter.subscribe();

        // Then task is loaded from model, callback is captured and progress indicator is shown
        verify(mTasksRepository).getTask(eq(ACTIVE_TASK.getId()));
        verify(mTaskDetailView).setLoadingIndicator(true);

        // Then progress indicator is hidden and title, description and completion status are shown
        // in UI
        verify(mTaskDetailView).setLoadingIndicator(false);
        verify(mTaskDetailView).showTitle(TITLE_TEST);
        verify(mTaskDetailView).showDescription(DESCRIPTION_TEST);
        verify(mTaskDetailView).showCompletionStatus(false);
    }

    @Test
    public void getCompletedTaskFromRepositoryAndLoadIntoView() {
        mUModInfoPresenter = givenTaskDetailPresenter(COMPLETED_TASK.getId());
        setTaskAvailable(COMPLETED_TASK);
        mUModInfoPresenter.subscribe();

        // Then task is loaded from model, callback is captured and progress indicator is shown
        verify(mTasksRepository).getTask(
                eq(COMPLETED_TASK.getId()));
        verify(mTaskDetailView).setLoadingIndicator(true);

        // Then progress indicator is hidden and title, description and completion status are shown
        // in UI
        verify(mTaskDetailView).setLoadingIndicator(false);
        verify(mTaskDetailView).showTitle(TITLE_TEST);
        verify(mTaskDetailView).showDescription(DESCRIPTION_TEST);
        verify(mTaskDetailView).showCompletionStatus(true);
    }

    @Test
    public void getUnknownTaskFromRepositoryAndLoadIntoView() {
        // When loading of a task is requested with an invalid task ID.
        mUModInfoPresenter = givenTaskDetailPresenter(INVALID_TASK_ID);
        mUModInfoPresenter.subscribe();
        verify(mTaskDetailView).showMissingTask();
    }

    @Test
    public void deleteTask() {
        // Given an initialized UModInfoPresenter with stubbed task
        UMod task = new UMod(TITLE_TEST, DESCRIPTION_TEST);

        // When the deletion of a task is requested
        mUModInfoPresenter = givenTaskDetailPresenter(task.getId());
        mUModInfoPresenter.deleteTask();

        // Then the repository and the view are notified
        verify(mTasksRepository).deleteTask(task.getId());
        verify(mTaskDetailView).showTaskDeleted();
    }

    @Test
    public void completeTask() {
        // Given an initialized presenter with an active task
        UMod task = new UMod(TITLE_TEST, DESCRIPTION_TEST);
        mUModInfoPresenter = givenTaskDetailPresenter(task.getId());
        setTaskAvailable(task);
        mUModInfoPresenter.subscribe();

        // When the presenter is asked to complete the task
        mUModInfoPresenter.completeTask();

        // Then a request is sent to the task repository and the UI is updated
        verify(mTasksRepository).completeTask(task.getId());
        verify(mTaskDetailView).showTaskMarkedComplete();
    }

    @Test
    public void activateTask() {
        // Given an initialized presenter with a completed task
        UMod task = new UMod(TITLE_TEST, DESCRIPTION_TEST, true);
        mUModInfoPresenter = givenTaskDetailPresenter(task.getId());
        setTaskAvailable(task);
        mUModInfoPresenter.subscribe();

        // When the presenter is asked to activate the task
        mUModInfoPresenter.activateTask();

        // Then a request is sent to the task repository and the UI is updated
        verify(mTasksRepository).activateTask(task.getId());
        verify(mTaskDetailView).showTaskMarkedActive();
    }

    @Test
    public void activeTaskIsShownWhenEditing() {
        // When the edit of an ACTIVE_TASK is requested
        mUModInfoPresenter = givenTaskDetailPresenter(ACTIVE_TASK.getId());
        mUModInfoPresenter.editTask();

        // Then the view is notified
        verify(mTaskDetailView).showEditTask(ACTIVE_TASK.getId());
    }

    @Test
    public void invalidTaskIsNotShownWhenEditing() {
        // When the edit of an invalid task id is requested
        mUModInfoPresenter = givenTaskDetailPresenter(INVALID_TASK_ID);
        mUModInfoPresenter.editTask();

        // Then the edit mode is never started
        verify(mTaskDetailView, never()).showEditTask(INVALID_TASK_ID);
        // instead, the error is shown.
        verify(mTaskDetailView).showMissingTask();
    }

    private UModInfoPresenter givenTaskDetailPresenter(String id) {
        GetUModAndUpdateInfo getUModAndUpdateInfo = new GetUModAndUpdateInfo(mTasksRepository, mSchedulerProvider, appUserRepository);
        EnableUModNotification enableUModNotification = new EnableUModNotification(mTasksRepository, mSchedulerProvider);
        DisableUModNotification disableUModNotification = new DisableUModNotification(mTasksRepository, mSchedulerProvider);
        DeleteUMod deleteUMod = new DeleteUMod(mTasksRepository, mSchedulerProvider);

        return new UModInfoPresenter(id, mTaskDetailView,
                getUModAndUpdateInfo, enableUModNotification, disableUModNotification, deleteUMod);
    }

    private void setTaskAvailable(UMod task) {
        when(mTasksRepository.getTask(eq(task.getId()))).thenReturn(Observable.just(task));
    }

}
