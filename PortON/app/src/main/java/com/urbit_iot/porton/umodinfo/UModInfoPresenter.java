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

package com.urbit_iot.porton.umodinfo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.umodconfig.domain.usecase.DeleteUMod;
import com.urbit_iot.porton.umodconfig.domain.usecase.GetUModAndUpdateInfo;
import com.urbit_iot.porton.data.UMod;
import com.google.common.base.Strings;
import com.urbit_iot.porton.umods.domain.usecase.SetOngoingNotificationStatus;

import javax.inject.Inject;

import rx.Subscriber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link UModInfoFragment}), retrieves the data and updates
 * the UI as required.
 */
public class UModInfoPresenter implements UModInfoContract.Presenter {

    private final UModInfoContract.View mTaskDetailView;
    private final GetUModAndUpdateInfo mGetUModAndUpdateInfo;
    private final SetOngoingNotificationStatus mSetOngoingNotificationStatus;
    private final DeleteUMod mDeleteUMod;

    @Nullable
    private String mTaskId;

    /**
     * Dagger strictly enforces that arguments not marked with {@code @Nullable} are not injected
     * with {@code @Nullable} values.
     */
    @Inject
    public UModInfoPresenter(@Nullable String taskId,
                             @NonNull UModInfoContract.View taskDetailView,
                             @NonNull GetUModAndUpdateInfo getUModAndUpdateInfo,
                             @NonNull SetOngoingNotificationStatus setOngoingNotificationStatus,
                             @NonNull DeleteUMod deleteUMod) {
        mTaskId = taskId;
        mTaskDetailView = checkNotNull(taskDetailView, "taskDetailView cannot be null!");
        mGetUModAndUpdateInfo = checkNotNull(getUModAndUpdateInfo, "getUModUUID cannot be null!");
        mSetOngoingNotificationStatus = checkNotNull(setOngoingNotificationStatus, "disableUModNotification cannot be null!");
        mDeleteUMod = checkNotNull(deleteUMod, "deleteUMod cannot be null!");
        mTaskDetailView.setPresenter(this);
    }

    /**
     * Method injection is used here to safely reference {@code this} after the object is created.
     * For more information, see Java Concurrency in Practice.
     */
    @Inject
    void setupListeners() {
        mTaskDetailView.setPresenter(this);
    }

    @Override
    public void subscribe() {
        openTask();
    }

    @Override
    public void unsubscribe() {
        mGetUModAndUpdateInfo.unsubscribe();
        mSetOngoingNotificationStatus.unsubscribe();
        mDeleteUMod.unsubscribe();
    }

    private void openTask() {
        if (Strings.isNullOrEmpty(mTaskId)) {
            mTaskDetailView.showMissingTask();
            return;
        }

        mTaskDetailView.setLoadingIndicator(true);

        //TODO remove mock
        String wifissid = "mockwifi";
        mGetUModAndUpdateInfo.execute(new GetUModAndUpdateInfo.RequestValues(mTaskId, wifissid),
                new Subscriber<GetUModAndUpdateInfo.ResponseValues>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                // The view may not be able to handle UI updates anymore
                if (!mTaskDetailView.isActive()) {
                    return;
                }
                mTaskDetailView.showMissingTask();
            }

            @Override
            public void onNext(GetUModAndUpdateInfo.ResponseValues response) {
                UMod task = response.getUMod();

                // The view may not be able to handle UI updates anymore
                if (!mTaskDetailView.isActive()) {
                    return;
                }
                mTaskDetailView.setLoadingIndicator(false);
                showTask(task);
            }
        });
    }

    @Override
    public void editTask() {
        if (Strings.isNullOrEmpty(mTaskId)) {
            mTaskDetailView.showMissingTask();
            return;
        }
        mTaskDetailView.showEditTask(mTaskId);
    }

    @Override
    public void deleteTask() {
        mDeleteUMod.execute(new DeleteUMod.RequestValues(mTaskId),
                new Subscriber<RxUseCase.NoResponseValues>() {
            @Override
            public void onCompleted() {
                mTaskDetailView.showTaskDeleted();
            }

            @Override
            public void onError(Throwable e) {
                // Show error, log, etc.
            }

            @Override
            public void onNext(RxUseCase.NoResponseValues noResponseValues) {

            }
        });
    }

    @Override
    public void completeTask() {
        if (Strings.isNullOrEmpty(mTaskId)) {
            mTaskDetailView.showMissingTask();
            return;
        }
        mSetOngoingNotificationStatus.execute(new SetOngoingNotificationStatus.RequestValues(mTaskId, true),
                new Subscriber<RxUseCase.NoResponseValues>() {
            @Override
            public void onCompleted() {
                mTaskDetailView.showTaskMarkedComplete();
            }

            @Override
            public void onError(Throwable e) {
                // Show error, log, etc.
            }

            @Override
            public void onNext(RxUseCase.NoResponseValues noResponseValues) {

            }
        });
    }

    @Override
    public void activateTask() {
        if (Strings.isNullOrEmpty(mTaskId)) {
            mTaskDetailView.showMissingTask();
            return;
        }
        mSetOngoingNotificationStatus.execute(new SetOngoingNotificationStatus.RequestValues(mTaskId,false),
                new Subscriber<RxUseCase.NoResponseValues>() {
            @Override
            public void onCompleted() {
                mTaskDetailView.showTaskMarkedActive();
            }

            @Override
            public void onError(Throwable e) {
                // Show error, log, etc.
            }

            @Override
            public void onNext(RxUseCase.NoResponseValues noResponseValues) {

            }
        });
    }

    private void showTask(@NonNull UMod uMod) {
        String title = uMod.getAlias();
        String description = uMod.getConnectionAddress();

        if (Strings.isNullOrEmpty(title)) {
            mTaskDetailView.hideTitle();
        } else {
            mTaskDetailView.showTitle(title);
        }

        if (Strings.isNullOrEmpty(description)) {
            mTaskDetailView.hideDescription();
        } else {
            mTaskDetailView.showDescription(description);
        }
        mTaskDetailView.showCompletionStatus(uMod.isOngoingNotificationEnabled());
    }
}
