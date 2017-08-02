package com.urbit_iot.onekey;

import android.app.Application;

import com.urbit_iot.onekey.addedittask.AddEditTaskComponent;
import com.urbit_iot.onekey.data.source.DaggerTasksRepositoryComponent;
import com.urbit_iot.onekey.data.source.TasksRepositoryComponent;
import com.urbit_iot.onekey.statistics.StatisticsComponent;
import com.urbit_iot.onekey.taskdetail.TaskDetailComponent;
import com.urbit_iot.onekey.tasks.TasksComponent;
import com.urbit_iot.onekey.util.schedulers.DaggerSchedulerProviderComponent;
import com.urbit_iot.onekey.util.schedulers.SchedulerProviderComponent;
import com.urbit_iot.onekey.util.schedulers.SchedulerProviderModule;

/**
 * Even though Dagger2 allows annotating a {@link dagger.Component} as a singleton, the code itself
 * must ensure only one instance of the class is created. Therefore, we create a custom
 * {@link Application} class to store a singleton reference to the {@link
 * TasksRepositoryComponent}.
 * <P>
 * The application is made of 5 Dagger components, as follows:<BR />
 * {@link TasksRepositoryComponent}: the data (it encapsulates a db and server data)<BR />
 * {@link TasksComponent}: showing the list of to do items, including marking them as
 * completed<BR />
 * {@link AddEditTaskComponent}: adding or editing a to do item<BR />
 * {@link TaskDetailComponent}: viewing details about a to do item, inlcuding marking it as
 * completed and deleting it<BR />
 * {@link StatisticsComponent}: viewing statistics about your to do items<BR />
 */
public class ToDoApplication extends Application {

    private TasksRepositoryComponent mRepositoryComponent;

    private SchedulerProviderComponent mSchedulerProviderComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        mSchedulerProviderComponent = DaggerSchedulerProviderComponent.builder()
                .schedulerProviderModule(new SchedulerProviderModule()).build();

        mRepositoryComponent = DaggerTasksRepositoryComponent.builder()
                .applicationModule(new ApplicationModule((getApplicationContext())))
                .schedulerProviderComponent(mSchedulerProviderComponent)
                .build();
    }

    public TasksRepositoryComponent getTasksRepositoryComponent() {
        return mRepositoryComponent;
    }

    public SchedulerProviderComponent getSchedulerProviderComponent() {
        return mSchedulerProviderComponent;
    }

}
