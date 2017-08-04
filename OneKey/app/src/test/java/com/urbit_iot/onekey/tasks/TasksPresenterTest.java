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

package com.urbit_iot.onekey.tasks;

import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.umods.UModsFilterType;
import com.urbit_iot.onekey.umods.domain.usecase.ClearAlienUMods;
import com.urbit_iot.onekey.umods.domain.usecase.DisableUModNotification;
import com.urbit_iot.onekey.umods.domain.usecase.EnableUModNotification;
import com.urbit_iot.onekey.umods.domain.usecase.GetUMods;
import com.urbit_iot.onekey.umods.UModsContract;
import com.urbit_iot.onekey.umods.UModsPresenter;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;
import com.urbit_iot.onekey.util.schedulers.ImmediateSchedulerProvider;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import rx.Observable;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the implementation of {@link UModsPresenter}
 */
public class TasksPresenterTest {

    private static List<UMod> TASKS;

    @Mock
    private UModsRepository mTasksRepository;

    @Mock
    private UModsContract.View mTasksView;

    private BaseSchedulerProvider mSchedulerProvider;

    private UModsPresenter mTasksPresenter;

    @Before
    public void setupTasksPresenter() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this);

        // Make the sure that all schedulers are immediate.
        mSchedulerProvider = new ImmediateSchedulerProvider();

        // Get a reference to the class under test
        mTasksPresenter = givenTasksPresenter();

        // The presenter won't update the view unless it's active.
        when(mTasksView.isNotificationDisabled()).thenReturn(true);

        // We subscribe the tasks to 3, with one active and two completed
        TASKS = Lists.newArrayList(new UMod("Title1", "Description1"),
                new UMod("Title2", "Description2", true), new UMod("Title3", "Description3", true));
    }

    private UModsPresenter givenTasksPresenter() {
        GetUMods getUMods = new GetUMods(mTasksRepository, mSchedulerProvider);
        EnableUModNotification enableUModNotification = new EnableUModNotification(mTasksRepository, mSchedulerProvider);
        DisableUModNotification disableUModNotification = new DisableUModNotification(mTasksRepository, mSchedulerProvider);
        ClearAlienUMods clearAlienUMods = new ClearAlienUMods(mTasksRepository, mSchedulerProvider);

        return new UModsPresenter(mTasksView, getUMods, enableUModNotification, disableUModNotification, clearAlienUMods);
    }

    @Test
    public void loadAllTasksFromRepositoryAndLoadIntoView() {
        // Given an initialized TasksPresenter with initialized tasks
        when(mTasksRepository.getTasks()).thenReturn(Observable.just(TASKS));
        // When loading of Tasks is requested
        mTasksPresenter.setFiltering(UModsFilterType.ALL_TASKS);
        mTasksPresenter.loadUMods(true);

        // Then progress indicator is shown
        verify(mTasksView).setLoadingIndicator(true);
        // Then progress indicator is hidden and all tasks are shown in UI
        verify(mTasksView).setLoadingIndicator(false);
    }

    @Test
    public void loadActiveTasksFromRepositoryAndLoadIntoView() {
        // Given an initialized TasksPresenter with initialized tasks
        when(mTasksRepository.getTasks()).thenReturn(Observable.just(TASKS));
        // When loading of Tasks is requested
        mTasksPresenter.setFiltering(UModsFilterType.ACTIVE_TASKS);
        mTasksPresenter.loadUMods(true);

        // Then progress indicator is hidden and active tasks are shown in UI
        verify(mTasksView).setLoadingIndicator(false);
    }

    @Test
    public void loadCompletedTasksFromRepositoryAndLoadIntoView() {
        // Given an initialized TasksPresenter with initialized tasks
        when(mTasksRepository.getTasks()).thenReturn(Observable.just(TASKS));
        // When loading of Tasks is requested
        mTasksPresenter.setFiltering(UModsFilterType.COMPLETED_TASKS);
        mTasksPresenter.loadUMods(true);

        // Then progress indicator is hidden and completed tasks are shown in UI
        verify(mTasksView).setLoadingIndicator(false);
    }

    @Test
    public void clickOnFab_ShowsAddTaskUi() {
        // When adding a new task
        mTasksPresenter.addNewUMod();

        // Then add task UI is shown
        verify(mTasksView).showAddUMod();
    }

    @Test
    public void clickOnTask_ShowsDetailUi() {
        // Given a stubbed active task
        UMod requestedTask = new UMod("Details Requested", "For this task");

        // When open task details is requested
        mTasksPresenter.openUModDetails(requestedTask);

        // Then task detail UI is shown
        verify(mTasksView).showUModConfigUi(any(String.class));
    }

    @Test
    public void completeTask_ShowsTaskMarkedComplete() {
        // Given a stubbed task
        UMod task = new UMod("Details Requested", "For this task");
        // And no tasks available in the repository
        when(mTasksRepository.getTasks()).thenReturn(Observable.<List<UMod>>empty());

        // When task is marked as complete
        mTasksPresenter.enableUModNotification(task);

        // Then repository is called and task marked complete UI is shown
        verify(mTasksRepository).completeTask(task.getId());
        verify(mTasksView).showUModNotificationEnabled();
    }

    @Test
    public void activateTask_ShowsTaskMarkedActive() {
        // Given a stubbed completed task
        UMod task = new UMod("Details Requested", "For this task", true);
        // And no tasks available in the repository
        when(mTasksRepository.getTasks()).thenReturn(Observable.<List<UMod>>empty());
        mTasksPresenter.loadUMods(true);

        // When task is marked as activated
        mTasksPresenter.disableUModNotification(task);

        // Then repository is called and task marked active UI is shown
        verify(mTasksRepository).activateTask(task.getId());
        verify(mTasksView).showUModNotificationDisabled();
    }

    @Test
    public void unavailableTasks_ShowsError() {
        // Given that no tasks are available in the repository
        when(mTasksRepository.getTasks()).thenReturn(Observable.<List<UMod>>error(new Exception()));

        // When tasks are loaded
        mTasksPresenter.setFiltering(UModsFilterType.ALL_TASKS);
        mTasksPresenter.loadUMods(true);

        // Then an error message is shown
        verify(mTasksView).showLoadingUModsError();
    }
}
