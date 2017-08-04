package com.urbit_iot.onekey.umodconfig;

import android.support.annotation.Nullable;

import dagger.Module;
import dagger.Provides;

/**
 * This is a Dagger module. We use this to pass in the View dependency to the
 * {@link UModConfigPresenter}.
 */
@Module
public class UModConfigPresenterModule {

    private final UModConfigContract.View mView;

    private String mTaskId;

    public UModConfigPresenterModule(UModConfigContract.View view, @Nullable String taskId) {
        mView = view;
        mTaskId = taskId;
    }

    @Provides
    UModConfigContract.View provideAddEditTaskContractView() {
        return mView;
    }

    @Provides
    @Nullable
    String provideTaskId() {
        return mTaskId;
    }
}
