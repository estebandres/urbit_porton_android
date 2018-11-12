package com.urbit_iot.porton.umodusers.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.rpc.APIUserType;
import com.urbit_iot.porton.data.rpc.UpdateUserRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by andresteve07 on 8/21/17.
 */

public class UpdateUserType extends SimpleUseCase<UpdateUserType.RequestValues, UpdateUserType.ResponseValues> {
    private final UModsRepository mUModsRepository;

    @Inject
    public UpdateUserType(@NonNull UModsRepository uModsRepository,
                          @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
    }

    @Override
    public Observable<UpdateUserType.ResponseValues> buildUseCase(final UpdateUserType.RequestValues values) {

        //TODO how many times should I try to execute the RPC.
        return mUModsRepository.getUMod(values.getuModUUID())
                .flatMap(new Func1<UMod, Observable<UpdateUserRPC.Result>>() {
                    @Override
                    public Observable<UpdateUserRPC.Result> call(UMod uMod) {

                        UpdateUserRPC.Arguments updateUserArgs =
                                new UpdateUserRPC.Arguments(
                                        values.getuModUserPhone(),
                                        values.getApiUserType());
                        return mUModsRepository.updateUModUser(uMod,updateUserArgs);
                    }
                })
                .map(new Func1<UpdateUserRPC.Result, UpdateUserType.ResponseValues>() {
                    @Override
                    public UpdateUserType.ResponseValues call(UpdateUserRPC.Result response) {
                        return new UpdateUserType.ResponseValues(response);
                    }
                });
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final String uModUserPhone;
        private final String uModUUID;
        private final APIUserType apiUserType;

        public RequestValues(@NonNull String uModUserPhone, @NonNull String uModUUID, @NonNull APIUserType apiUserType) {
            this.uModUserPhone = checkNotNull(uModUserPhone, "uModUserPhone cannot be null!");
            this.uModUUID = checkNotNull(uModUUID, "uModUUID cannot be null!");
            this.apiUserType = checkNotNull(apiUserType,"apiUserType cannot be null!") ;
        }

        public String getuModUserPhone() {
            return this.uModUserPhone;
        }

        public String getuModUUID(){
            return this.uModUUID;
        }

        public APIUserType getApiUserType() {
            return apiUserType;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final UpdateUserRPC.Result result;

        public ResponseValues(@NonNull UpdateUserRPC.Result result) {
            this.result = checkNotNull(result, "result cannot be null!");
        }

        public UpdateUserRPC.Result getResult() {
            return result;
        }
    }
}
