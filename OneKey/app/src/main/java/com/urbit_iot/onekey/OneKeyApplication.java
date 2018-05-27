package com.urbit_iot.onekey;

import android.app.Application;
import android.util.Log;

import com.bugfender.sdk.Bugfender;
import com.github.tony19.loggly.LogglyClient;
import com.github.tony19.timber.loggly.LogglyTree;
import com.urbit_iot.onekey.appuser.data.source.AppUserRepositoryComponent;
import com.urbit_iot.onekey.data.source.UModsRepositoryModule;
import com.urbit_iot.onekey.umodconfig.UModConfigComponent;
import com.urbit_iot.onekey.data.source.UModsRepositoryComponent;
import com.urbit_iot.onekey.statistics.StatisticsComponent;
import com.urbit_iot.onekey.umodinfo.UModInfoComponent;
import com.urbit_iot.onekey.umods.UModsComponent;

import com.urbit_iot.onekey.util.schedulers.SchedulerProviderComponent;
import com.urbit_iot.onekey.util.schedulers.SchedulerProviderModule;

 import com.urbit_iot.onekey.appuser.data.source.DaggerAppUserRepositoryComponent;
import com.urbit_iot.onekey.util.schedulers.DaggerSchedulerProviderComponent;
import com.urbit_iot.onekey.data.source.DaggerUModsRepositoryComponent;

import timber.log.Timber;

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

    private ApplicationModule applicationModule;

    @Override
    public void onCreate() {
        super.onCreate();


        //BUGFENDER SETUP------------------------------------------------------------------
        Bugfender.init(this, "MplxVduASCOpZgp9YLGsIH3gx0Ilnmq3", BuildConfig.DEBUG);
        //Bugfender.enableLogcatLogging();
        //Bugfender.enableUIEventLogging(this);
        Bugfender.enableCrashReporting();
        Bugfender.forceSendOnce();
        Log.e("Bugfender", "DEVICE ID:  " + Bugfender.getDeviceIdentifier());
        Log.e("Bugfender", "SESSION ID:  " + Bugfender.getSessionIdentifier());

        //LOGGLY SETUP------------------------------------------------------------------
        //Done in the chooser activity...



        this.applicationModule = new ApplicationModule((getApplicationContext()));

        mSchedulerProviderComponent = DaggerSchedulerProviderComponent.builder()
                .schedulerProviderModule(new SchedulerProviderModule())
                .build();

        mAppUserRepositoryComponent = DaggerAppUserRepositoryComponent.builder()
                .applicationModule(applicationModule)
                .schedulerProviderComponent(mSchedulerProviderComponent)
                .build();
    }

    public UModsRepositoryComponent createUModsRepositoryComponentSingleton(String appUserPhoneNumber, String appUUIDHash) {
        if (this.mUModsRepositoryComponent == null){
            this.mUModsRepositoryComponent = DaggerUModsRepositoryComponent.builder()
                    .applicationModule(applicationModule)
                    .schedulerProviderComponent(mSchedulerProviderComponent)
                    .uModsRepositoryModule(new UModsRepositoryModule(appUserPhoneNumber, appUUIDHash))
                    .build();
        }
        return this.mUModsRepositoryComponent;
    }

    public UModsRepositoryComponent getUModsRepositoryComponentSingleton() {
        return this.mUModsRepositoryComponent;
    }

    public SchedulerProviderComponent getSchedulerProviderComponentSingleton() {
        return mSchedulerProviderComponent;
    }

    public AppUserRepositoryComponent getAppUserRepositoryComponentSingleton(){
        return mAppUserRepositoryComponent;
    }

    public ApplicationModule getApplicationModule(){
        return this.applicationModule;
    }

}
