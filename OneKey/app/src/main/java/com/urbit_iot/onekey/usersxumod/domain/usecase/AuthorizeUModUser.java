package com.urbit_iot.onekey.usersxumod.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by andresteve07 on 8/21/17.
 */

public class AuthorizeUModUser extends SimpleUseCase<AuthorizeUModUser.RequestValues, AuthorizeUModUser.ResponseValues> {
    private final UModsRepository mUModsRepository;

    @Inject
    public AuthorizeUModUser(@NonNull UModsRepository uModsRepository,
                             @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
    }

    @Override
    public Observable<AuthorizeUModUser.ResponseValues> buildUseCase(final AuthorizeUModUser.RequestValues values) {
        /*
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }
        */
        final UpdateUserRPC.Request request = new UpdateUserRPC.Request(
                new UpdateUserRPC.Arguments(values.getUModUser().getPhoneNumber(), UModUser.Level.AUTHORIZED),
                values.uModUser.getuModUUID(),
                666);
        //TODO how many times should I try to execute the RPC.
        return mUModsRepository.getUMod(values.getUModUser().getuModUUID())
                .flatMap(new Func1<UMod, Observable<UpdateUserRPC.Response>>() {
                    @Override
                    public Observable<UpdateUserRPC.Response> call(UMod uMod) {
                        return mUModsRepository.updateUModUser(uMod,request);
                    }
                })
                .map(new Func1<UpdateUserRPC.Response, AuthorizeUModUser.ResponseValues>() {
                    @Override
                    public AuthorizeUModUser.ResponseValues call(UpdateUserRPC.Response response) {
                        return new AuthorizeUModUser.ResponseValues(response);
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

        private final UpdateUserRPC.Response response;

        public ResponseValues(@NonNull UpdateUserRPC.Response response) {
            this.response = checkNotNull(response, "response cannot be null!");
        }

        public UpdateUserRPC.Response getResponse() {
            return response;
        }
    }
}
