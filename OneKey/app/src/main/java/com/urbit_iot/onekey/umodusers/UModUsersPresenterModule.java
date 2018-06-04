package com.urbit_iot.onekey.umodusers;

import dagger.Module;
import dagger.Provides;

/**
 * This is a Dagger module. We use this to pass in the View dependency to the
 * {@link UModUsersPresenter}.
 */
@Module
public class UModUsersPresenterModule {

    private final UModUsersContract.View mView;

    public UModUsersPresenterModule(UModUsersContract.View view) {
        mView = view;
    }

    @Provides
    UModUsersContract.View provideUModUsersContractView() {
        return mView;
    }

}
