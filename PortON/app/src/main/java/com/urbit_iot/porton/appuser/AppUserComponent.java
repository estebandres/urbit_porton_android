package com.urbit_iot.porton.appuser;

import com.urbit_iot.porton.appuser.data.source.AppUserRepositoryComponent;
import com.urbit_iot.porton.util.dagger.FragmentScoped;
import com.urbit_iot.porton.util.schedulers.SchedulerProviderComponent;

import dagger.Component;

/**
 * Created by andresteve07 on 11/14/17.
 */
@FragmentScoped
@Component(dependencies = {AppUserRepositoryComponent.class, SchedulerProviderComponent.class},
        modules = {AppUserPresenterModule.class})
public interface AppUserComponent {

    void inject(AppUserActivity appUserActivity);
}
