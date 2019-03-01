package com.urbit_iot.porton.umods.domain.usecase;

import androidx.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.umods.UModsFilterType;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Completable;
import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class GetUModsOneByOne extends SimpleUseCase<GetUModsOneByOne.RequestValues, GetUModsOneByOne.ResponseValues> {

    private final UModsRepository mUModsRepository;
    private final AppUserRepository mAppUserRepository;
    private final BaseSchedulerProvider schedulerProvider;

    @Inject
    public GetUModsOneByOne(@NonNull UModsRepository tasksRepository,
                            @NonNull AppUserRepository appUserRepository,
                            @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        this.schedulerProvider = schedulerProvider;
        mUModsRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null!");
        mAppUserRepository = checkNotNull(appUserRepository, "appUserRepository cannot be null!");
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {
        Log.d("STEVE-TEST", "CASO DE USO " + mUModsRepository.hashCode());

        Observable<UMod> oneByOneFromCache = Completable
                .fromAction(mUModsRepository::cachedFirst)
                .andThen(mUModsRepository.getUModsOneByOne());
        Observable<UMod> oneByOneRefreshed = Completable
                .fromAction(mUModsRepository::refreshUMods)
                .andThen(mUModsRepository.getUModsOneByOne());

        Observable<ResponseValues> getUModsUseCaseObservable = mAppUserRepository.getAppUser()
                //.observeOn(schedulerProvider.io())
                .flatMap(appUser -> Observable.just(values.isForceUpdate())
                        .flatMap(isForceUpdate -> {
                            if (isForceUpdate){
                                return Observable.concatDelayError(oneByOneFromCache, oneByOneRefreshed);
                            } else {
                                return oneByOneFromCache;
                            }
                        })
                        //return  uMod.getAppUserLevel() != UModUser.Level.UNAUTHORIZED;// Unauthorized in the DB are ignored
                        .filter(uMod -> {
                            //TODO Replace current isOpen logic or remove it completely
                            if (uMod.getuModSource() == UMod.UModSource.LOCAL_DB
                                    || uMod.getuModSource() == UMod.UModSource.CACHE){
                                return  uMod.getAppUserLevel() != UModUser.Level.UNAUTHORIZED && uMod.getState() != UMod.State.AP_MODE;// Unauthorized in the DB are ignored
                            }
                            return true;
                        })
                        //.observeOn(schedulerProvider.io())
                        .flatMap(uMod -> {
                            if(uMod.getAppUserLevel() == UModUser.Level.PENDING
                                    //TODO Review sources logic. The aim is to lower the sockettimeout in case the umod was disconnected.
                                    && (uMod.getuModSource() == UMod.UModSource.LAN_SCAN
                                    || uMod.getuModSource() == UMod.UModSource.MQTT_SCAN)
                                    && !uMod.isInAPMode()){
                                Log.d("GetUM1x1", "PENDING detected  ON: " + Thread.currentThread().getName());
                                GetUserLevelRPC.Arguments getMyLevelArgs = new GetUserLevelRPC.Arguments(appUser.getUserName());
                                return mUModsRepository.getUserLevel(uMod, getMyLevelArgs)
                                        .flatMap(result -> {
                                            Log.d("getumods1x1_uc", "Get User Status Success: " + result.toString());
                                            uMod.setAppUserLevel(result.getUserLevel());
                                            mUModsRepository.saveUMod(uMod);
                                            return Observable.just(uMod);
                                        })
                                        .onErrorResumeNext(throwable -> {
                                            Log.e("getumods1x1_uc", "Get User Status Fail: " + throwable.getMessage()
                                                    + "ExcType: " + throwable.getClass().getSimpleName(), throwable);
                                            if (throwable instanceof HttpException) {
                                                String errorMessage = "";
                                                //Check for HTTP UNAUTHORIZED error code
                                                try {
                                                    errorMessage = ((HttpException) throwable).response().errorBody().string();
                                                }catch (IOException exc){
                                                    return Observable.error(exc);
                                                }
                                                int httpErrorCode = ((HttpException) throwable).response().code();

                                                Log.e("getumods1x1_uc", "Get User Status (urbit:urbit) Failed on error CODE:"
                                                        + httpErrorCode
                                                        + " MESSAGE: "
                                                        + errorMessage);

                                                //401 and 403 aren't considered because the call is made with urbit:urbit
                                                //As defined in the API and because mongoose limitations all API errors are retrieved with 500 error code
                                                //but inside the error message the actual error code can be found.
                                                if (httpErrorCode == HttpURLConnection.HTTP_INTERNAL_ERROR){
                                                    if (errorMessage.contains(Integer.toString(HttpURLConnection.HTTP_NOT_FOUND))) {
                                                        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
                                                        return Observable.just(uMod);
                                                    }
                                                }
                                            }
                                            return Observable.error(throwable);
                                        })/*
                                        .retry((retryCount, throwable) -> {
                                            Log.e("getumods1x1_uc", "Retry GetUserLevel. Count: " + retryCount + "\n Excep msge: " + throwable.getMessage());
                                            if (retryCount <= 2){
                                                mUModsRepository.refreshUMods();
                                                return true;
                                            } else {
                                                return false;
                                            }
                                        })*/
                                        .onErrorResumeNext(throwable -> {
                                            //If it is an unhandled error then return the umod untouched (pending) so the flow isn't interrupted.
                                            uMod.setAppUserLevel(UModUser.Level.PENDING);
                                            return Observable.just(uMod);
                                        });
                            } else {
                                return Observable.just(uMod);
                            }
                        })
                )
                /*
                .filter(uMod -> {
                    switch (values.getCurrentFiltering()) {
                        case NOTIF_EN_UMODS:
                            return uMod.isOngoingNotificationEnabled();
                        case NOTIF_DIS_UMODS:
                            return !uMod.isOngoingNotificationEnabled();
                        case ALL_UMODS:
                        default:
                            return true;
                    }
                })
                */
                .map(ResponseValues::new);

        return getUModsUseCaseObservable;

        /*
        mUModsRepository.cachedFirst();
        return mUModsRepository.getUModsOneByOne()
                //.switchIfEmpty()
                .doOnNext(mUModMqttService::subscribeToUModTopics)
                .toCompletable()
                .doOnCompleted(() -> {
                    if (values.isForceUpdate()) {
                        mUModsRepository.refreshUMods();
                    } else {
                        mUModsRepository.cachedFirst();
                    }
                })
                .andThen(getUModsUseCaseObservable);
                */
        /*
            if (values.isForceUpdate()) {
                mUModsRepository.refreshUMods();
            } else {
                mUModsRepository.cachedFirst();
            }
            return getUModsUseCaseObservable;
        */
    }


    public static final class RequestValues implements RxUseCase.RequestValues {

        private final UModsFilterType mCurrentFiltering;
        private final boolean mForceUpdate;

        public RequestValues(boolean forceUpdate, @NonNull UModsFilterType currentFiltering) {
            mForceUpdate = forceUpdate;
            mCurrentFiltering = checkNotNull(currentFiltering, "currentFiltering cannot be null!");
        }

        public boolean isForceUpdate() {
            return mForceUpdate;
        }

        public UModsFilterType getCurrentFiltering() {
            return mCurrentFiltering;
        }
    }

    public static final class ResponseValues implements RxUseCase.ResponseValues {

        private final UMod mUMod;

        public ResponseValues(@NonNull UMod uMods) {
            mUMod = checkNotNull(uMods, "umod cannot be null!");
        }

        public UMod getUMod() {
            return mUMod;
        }
    }
}
