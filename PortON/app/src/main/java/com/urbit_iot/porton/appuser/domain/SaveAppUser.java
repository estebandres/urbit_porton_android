/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.urbit_iot.porton.appuser.domain;

import androidx.annotation.NonNull;

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.appuser.AppUserViewModel;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Updates or creates a new {@link UMod} in the {@link UModsRepository}.
 */
public class SaveAppUser extends SimpleUseCase<SaveAppUser.RequestValues, SaveAppUser.ResponseValues> {

    private final AppUserRepository appUserRepository;

    @Inject
    public SaveAppUser(@NonNull AppUserRepository appUserRepository,
                       @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        this.appUserRepository = appUserRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {
        return appUserRepository.createAppUser(values.getAppUserViewModel().getPhoneNumber())
                .map(ResponseValues::new);
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final AppUserViewModel mAppUserViewModel;

        public RequestValues(@NonNull AppUserViewModel appUser) {
            mAppUserViewModel = checkNotNull(appUser, "appUser cannot be null!");
        }

        public AppUserViewModel getAppUserViewModel() {
            return mAppUserViewModel;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final AppUser mAppUser;

        public ResponseValues(@NonNull AppUser appUser) {
            mAppUser = checkNotNull(appUser, "appUser cannot be null!");
        }

        public AppUser getmAppUser() {
            return mAppUser;
        }
    }
}
