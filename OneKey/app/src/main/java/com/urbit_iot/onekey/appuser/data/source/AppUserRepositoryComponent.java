package com.urbit_iot.onekey.appuser.data.source;

/**
 * Created by andresteve07 on 11/14/17.
 */

import com.urbit_iot.onekey.ApplicationModule;
import com.urbit_iot.onekey.OneKeyApplication;
import com.urbit_iot.onekey.util.schedulers.SchedulerProviderComponent;

import javax.inject.Singleton;

import dagger.Component;

/**
 * This is a Dagger component. Refer to {@link OneKeyApplication} for the list of Dagger components
 * used in this application.
 * <P>
 * Even though Dagger allows annotating a {@link Component @Component} as a singleton, the code
 * itself must ensure only one instance of the class is created. This is done in {@link
 * OneKeyApplication}.
 */
@Singleton
@Component(dependencies = SchedulerProviderComponent.class,
        modules = {AppUserRepositoryModule.class, ApplicationModule.class})
public interface AppUserRepositoryComponent {

    AppUserRepository getAppUserRepository();
}
