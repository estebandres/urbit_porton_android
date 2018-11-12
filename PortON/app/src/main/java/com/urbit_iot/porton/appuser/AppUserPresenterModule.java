package com.urbit_iot.porton.appuser;

import dagger.Module;
import dagger.Provides;

/**
 * This is a Dagger module. We use this to pass in the View dependency to the
 * {@link AppUserPresenter}.
 */
@Module
public class AppUserPresenterModule {

    private final AppUserContract.View mView;

    public AppUserPresenterModule(AppUserContract.View view) {
        mView = view;
    }

    @Provides
    AppUserContract.View provideAppUserFragment() {
        return mView;
    }
}
