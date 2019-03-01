package com.urbit_iot.porton.umodusers.domain.usecase;

import androidx.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.DeleteUserRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
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
                values.uModUserPhoneNum.getUModUUID(),
                666);
         */

        return mUModsRepository.getUMod(values.getuModUUID())
                .flatMap(new Func1<UMod, Observable<DeleteUserRPC.Result>>() {
                    @Override
                    public Observable<DeleteUserRPC.Result> call(UMod uMod) {
                        DeleteUserRPC.Arguments deleteUserArgs = new DeleteUserRPC.Arguments(values.getuModUserPhoneNum());
                        return mUModsRepository.deleteUModUser(uMod, deleteUserArgs);
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends DeleteUserRPC.Result>>() {
                    @Override
                    public Observable<? extends DeleteUserRPC.Result> call(Throwable throwable) {
                        if (throwable instanceof HttpException) {
                            //Check for HTTP UNAUTHORIZED error code
                            int httpErrorCode = ((HttpException) throwable).response().code();
                            Log.e("req_access_uc", "CreateUser Failed: " + httpErrorCode);
                            if (CreateUserRPC.DOC_ERROR_CODES.contains(httpErrorCode)) {
                                if (httpErrorCode == HttpURLConnection.HTTP_UNAUTHORIZED
                                        || httpErrorCode == HttpURLConnection.HTTP_FORBIDDEN) {

                                }
                                if (httpErrorCode == HttpURLConnection.HTTP_NOT_FOUND){

                                }
                            }
                        }
                        return Observable.error(throwable);
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

        private final String uModUserPhoneNum;
        private final String uModUUID;

        public RequestValues(@NonNull String uModUserPhoneNum, String uModUUID) {
            this.uModUserPhoneNum = checkNotNull(uModUserPhoneNum, "uModUserPhoneNum cannot be null!");
            this.uModUUID = uModUUID;
        }

        public String getuModUserPhoneNum() {
            return this.uModUserPhoneNum;
        }

        public String getuModUUID() {
            return uModUUID;
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

    public static class UserNotFoundForDeletionException extends Exception{
        private String userPhoneNum;
        public UserNotFoundForDeletionException(String userPhoneNum){
            //TODO message to globalConstants??
            super("Delete cannot be performed. User " + userPhoneNum + " not found");
            this.userPhoneNum = userPhoneNum;
        }

        public String getUserPhoneNum() {
            return userPhoneNum;
        }

        public void setUserPhoneNum(String userPhoneNum) {
            this.userPhoneNum = userPhoneNum;
        }
    }

    public static class ForbbidenDeletionException{

    }
}
