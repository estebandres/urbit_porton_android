package com.urbit_iot.porton;

import com.urbit_iot.porton.util.schedulers.SchedulerProviderComponent;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by andresteve07 on 12/7/17.
 */
@Singleton
@Component(dependencies = {SchedulerProviderComponent.class},
        modules = {ApplicationModule.class})
public interface ChooserComponent {

    void inject(InvisibleChooserActivity invisibleChooserActivity);


}
