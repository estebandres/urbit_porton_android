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

package com.urbit_iot.onekey.umods.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.umods.UModsFilterType;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class GetUMods extends SimpleUseCase<GetUMods.RequestValues, GetUMods.ResponseValues> {

    private final UModsRepository mUModsRepository;

    @Inject
    public GetUMods(@NonNull UModsRepository tasksRepository,
                    @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null!");
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }

        return mUModsRepository.getUMods()
                .flatMap(new Func1<List<UMod>, Observable<UMod>>() {
                    @Override
                    public Observable<UMod> call(List<UMod> uMods) {
                        return Observable.from(uMods);
                    }
                })
                .filter(new Func1<UMod, Boolean>() {
                    @Override
                    public Boolean call(UMod uMod) {
                        switch (values.getCurrentFiltering()) {
                            case NOTIF_EN_UMODS:
                                return uMod.isOngoingNotificationEnabled();
                            case NOTIF_DIS_UMODS:
                                return !uMod.isOngoingNotificationEnabled();
                            case ALL_UMODS:
                            default:
                                return true;
                        }
                    }
                })
                .toList()
                .map(new Func1<List<UMod>, ResponseValues>() {
                    @Override
                    public ResponseValues call(List<UMod> uMods) {
                        return new ResponseValues(uMods);
                    }
                });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final UModsFilterType mCurrentFiltering;
        private final boolean mForceUpdate;

        public RequestValues(boolean forceUpdate, @NonNull UModsFilterType currentFiltering) {
            mForceUpdate = forceUpdate;
            mCurrentFiltering = checkNotNull(currentFiltering, "currentFiltering cannot be null!");
        }

        public boolean isForceUpdate() {
            return mForceUpdate;
        }

        public UModsFilterType getCurrentFiltering() {
            return mCurrentFiltering;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final List<UMod> mUMods;

        public ResponseValues(@NonNull List<UMod> uMods) {
            mUMods = checkNotNull(uMods, "umods cannot be null!");
        }

        public List<UMod> getUMods() {
            return mUMods;
        }
    }
}
