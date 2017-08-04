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

import com.urbit_iot.onekey.CompletableUseCase;
import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;
import com.urbit_iot.onekey.data.source.UModsRepository;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Deletes tasks marked as completed.
 */
public class ClearAlienUMods extends CompletableUseCase<ClearAlienUMods.RequestValues> {

    private final UModsRepository mTasksRepository;

    @Inject
    public ClearAlienUMods(@NonNull UModsRepository tasksRepository,
                           @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null!");
    }

    @Override
    protected void complete(RequestValues values) {
        mTasksRepository.clearAlienUMods();
    }

    public static class RequestValues implements RxUseCase.RequestValues { }
}
