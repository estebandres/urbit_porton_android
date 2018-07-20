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

package com.urbit_iot.onekey.umodconfig.domain.usecase;

import android.location.Location;
import android.support.annotation.NonNull;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Retrieves a {@link UMod} from the {@link UModsRepository}.
 */
public class GetCurrentLocation extends SimpleUseCase<GetCurrentLocation.RequestValues, GetCurrentLocation.ResponseValues> {

    private final UModsRepository uModsRepository;

    @Inject
    public GetCurrentLocation(@NonNull UModsRepository tasksRepository,
                              @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        uModsRepository = tasksRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(RequestValues values) {
        return uModsRepository.getCurrentLocation()
                .map(ResponseValues::new);
    }


    public static final class RequestValues implements RxUseCase.RequestValues {
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private Location currentLocation;

        public ResponseValues(@NonNull Location currentLocation) {
            this.currentLocation = checkNotNull(currentLocation, "currentLocation cannot be null!");
        }

        public Location getCurrentLocation() {
            return this.currentLocation;
        }
    }
}