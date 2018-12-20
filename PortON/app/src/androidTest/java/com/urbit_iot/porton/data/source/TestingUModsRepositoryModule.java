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
    private UModsRepository repoMock;

    public TestingUModsRepositoryModule(String appUserName, String appUUID) {
        super(appUserName, appUUID);
    }

    @Provides
    @Singleton
    UModsRepository provideUModsRepository(){
        Log.d("STEVE","Creando el REPO!");
        return mock(UModsRepository.class);
    }

}