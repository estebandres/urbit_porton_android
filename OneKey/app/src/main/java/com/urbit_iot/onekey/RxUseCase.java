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

package com.urbit_iot.onekey;

import rx.Observable;

public abstract class RxUseCase<Q extends RxUseCase.RequestValues, P extends RxUseCase.ResponseValues> {

    /**
     * Builds an {@link rx.Observable} which will be used when executing the current {@link RxUseCase}.
     */
    protected abstract Observable<P> buildUseCase(Q requestValues);

    /**
     * Data passed to a request.
     */
    public interface RequestValues {
    }

    /**
     * Data received from a response.
     */
    public interface ResponseValues {
    }

    public enum NoRequestValues implements RequestValues {
        INSTANCE
    }

    public enum NoResponseValues implements ResponseValues {
        INSTANCE
    }
}
