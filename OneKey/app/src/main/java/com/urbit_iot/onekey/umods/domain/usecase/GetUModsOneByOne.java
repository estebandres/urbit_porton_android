package com.urbit_iot.onekey.umods.domain.usecase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.appuser.data.source.AppUserRepository;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.GetUserLevelRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.data.source.internet.UModMqttService;
import com.urbit_iot.onekey.umods.UModsFilterType;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class GetUModsOneByOne extends SimpleUseCase<GetUModsOneByOne.RequestValues, GetUModsOneByOne.ResponseValues> {

    private final UModsRepository mUModsRepository;
    private final AppUserRepository mAppUserRepository;
    private final UModMqttService mUModMqttService;

    @Inject
    public GetUModsOneByOne(@NonNull UModsRepository tasksRepository,
                            @NonNull AppUserRepository appUserRepository,
                            @NonNull BaseSchedulerProvider schedulerProvider,
                            @NonNull UModMqttService mUModMqttService) {
        super(Schedulers.io(), schedulerProvider.ui());
        mUModsRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null!");
        mAppUserRepository = checkNotNull(appUserRepository, "appUserRepository cannot be null!");
        this.mUModMqttService = mUModMqttService;
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {

        Observable<ResponseValues> getUModsUseCaseObservable = mAppUserRepository.getAppUser()
                .flatMap(appUser -> mUModsRepository.getUModsOneByOne()
                        .filter(uMod -> {
                            Log.d("GetUM1x1", uMod.toString());
                            //TODO Replace current isOpen logic or remove it completely
                            //isOpen == true means that a module is connected to the LAN and advertising through mDNS and is open to access request...
                            //return (uMod.isOpen() || uMod.isInAPMode()) && (uMod.getuModSource() == UMod.UModSource.LAN_SCAN) ;
                            UMod.UModSource source = uMod.getuModSource();
                            if (source == UMod.UModSource.CACHE || source == UMod.UModSource.LOCAL_DB){
                                return uMod.getState() != UMod.State.AP_MODE;
                            }
                            else {// LAN_SCAN and MQTT_SCAN results.
                                return true;
                            }
                        })
                        .filter(uMod -> {
                            long diffInMillies = Math.abs(new Date().getTime() -  uMod.getLastUpdateDate().getTime());
                            long diffInHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                            return diffInHours < 12L;
                        })
                        .flatMap(uMod -> {
                            //updates topic on each umod
                            //uMod.setMqttResponseTopic(appUser.getUserName());
                            //mUModsRepository.saveUMod(uMod);
                            /*
                            if (uMod.getuModSource() == UMod.UModSource.CACHE){
                                mUModMqttService.subscribeToUModResponseTopic(uMod);
                            }
                            */
                            if(uMod.getAppUserLevel() == UModUser.Level.PENDING && !uMod.isInAPMode()){
                                Log.d("GetUM1x1", "PENDING detected");
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
                                                    + "ExcType: " + throwable.getClass().getSimpleName());
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
                                                if (httpErrorCode == HttpURLConnection.HTTP_INTERNAL_ERROR
                                                        || httpErrorCode == HttpURLConnection.HTTP_NOT_FOUND){
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
                .map(ResponseValues::new);

    mUModsRepository.cachedFirst();
    return mUModsRepository.getUModsOneByOne()
            .doOnNext(mUModMqttService::subscribeToUModResponseTopic)
            .toCompletable()
            .doOnCompleted(() -> {
                if (values.isForceUpdate()) {
                    mUModsRepository.refreshUMods();
                } else {
                    mUModsRepository.cachedFirst();
                }
            })
            .andThen(getUModsUseCaseObservable);
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
