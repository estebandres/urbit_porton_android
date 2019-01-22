package com.urbit_iot.porton;

import android.content.Context;

import com.urbit_iot.porton.appuser.data.source.AppUserDataSource;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.util.dagger.Local;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Mockito.mock;

//@Module
public class TestingApplicationModule extends ApplicationModule {

    public TestingApplicationModule(Context context) {
        super(context);
    }

    /*
    @Provides
    @Singleton
    */
    AppUserRepository provideAppUserRepository(@Local AppUserDataSource appUserDataSource) {
        return mock(AppUserRepository.class);
    }
}
