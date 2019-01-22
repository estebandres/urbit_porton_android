package com.urbit_iot.porton.data.source;

import android.util.Log;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Observable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Module
public class TestingUModsRepositoryModule extends UModsRepositoryModule{

    public TestingUModsRepositoryModule(String appUserName, String appUUID) {
        super(appUserName, appUUID);
    }

    @Provides
    @Singleton
    UModsRepository provideUModsRepository(){
        return mock(UModsRepository.class);
    }

}