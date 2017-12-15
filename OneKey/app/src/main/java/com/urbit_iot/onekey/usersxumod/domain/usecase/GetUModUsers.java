package com.urbit_iot.onekey.usersxumod.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.usersxumod.UModUsersFilterType;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by andresteve07 on 8/14/17.
 */

public class GetUModUsers extends SimpleUseCase<GetUModUsers.RequestValues, GetUModUsers.ResponseValues> {
    private final UModsRepository mUModsRepository;

    @Inject
    public GetUModUsers(@NonNull UModsRepository tasksRepository,
                    @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null!");
    }

    @Override
    public Observable<GetUModUsers.ResponseValues> buildUseCase(final GetUModUsers.RequestValues values) {
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }

        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(new Func1<UMod, Observable<List<UModUser>>>() {
                    @Override
                    public Observable<List<UModUser>> call(UMod uMod) {
                        return mUModsRepository.getUModUsers(uMod);
                    }
                })
                .flatMap(new Func1<List<UModUser>, Observable<UModUser>>() {
                    @Override
                    public Observable<UModUser> call(List<UModUser> uModUsers) {
                        return Observable.from(uModUsers);
                    }
                })
                .filter(new Func1<UModUser, Boolean>() {
                    @Override
                    public Boolean call(UModUser uModUser) {
                        switch (values.getCurrentFiltering()) {
                            case ADMINS:
                                return uModUser.isAdmin();
                            case NOT_ADMINS:
                                return !uModUser.isAdmin();
                            case ALL_UMOD_USERS:
                            default:
                                return true;
                        }
                    }
                })
                .toList()
                .map(new Func1<List<UModUser>, GetUModUsers.ResponseValues>() {
                    @Override
                    public GetUModUsers.ResponseValues call(List<UModUser> uModUsers) {
                        return new GetUModUsers.ResponseValues(uModUsers);
                    }
                });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final UModUsersFilterType mCurrentFiltering;
        private final boolean mForceUpdate;
        private final String uModUUID;

        public RequestValues(@NonNull String uModUUID, boolean forceUpdate, @NonNull UModUsersFilterType currentFiltering) {
            mForceUpdate = forceUpdate;
            mCurrentFiltering = checkNotNull(currentFiltering, "currentFiltering cannot be null!");
            this.uModUUID = checkNotNull(uModUUID, "uModUUID cannot be null!");
        }

        public boolean isForceUpdate() {
            return mForceUpdate;
        }

        public UModUsersFilterType getCurrentFiltering() {
            return mCurrentFiltering;
        }

        public String getUModUUID(){
            return this.uModUUID;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final List<UModUser> mUModUsers;

        public ResponseValues(@NonNull List<UModUser> uModUsers) {
            this.mUModUsers = checkNotNull(uModUsers, "uModUsers cannot be null!");
        }

        public List<UModUser> getUModUsers() {
            return this.mUModUsers;
        }
    }
}
