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
import com.urbit_iot.onekey.data.commands.Command;
import com.urbit_iot.onekey.data.commands.OpenCloseCmd;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class OpenCloseUMod extends SimpleUseCase<OpenCloseUMod.RequestValues, OpenCloseUMod.ResponseValues> {

    private final UModsRepository mUModsRepository;

    @Inject
    public OpenCloseUMod(@NonNull UModsRepository uModsRepository,
                         @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {
        /*
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }
        */
        Command.CommandRequest request = new OpenCloseCmd.Request(6666666,"asdf","lkjh");
        return mUModsRepository.openCloseUMod(values.getUMod(), request)
                .map(new Func1<OpenCloseCmd.Response, ResponseValues>() {
                    @Override
                    public ResponseValues call(OpenCloseCmd.Response response) {
                        return new ResponseValues(response);
                    }
                });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final UMod uMod;

        public RequestValues(@NonNull UMod uMod) {
            this.uMod = checkNotNull(uMod, "uMod cannot be null!");
        }

        public UMod getUMod() {
            return uMod;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final OpenCloseCmd.Response response;

        public ResponseValues(@NonNull OpenCloseCmd.Response response) {
            this.response = checkNotNull(response, "response cannot be null!");
        }

        public OpenCloseCmd.Response getResponse() {
            return response;
        }
    }
}
