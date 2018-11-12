package com.urbit_iot.porton.util.schedulers;

import dagger.Module;
import dagger.Provides;

/**
 * This is a Dagger module. We use this to pass in the BaseSchedulerProvider dependency to the
 * {@link
 * com.urbit_iot.porton.util.schedulers.SchedulerProviderComponent}.
 */
@Module
public class SchedulerProviderModule {

    private BaseSchedulerProvider schedulerProvider;

    public SchedulerProviderModule() {
        this.schedulerProvider = new SingleSchedulersProvider();
    }

    @Provides
    BaseSchedulerProvider provideSchedulerProvider() {
        return this.schedulerProvider;
    }
}
