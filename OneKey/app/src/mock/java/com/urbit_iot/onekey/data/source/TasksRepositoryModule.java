package com.urbit_iot.onekey.data.source;

import android.content.Context;

import com.urbit_iot.onekey.data.FakeTasksRemoteDataSource;
import com.urbit_iot.onekey.data.source.local.TasksLocalDataSource;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This is used by Dagger to inject the required arguments into the {@link TasksRepository}.
 */
@Module
public class TasksRepositoryModule {

    @Singleton
    @Provides
    @Local
    TasksDataSource provideTasksLocalDataSource(Context context, BaseSchedulerProvider schedulerProvider) {
        return new TasksLocalDataSource(context, schedulerProvider);
    }

    @Singleton
    @Provides
    @Remote
    TasksDataSource provideTasksRemoteDataSource() {
        return new FakeTasksRemoteDataSource();
    }

}