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

import android.location.Location;
import android.support.annotation.NonNull;

import com.google.common.base.Strings;
import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Retrieves a {@link UMod} from the {@link UModsRepository}.
 */
public class GetCurrentLocation extends SimpleUseCase<GetCurrentLocation.RequestValues, GetCurrentLocation.ResponseValues> {

    //TODO move to locations repository
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
                .filter(location -> location!=null)
                .switchIfEmpty(Observable.error(new Exception("agefgs")))
                .retry((integer, throwable) -> integer < 3)
                .flatMap(location ->{
                    return uModsRepository.getAddressFromLocation(location)
                            .filter(address -> address!=null)
                            .switchIfEmpty(Observable.error(new Exception("No location data found")))
                            .retry((integer, throwable) -> integer < 3)
                            .flatMap(address ->{
                                String locationString = null;
                                if (address.getThoroughfare() != null
                                        && address.getFeatureName() != null){
                                    if(address.getThoroughfare().trim()
                                            .equalsIgnoreCase(address.getFeatureName().trim())){
                                        locationString = address.getThoroughfare();
                                    } else {
                                        locationString = address.getThoroughfare()
                                                + "  "
                                                + address.getFeatureName();
                                    }
                                } else {
                                    if (!Strings.isNullOrEmpty(address.getAddressLine(0))){
                                        locationString = address.getAddressLine(0);
                                    }
                                }
                                return Observable.just(
                                        new ResponseValues(location, locationString));
                                    }
                            );
                });
                //.map(location -> new ResponseValues(location,""));
    }


    public static final class RequestValues implements RxUseCase.RequestValues {
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private Location currentLocation;
        private String locationAddress;

        public ResponseValues(@NonNull Location currentLocation, @NonNull String locationString) {
            this.currentLocation = checkNotNull(currentLocation, "currentLocation cannot be null!");
            this.locationAddress = locationString;
        }

        public Location getCurrentLocation() {
            return this.currentLocation;
        }

        public String getLocationAddress() {
            return locationAddress;
        }
    }

}
