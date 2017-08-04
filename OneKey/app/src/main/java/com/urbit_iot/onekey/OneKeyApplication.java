package com.urbit_iot.onekey;

import android.app.Application;

import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdBindable;
import com.urbit_iot.onekey.appuser.data.source.AppUserRepositoryComponent;
import com.urbit_iot.onekey.appuser.data.source.DaggerAppUserRepositoryComponent;
import com.urbit_iot.onekey.data.source.DaggerUModsRepositoryComponent;
import com.urbit_iot.onekey.umodconfig.UModConfigComponent;
import com.urbit_iot.onekey.data.source.UModsRepositoryComponent;
import com.urbit_iot.onekey.statistics.StatisticsComponent;
import com.urbit_iot.onekey.umodinfo.UModInfoComponent;
import com.urbit_iot.onekey.umods.UModsComponent;
import com.urbit_iot.onekey.util.schedulers.DaggerSchedulerProviderComponent;
import com.urbit_iot.onekey.util.schedulers.SchedulerProviderComponent;
import com.urbit_iot.onekey.util.schedulers.SchedulerProviderModule;

/**
 * Even though Dagger2 allows annotating a {@link dagger.Component} as a singleton, the code itself
 * must ensure only one instance of the class is created. Therefore, we create a custom
 * {@link Application} class to store a singleton reference to the {@link
 * UModsRepositoryComponent}.
 * <P>
 * The application is made of 5 Dagger components, as follows:<BR />
 * {@link UModsRepositoryComponent}: the data (it encapsulates a db and server data)<BR />
 * {@link UModsComponent}: showing the list of to do items, including marking them as
 * completed<BR />
 * {@link UModConfigComponent}: adding or editing a to do item<BR />
 * {@link UModInfoComponent}: viewing details about a to do item, inlcuding marking it as
 * completed and deleting it<BR />
 * {@link StatisticsComponent}: viewing statistics about your to do items<BR />
 */
public class OneKeyApplication extends Application {

    private UModsRepositoryComponent mUModsRepositoryComponent;

    private SchedulerProviderComponent mSchedulerProviderComponent;

    private AppUserRepositoryComponent mAppUserRepositoryComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        mSchedulerProviderComponent = DaggerSchedulerProviderComponent.builder()
                .schedulerProviderModule(new SchedulerProviderModule()).build();

        /*
        mUModsRepositoryComponent = DaggerTasksRepositoryComponent.builder()
                .applicationModule(new ApplicationModule((getApplicationContext())))
                .schedulerProviderComponent(mSchedulerProviderComponent)
                .build();
        */

        mUModsRepositoryComponent = DaggerUModsRepositoryComponent.builder()
                .applicationModule(new ApplicationModule((getApplicationContext())))
                .schedulerProviderComponent(mSchedulerProviderComponent)
                .build();

        mAppUserRepositoryComponent = DaggerAppUserRepositoryComponent.builder()
                .applicationModule(new ApplicationModule(getApplicationContext()))
                .schedulerProviderComponent(mSchedulerProviderComponent)
                .build();

    }

    public UModsRepositoryComponent getUModsRepositoryComponent() {
        return mUModsRepositoryComponent;
    }

    public SchedulerProviderComponent getSchedulerProviderComponent() {
        return mSchedulerProviderComponent;
    }

    public AppUserRepositoryComponent getAppUserRepositoryComponent(){
        return mAppUserRepositoryComponent;
    }

}
