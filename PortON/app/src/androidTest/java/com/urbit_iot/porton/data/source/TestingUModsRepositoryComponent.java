package com.urbit_iot.porton.data.source;


import com.urbit_iot.porton.ApplicationModule;
import com.urbit_iot.porton.PortONApplication;

import com.urbit_iot.porton.TestingApplicationModule;
import com.urbit_iot.porton.umods.UModsActivityTest;
import com.urbit_iot.porton.util.schedulers.SchedulerProviderComponent;

import javax.inject.Singleton;

import dagger.Component;

/**
 * This is a Dagger component. Refer to {@link PortONApplication} for the list of Dagger components
 * used in this application.
 * <P>
 * Even though Dagger allows annotating a {@link Component @Component} as a singleton, the code
 * itself must ensure only one instance of the class is created. This is done in {@link
 * PortONApplication}.
 */
@Singleton
@Component(dependencies = SchedulerProviderComponent.class,
        modules = {TestingUModsRepositoryModule.class, ApplicationModule.class})
public interface TestingUModsRepositoryComponent extends UModsRepositoryComponent{

    void inject(UModsActivityTest activityTest);

}
