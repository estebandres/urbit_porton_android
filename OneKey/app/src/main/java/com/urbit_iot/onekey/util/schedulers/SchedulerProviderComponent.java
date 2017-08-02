package com.urbit_iot.onekey.util.schedulers;

import dagger.Component;

/**
 * Created by t-xu on 11/8/16.
 */
@Component(modules = SchedulerProviderModule.class)
public interface SchedulerProviderComponent {

    BaseSchedulerProvider schedulerProvider();
}
