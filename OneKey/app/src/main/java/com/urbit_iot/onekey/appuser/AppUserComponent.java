package com.urbit_iot.onekey.appuser;

import com.urbit_iot.onekey.appuser.data.source.AppUserRepositoryComponent;
import com.urbit_iot.onekey.util.dagger.FragmentScoped;
import com.urbit_iot.onekey.util.schedulers.SchedulerProviderComponent;

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
