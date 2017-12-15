package com.urbit_iot.onekey.usersxumod.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.RPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by andresteve07 on 8/14/17.
 */

public class UpDownAdminLevel extends SimpleUseCase<UpDownAdminLevel.RequestValues, UpDownAdminLevel.ResponseValues> {
    private final UModsRepository mUModsRepository;

    @Inject
    public UpDownAdminLevel(@NonNull UModsRepository uModsRepository,
                             @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
    }

    @Override
    public Observable<UpDownAdminLevel.ResponseValues> buildUseCase(final UpDownAdminLevel.RequestValues values) {
        /*
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        }
        */
        UModUser.Level newUserLevel;
        UpDownAdminLevel.ResponseValues defaultResponse = new UpDownAdminLevel.ResponseValues(
                new UpdateUserRPC.Response(new UpdateUserRPC.Result(),
                        "STEVE DEFAULT",
                        new RPC.ResponseError(null,null)));

        if(values.isUModUserToBeAdmin()){
            //If for some reason app tries to ADMIN an ADMIN
            if(values.getUModUser().isAdmin()){
                return Observable.just(defaultResponse);
            }
            newUserLevel = UModUser.Level.ADMINISTRATOR;
        } else {
            //If for some reason app tries to AUTHORIZE an AUTHORIZE
            if(!values.getUModUser().isAdmin()){
                return Observable.just(defaultResponse);
            }
            newUserLevel = UModUser.Level.AUTHORIZED;
        }

        final UpdateUserRPC.Request request = new UpdateUserRPC.Request(
                new UpdateUserRPC.Arguments(values.getUModUser().getPhoneNumber(), newUserLevel),
                values.getUModUser().getuModUUID(),
                666);

        //TODO how many times should I try to execute the RPC.
        return mUModsRepository.getUMod(values.getUModUser().getuModUUID())
                .flatMap(new Func1<UMod, Observable<UpdateUserRPC.Response>>() {
                    @Override
                    public Observable<UpdateUserRPC.Response> call(UMod uMod) {
                        return mUModsRepository.updateUModUser(uMod,request);
                    }
                })
                .map(new Func1<UpdateUserRPC.Response, UpDownAdminLevel.ResponseValues>() {
                    @Override
                    public UpDownAdminLevel.ResponseValues call(UpdateUserRPC.Response response) {
                        return new UpDownAdminLevel.ResponseValues(response);
                    }
                });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final UModUser uModUser;
        private final boolean toAdmin;

        public RequestValues(@NonNull UModUser uModUser, boolean toAdmin) {
            this.uModUser = checkNotNull(uModUser, "uModUser cannot be null!");
            this.toAdmin = checkNotNull(toAdmin, "toAdmin flag cannot be null!");
        }

        public UModUser getUModUser() {
            return this.uModUser;
        }

        public boolean isUModUserToBeAdmin() {
            return this.toAdmin;
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
