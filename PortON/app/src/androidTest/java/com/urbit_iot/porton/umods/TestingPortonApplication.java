package com.urbit_iot.porton.umods;

import com.urbit_iot.porton.ApplicationModule;
import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.appuser.data.source.AppUserRepositoryComponent;
import com.urbit_iot.porton.data.source.DaggerTestingUModsRepositoryComponent;
import com.urbit_iot.porton.data.source.TestingUModsRepositoryModule;
import com.urbit_iot.porton.data.source.UModsRepositoryComponent;
import com.urbit_iot.porton.util.schedulers.SchedulerProviderComponent;

public class TestingPortonApplication extends PortONApplication {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public UModsRepositoryComponent createUModsRepositoryComponentSingleton(String appUserPhoneNumber, String appUUIDHash) {
        if (this.mUModsRepositoryComponent == null){
            this.mUModsRepositoryComponent = DaggerTestingUModsRepositoryComponent.builder()
                    .applicationModule(applicationModule)
                    .schedulerProviderComponent(mSchedulerProviderComponent)
                    .testingUModsRepositoryModule(new TestingUModsRepositoryModule(appUserPhoneNumber, appUUIDHash))
                    .build();
        }
        return this.mUModsRepositoryComponent;
    }

    @Override
    public UModsRepositoryComponent getUModsRepositoryComponentSingleton() {
        return super.getUModsRepositoryComponentSingleton();
    }

    @Override
    public SchedulerProviderComponent getSchedulerProviderComponentSingleton() {
        return super.getSchedulerProviderComponentSingleton();
    }

    @Override
    public AppUserRepositoryComponent getAppUserRepositoryComponentSingleton() {
        return super.getAppUserRepositoryComponentSingleton();
    }

    @Override
    public ApplicationModule getApplicationModule() {
        return super.getApplicationModule();
    }
}
