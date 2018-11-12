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

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;

import static com.google.common.base.Preconditions.checkNotNull;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.source.UModsRepository;

/**
 * Updates or creates a new {@link UMod} in the {@link UModsRepository}.
 */
public class SaveUMod extends SimpleUseCase<SaveUMod.RequestValues, SaveUMod.ResponseValues> {

    private final UModsRepository uModsRepository;

    @Inject
    public SaveUMod(@NonNull UModsRepository tasksRepository,
                    @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        uModsRepository = tasksRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {
        return Observable.create(new Observable.OnSubscribe<ResponseValues>() {
            @Override
            public void call(Subscriber<? super ResponseValues> subscriber) {
                try {
                    UMod uMod = values.getUMod();
                    uModsRepository.saveUMod(uMod);
                    subscriber.onNext(new ResponseValues(uMod));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final UMod mUMod;

        public RequestValues(@NonNull UMod uMod) {
            mUMod = checkNotNull(uMod, "task cannot be null!");
        }

        public UMod getUMod() {
            return mUMod;
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
