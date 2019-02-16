package com.urbit_iot.porton.data.source;

import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.urbit_iot.porton.ApplicationModule;
import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.data.source.internet.UModMqttServiceContract;
import com.urbit_iot.porton.data.source.lan.UModsWiFiScanner;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;
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
        modules = {UModsRepositoryModule.class, ApplicationModule.class})
public interface UModsRepositoryComponent {

    UModsRepository getUModsRepository();

    AppUserRepository getAppUserRepository();

    RxSharedPreferences getRxSharedPreferences();

    UModMqttServiceContract getUModMqttService();//TODO breaks dependecy rule!

    PhoneConnectivity getPhoneConnectivityInfo();//TODO breaks dependecy rule!

    BaseSchedulerProvider getBaseSchedulerProvider();

    UModsWiFiScanner getUModsWiFiScanner();

}
