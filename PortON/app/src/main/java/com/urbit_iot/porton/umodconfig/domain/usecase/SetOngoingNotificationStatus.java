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

package com.urbit_iot.porton.umodconfig.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.porton.CompletableUseCase;
import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Marks a task as completed.
 */
public class SetOngoingNotificationStatus extends CompletableUseCase<SetOngoingNotificationStatus.RequestValues> {

    private final UModsRepository uModsRepository;

    @Inject
    public SetOngoingNotificationStatus(@NonNull UModsRepository uModsRepository,
                                        @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        this.uModsRepository = checkNotNull(uModsRepository, "tasksRepository cannot be null!");
    }

    @Override
    protected void complete(RequestValues values) {
        uModsRepository.setUModNotificationStatus(values.getUModUUID(), values.getUModOngoingNotifEnabled());
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final String mUModUUID;
        private final Boolean uModOngoingNotifEnabled;

        public RequestValues(@NonNull String uModUUID, @NonNull Boolean uModOngoingNotifEnabled) {
            mUModUUID = checkNotNull(uModUUID, "completedTask cannot be null!");
            this.uModOngoingNotifEnabled = checkNotNull(uModOngoingNotifEnabled, "uModOngoingNotifEnabled cannot be null!");
        }

        public String getUModUUID() {
            return mUModUUID;
        }

        public Boolean getUModOngoingNotifEnabled(){
            return this.uModOngoingNotifEnabled;
        }
    }
}
