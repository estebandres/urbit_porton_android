package com.urbit_iot.porton.umodusers.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.data.rpc.AdminCreateUserRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by andresteve07 on 8/21/17.
 */

public class CreateUModUser extends SimpleUseCase<CreateUModUser.RequestValues, CreateUModUser.ResponseValues> {
    private final UModsRepository mUModsRepository;

    @Inject
    public CreateUModUser(@NonNull UModsRepository uModsRepository,
                          @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
    }

    @Override
    public Observable<CreateUModUser.ResponseValues> buildUseCase(final CreateUModUser.RequestValues values) {

        mUModsRepository.cachedFirst();
        return mUModsRepository.getUMod(values.getUModUUID())
                .flatMap(uMod -> {
                    AdminCreateUserRPC.Arguments createUserArgs =
                            new AdminCreateUserRPC.Arguments(values.getUserName());
                    return mUModsRepository.createUModUserByName(uMod,createUserArgs);
                })
                .map(ResponseValues::new);
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final String userName;
        private final String uModUUID;

        public RequestValues(@NonNull String userName, @NonNull String uModUUID) {
            this.userName = checkNotNull(userName, "userName cannot be null!");
            this.uModUUID = checkNotNull(uModUUID, "uModUUID cannot be null!");
        }

        public String getUserName() {
            return this.userName;
        }

        public String getUModUUID(){
            return this.uModUUID;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final AdminCreateUserRPC.Result result;

        public ResponseValues(@NonNull AdminCreateUserRPC.Result result) {
            this.result = checkNotNull(result, "result cannot be null!");
        }

        public AdminCreateUserRPC.Result getResult() {
            return result;
        }
    }
}
