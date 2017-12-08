package com.urbit_iot.onekey;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by andresteve07 on 12/7/17.
 */
@Singleton
@Component(modules = ApplicationModule.class)
public interface ChooserComponent {

    void inject(InvisibleChooserActivity invisibleChooserActivity);

}
