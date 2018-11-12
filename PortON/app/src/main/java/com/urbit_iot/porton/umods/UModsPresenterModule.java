package com.urbit_iot.porton.umods;

import dagger.Module;
import dagger.Provides;

/**
 * This is a Dagger module. We use this to pass in the View dependency to the
 * {@link UModsPresenter}.
 */
@Module
public class UModsPresenterModule {

    private final UModsContract.View mView;

    public UModsPresenterModule(UModsContract.View view) {
        mView = view;
    }

    @Provides
    UModsContract.View provideUModsContractView() {
        return mView;
    }

}
