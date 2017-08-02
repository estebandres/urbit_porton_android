package com.urbit_iot.onekey.taskdetail;

import com.urbit_iot.onekey.ToDoApplication;
import com.urbit_iot.onekey.data.source.TasksRepositoryComponent;
import com.urbit_iot.onekey.util.FragmentScoped;
import com.urbit_iot.onekey.util.schedulers.SchedulerProviderComponent;

import dagger.Component;

/**
 * This is a Dagger component. Refer to {@link ToDoApplication} for the list of Dagger components
 * used in this application.
 * <P>
 * Because this component depends on the {@link TasksRepositoryComponent}, which is a singleton, a
 * scope must be specified. All fragment components use a custom scope for this purpose.
 */
@FragmentScoped
@Component(dependencies = {TasksRepositoryComponent.class, SchedulerProviderComponent.class},
        modules = TaskDetailPresenterModule.class)
public interface TaskDetailComponent {
    
    void inject(TaskDetailActivity taskDetailActivity);
}

