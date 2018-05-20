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
        final UModUser.Level newUserLevel;
        UpDownAdminLevel.ResponseValues defaultResponse = new UpDownAdminLevel.ResponseValues(
                new UpdateUserRPC.Result("updated???"));

        if(values.isUModUserToBeAdmin()){
            //If for some reason app tries to ADMIN_CROWN an ADMIN_CROWN
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

        /*
        final UpdateUserRPC.Request request = new UpdateUserRPC.Request(
                new UpdateUserRPC.Arguments(values.getUModUser().getPhoneNumber(), newUserLevel),
                values.getUModUser().getuModUUID(),
                666);
         */


        //TODO how many times should I try to execute the RPC.
        return mUModsRepository.getUMod(values.getUModUser().getuModUUID())
                .flatMap(new Func1<UMod, Observable<UpdateUserRPC.Result>>() {
                    @Override
                    public Observable<UpdateUserRPC.Result> call(UMod uMod) {
                        UpdateUserRPC.Arguments updateUserArgs = new UpdateUserRPC.Arguments(values.getUModUser().getPhoneNumber(), newUserLevel.asAPIUserType());
                        return mUModsRepository.updateUModUser(uMod,updateUserArgs);
                    }
                })
                .map(new Func1<UpdateUserRPC.Result, UpDownAdminLevel.ResponseValues>() {
                    @Override
                    public UpDownAdminLevel.ResponseValues call(UpdateUserRPC.Result response) {
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

        private final UpdateUserRPC.Result response;

        public ResponseValues(@NonNull UpdateUserRPC.Result response) {
            this.response = checkNotNull(response, "response cannot be null!");
        }

        public UpdateUserRPC.Result getResponse() {
            return response;
        }
    }
}
