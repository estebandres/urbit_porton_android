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

package com.urbit_iot.porton;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

public abstract class CompletableUseCase <Q extends RxUseCase.RequestValues>
        extends SimpleUseCase<Q, RxUseCase.NoResponseValues> {

    public CompletableUseCase(Scheduler subscribeOn, Scheduler observeOn) {
        super(subscribeOn, observeOn);
    }

    @Override
    public final Observable<RxUseCase.NoResponseValues> buildUseCase(final Q requestValues) {
        return Observable.create(new Observable.OnSubscribe<RxUseCase.NoResponseValues>() {
            @Override
            public void call(Subscriber<? super RxUseCase.NoResponseValues> subscriber) {
                try {
                    complete(requestValues);
                    subscriber.onNext(NoResponseValues.INSTANCE);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    protected abstract void complete(Q requestValues);
}
