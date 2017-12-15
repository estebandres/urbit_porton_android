package com.urbit_iot.onekey.statistics.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.statistics.domain.model.Statistics;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Calculate statistics of active and completed Tasks {@link UMod} in the {@link UModsRepository}.
 */
public class GetStatistics extends SimpleUseCase<GetStatistics.RequestValues, GetStatistics.ResponseValues> {

    private final UModsRepository mTasksRepository;

    @Inject
    public GetStatistics(@NonNull UModsRepository tasksRepository,
                         @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mTasksRepository = tasksRepository;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(RequestValues requestValues) {
        return mTasksRepository.getUMods().map(new Func1<List<UMod>, ResponseValues>() {
            @Override
            public ResponseValues call(List<UMod> tasks) {
                int activeTasks = 0;
                int completedTasks = 0;

                // We calculate number of active and completed tasks
                for (UMod task : tasks) {
                    if (task.isOngoingNotificationEnabled()) {
                        completedTasks += 1;
                    } else {
                        activeTasks += 1;
                    }
                }
                return new ResponseValues(new Statistics(completedTasks, activeTasks));
            }
        });
    }

    public static class RequestValues implements RxUseCase.RequestValues {
    }

    public static class ResponseValues implements RxUseCase.ResponseValues {

        private final Statistics mStatistics;

        public ResponseValues(@NonNull Statistics statistics) {
            mStatistics = checkNotNull(statistics, "statistics cannot be null!");
        }

        public Statistics getStatistics() {
            return mStatistics;
        }
    }
}
