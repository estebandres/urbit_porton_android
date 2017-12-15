package com.urbit_iot.onekey.umodconfig;

import android.support.annotation.NonNull;

import dagger.Module;
import dagger.Provides;

/**
 * This is a Dagger module. We use this to pass in the View dependency to the
 * {@link UModConfigPresenter}.
 */
@Module
public class UModConfigPresenterModule {

    private final UModConfigContract.View mView;

    private String mUModUUID;

    public UModConfigPresenterModule(UModConfigContract.View view, @NonNull String uModUUID) {
        mView = view;
        mUModUUID = uModUUID;
    }

    @Provides
    UModConfigContract.View provideAddEditTaskContractView() {
        return mView;
    }

    @Provides
    @NonNull
    String provideUModUUID() {
        return mUModUUID;
    }
}
