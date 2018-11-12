package com.urbit_iot.porton.umodsnotification;

import com.urbit_iot.porton.umods.UModsPresenter;

import dagger.Module;
import dagger.Provides;

/**
 * This is a Dagger module. We use this to pass in the View dependency to the
 * {@link UModsPresenter}.
 */
@Module
public class UModsNotifPresenterModule {

    private final UModsNotifContract.View mView;

    public UModsNotifPresenterModule(UModsNotifContract.View view) {
        mView = view;
    }

    @Provides
    UModsNotifContract.View provideUModsNotifContractView() {
        return mView;
    }

}
