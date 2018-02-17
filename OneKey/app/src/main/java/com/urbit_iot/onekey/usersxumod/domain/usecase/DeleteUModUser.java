package com.urbit_iot.onekey.usersxumod.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by andresteve07 on 8/21/17.
 */

public class DeleteUModUser extends SimpleUseCase<DeleteUModUser.RequestValues, DeleteUModUser.ResponseValues> {
    private final UModsRepository mUModsRepository;

    @Inject
    public DeleteUModUser(@NonNull UModsRepository uModsRepository,
                           @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
    }

    @Override
    public Observable<DeleteUModUser.ResponseValues> buildUseCase(final DeleteUModUser.RequestValues values) {
        /*
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }
        */

        /*
        final DeleteUserRPC.Request request = new DeleteUserRPC.Request(
                new DeleteUserRPC.Arguments(values.getUModUser().getPhoneNumber()),
                values.uModUser.getuModUUID(),
                666);
         */

        return mUModsRepository.getUMod(values.getUModUser().getuModUUID())
                .flatMap(new Func1<UMod, Observable<DeleteUserRPC.Result>>() {
                    @Override
                    public Observable<DeleteUserRPC.Result> call(UMod uMod) {
                        DeleteUserRPC.Arguments deleteUserArgs = new DeleteUserRPC.Arguments(values.getUModUser().getPhoneNumber());
                        return mUModsRepository.deleteUModUser(uMod, deleteUserArgs);
                    }
                })
                .map(new Func1<DeleteUserRPC.Result, DeleteUModUser.ResponseValues>() {
                    @Override
                    public DeleteUModUser.ResponseValues call(DeleteUserRPC.Result result) {
                        return new DeleteUModUser.ResponseValues(result);
                    }
                });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final UModUser uModUser;

        public RequestValues(@NonNull UModUser uModUser) {
            this.uModUser = checkNotNull(uModUser, "uModUser cannot be null!");
        }

        public UModUser getUModUser() {
            return this.uModUser;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final DeleteUserRPC.Result result;

        public ResponseValues(@NonNull DeleteUserRPC.Result result) {
            this.result = checkNotNull(result, "result cannot be null!");
        }

        public DeleteUserRPC.Result getResult() {
            return result;
        }
    }
}
