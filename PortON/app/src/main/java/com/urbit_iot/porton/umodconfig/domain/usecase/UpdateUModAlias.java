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
import android.util.Log;

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Retrieves a {@link UMod} from the {@link UModsRepository}.
 */
public class UpdateUModAlias extends SimpleUseCase<UpdateUModAlias.RequestValues, UpdateUModAlias.ResponseValues> {

    private final UModsRepository uModsRepository;

    @Inject
    public UpdateUModAlias(@NonNull UModsRepository tasksRepository,
                           @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        uModsRepository = tasksRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(RequestValues values) {
        return uModsRepository.updateUModAlias(values.getuModUUID(),values.getNewAlias())
                .map(new Func1<UMod, ResponseValues>() {
            @Override
            public ResponseValues call(UMod uMod) {
                Log.d("update_alias", uMod.toString());
                return new ResponseValues(uMod);
            }
        });
    }


    public static final class RequestValues implements RxUseCase.RequestValues {

        private final String uModUUID;
        private final String newAlias;

        public RequestValues(String uModUUID, String newAlias) {
            this.uModUUID = checkNotNull(uModUUID, "uModUUID cannot be null!");
            this.newAlias = checkNotNull(newAlias, "newAlias cannot be null!");
        }

        public String getuModUUID() {
            return uModUUID;
        }

        public String getNewAlias() {
            return newAlias;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final UMod mUMod;

        public ResponseValues(@NonNull UMod uMod) {
            mUMod = checkNotNull(uMod, "uMod cannot be null!");
        }

        public UMod getUMod() {
            return mUMod;
        }
    }
}
