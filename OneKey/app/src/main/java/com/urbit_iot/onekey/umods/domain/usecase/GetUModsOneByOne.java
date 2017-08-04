package com.urbit_iot.onekey.umods.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.umods.UModsFilterType;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class GetUModsOneByOne extends SimpleUseCase<GetUModsOneByOne.RequestValues, GetUModsOneByOne.ResponseValues> {

    private final UModsRepository mUModsRepository;

    @Inject
    public GetUModsOneByOne(@NonNull UModsRepository tasksRepository,
                            @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null!");
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }

        return mUModsRepository.getUModsOneByOne()
                .filter(new Func1<UMod, Boolean>() {
                    @Override
                    public Boolean call(UMod uMod) {
                        switch (values.getCurrentFiltering()) {
                            case NOTIF_EN_UMODS:
                                return uMod.isNotificationEnabled();
                            case NOTIF_DIS_UMODS:
                                return !uMod.isNotificationEnabled();
                            case ALL_UMODS:
                            default:
                                return true;
                        }
                    }
                })
                .map(new Func1<UMod, ResponseValues>() {
                    @Override
                    public ResponseValues call(UMod uMod) {
                        return new ResponseValues(uMod);
                    }
                });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final UModsFilterType mCurrentFiltering;
        private final boolean mForceUpdate;

        public RequestValues(boolean forceUpdate, @NonNull UModsFilterType currentFiltering) {
            mForceUpdate = forceUpdate;
            mCurrentFiltering = checkNotNull(currentFiltering, "currentFiltering cannot be null!");
        }

        public boolean isForceUpdate() {
            return mForceUpdate;
        }

        public UModsFilterType getCurrentFiltering() {
            return mCurrentFiltering;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final UMod mUMod;

        public ResponseValues(@NonNull UMod uMods) {
            mUMod = checkNotNull(uMods, "umod cannot be null!");
        }

        public UMod getUMod() {
            return mUMod;
        }
    }
}
