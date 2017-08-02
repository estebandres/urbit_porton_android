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

package com.urbit_iot.onekey.addedittask.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.Task;
import com.urbit_iot.onekey.data.source.TasksRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Updates or creates a new {@link Task} in the {@link TasksRepository}.
 */
public class SaveTask extends SimpleUseCase<SaveTask.RequestValues, SaveTask.ResponseValues> {

    private final TasksRepository mTasksRepository;

    @Inject
    public SaveTask(@NonNull TasksRepository tasksRepository,
                    @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mTasksRepository = tasksRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {
        return Observable.create(new Observable.OnSubscribe<ResponseValues>() {
            @Override
            public void call(Subscriber<? super ResponseValues> subscriber) {
                try {
                    Task task = values.getTask();
                    mTasksRepository.saveTask(task);
                    subscriber.onNext(new ResponseValues(task));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final Task mTask;

        public RequestValues(@NonNull Task task) {
            mTask = checkNotNull(task, "task cannot be null!");
        }

        public Task getTask() {
            return mTask;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final Task mTask;

        public ResponseValues(@NonNull Task task) {
            mTask = checkNotNull(task, "task cannot be null!");
        }

        public Task getTask() {
            return mTask;
        }
    }
}
