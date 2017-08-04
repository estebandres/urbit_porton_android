package com.urbit_iot.onekey.usersxumod.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.commands.ApproveUserCmd;
import com.urbit_iot.onekey.data.commands.Command;
import com.urbit_iot.onekey.data.commands.DeleteUserCmd;
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

        return mUModsRepository.deleteUModUser(values.getUModUser())
                .map(new Func1<DeleteUserCmd.Response, DeleteUModUser.ResponseValues>() {
                    @Override
                    public DeleteUModUser.ResponseValues call(DeleteUserCmd.Response response) {
                        return new DeleteUModUser.ResponseValues(response);
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

        private final DeleteUserCmd.Response response;

        public ResponseValues(@NonNull DeleteUserCmd.Response response) {
            this.response = checkNotNull(response, "response cannot be null!");
        }

        public DeleteUserCmd.Response getResponse() {
            return response;
        }
    }
}
