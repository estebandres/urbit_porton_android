package com.urbit_iot.porton.umodusers.domain.usecase;

import androidx.annotation.NonNull;

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.appuser.domain.AppUser;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.rpc.GetUsersRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

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
    private final AppUserRepository mAppUserRepository;

    @Inject
    public GetUModUsers(@NonNull UModsRepository uModsRepository,
                        @NonNull BaseSchedulerProvider schedulerProvider,
                        @NonNull AppUserRepository mAppUserRepository) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
        this.mAppUserRepository = checkNotNull(mAppUserRepository,"mAppUserRepository cannot be null!");
    }

    @Override
    public Observable<GetUModUsers.ResponseValues> buildUseCase(final GetUModUsers.RequestValues values) {


        return mAppUserRepository.getAppUser()
                .flatMap(new Func1<AppUser, Observable<GetUsersRPC.UserResult>>() {
                    @Override
                    public Observable<GetUsersRPC.UserResult> call(final AppUser appUser) {
                        return mUModsRepository.getUMod(values.getUModUUID())
                                .flatMap(new Func1<UMod, Observable<GetUsersRPC.Result>>() {
                                    @Override
                                    public Observable<GetUsersRPC.Result> call(UMod uMod) {
                                        return mUModsRepository.getUModUsers(uMod, new GetUsersRPC.Arguments());
                                    }
                                })
                                .flatMap(new Func1<GetUsersRPC.Result, Observable<GetUsersRPC.UserResult>>() {
                                    @Override
                                    public Observable<GetUsersRPC.UserResult> call(GetUsersRPC.Result result) {
                                        return Observable.from(result.getUsers());
                                    }
                                })
                                .filter(new Func1<GetUsersRPC.UserResult, Boolean>() {
                                    @Override
                                    public Boolean call(final GetUsersRPC.UserResult userResult) {
                                        return !userResult.getUserName().contentEquals("urbit")
                                                && !appUser.getUserName().contentEquals(userResult.getUserName());//TODO add umodUserName as AppUser member
                                    }
                                });
                    }
                })
                .toList()
                .flatMap(new Func1<List<GetUsersRPC.UserResult>, Observable<GetUsersRPC.Result>>() {
                    @Override
                    public Observable<GetUsersRPC.Result> call(List<GetUsersRPC.UserResult> userResultList) {
                        return Observable.just(new GetUsersRPC.Result(userResultList));
                    }
                })
                .map(new Func1<GetUsersRPC.Result, GetUModUsers.ResponseValues>() {
                    @Override
                    public GetUModUsers.ResponseValues call(GetUsersRPC.Result result) {
                        return new GetUModUsers.ResponseValues(result);
                    }
                });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        //private final UModUsersFilterType mCurrentFiltering;
        //private final boolean mForceUpdate;
        private final String uModUUID;

        public RequestValues(@NonNull String uModUUID) {//, boolean forceUpdate, @NonNull UModUsersFilterType currentFiltering
            //mForceUpdate = forceUpdate;
            //mCurrentFiltering = checkNotNull(currentFiltering, "currentFiltering cannot be null!");
            this.uModUUID = checkNotNull(uModUUID, "uModUUID cannot be null!");
        }

        /*
        public boolean isForceUpdate() {
            return mForceUpdate;
        }

        public UModUsersFilterType getCurrentFiltering() {
            return mCurrentFiltering;
        }
        */
        public String getUModUUID(){
            return this.uModUUID;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final GetUsersRPC.Result getUsersResponse;

        public ResponseValues(@NonNull GetUsersRPC.Result response) {
            this.getUsersResponse = checkNotNull(response, "uModUsers cannot be null!");
        }

        public GetUsersRPC.Result getResult() {
            return this.getUsersResponse;
        }
    }
}

/*
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
 */