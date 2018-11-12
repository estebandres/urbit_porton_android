package com.urbit_iot.porton.statistics;

import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.util.dagger.FragmentScoped;
import com.urbit_iot.porton.data.source.UModsRepositoryComponent;

import dagger.Component;

/**
 * This is a Dagger component. Refer to {@link PortONApplication} for the list of Dagger components
 * used in this application.
 * <P>
 * Because this component depends on the {@link UModsRepositoryComponent}, which is a singleton, a
 * scope must be specified. All fragment components use a custom scope for this purpose.
 */
@FragmentScoped
@Component(dependencies = {UModsRepositoryComponent.class},
        modules = StatisticsPresenterModule.class)
public interface StatisticsComponent {

    void inject(StatisticsActivity statisticsActivity);
}
