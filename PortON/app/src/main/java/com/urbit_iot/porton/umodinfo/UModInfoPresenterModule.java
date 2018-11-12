package com.urbit_iot.porton.umodinfo;

import dagger.Module;
import dagger.Provides;

/**
 * This is a Dagger module. We use this to pass in the View dependency to the
 * {@link UModInfoPresenter}.
 */
@Module
public class UModInfoPresenterModule {

    private final UModInfoContract.View mView;

    private final String mTaskId;

    public UModInfoPresenterModule(UModInfoContract.View view, String taskId) {
        mView = view;
        mTaskId = taskId;
    }

    @Provides
    UModInfoContract.View provideTaskDetailContractView() {
        return mView;
    }

    @Provides
    String provideTaskId() {
        return mTaskId;
    }
}
