package com.urbit_iot.onekey.usersxumod;

import com.urbit_iot.onekey.OneKeyApplication;
import com.urbit_iot.onekey.data.source.UModsRepositoryComponent;
import com.urbit_iot.onekey.util.dagger.FragmentScoped;
import com.urbit_iot.onekey.util.schedulers.SchedulerProviderComponent;

import dagger.Component;

/**
 * This is a Dagger component. Refer to {@link OneKeyApplication} for the list of Dagger components
 * used in this application.
 * <P>
 * Because this component depends on the {@link UModsRepositoryComponent}, which is a singleton, a
 * scope must be specified. All fragment components use a custom scope for this purpose.
 */
@FragmentScoped
@Component(dependencies = {UModsRepositoryComponent.class, SchedulerProviderComponent.class},
        modules = UModUsersPresenterModule.class)
public interface UModUsersComponent {
	
    void inject(UModUsersActivity activity);
}
