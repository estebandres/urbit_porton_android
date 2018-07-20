package com.urbit_iot.onekey.umodsnotification.domain.usecase;

import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.onekey.RxUseCase;
import com.urbit_iot.onekey.SimpleUseCase;
import com.urbit_iot.onekey.appuser.data.source.AppUserRepository;
import com.urbit_iot.onekey.appuser.domain.AppUser;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.GetUserLevelRPC;
import com.urbit_iot.onekey.data.source.UModsRepository;
import com.urbit_iot.onekey.umods.UModsFilterType;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetches the list of tasks.
 */
public class GetUModsForNotif extends SimpleUseCase<GetUModsForNotif.RequestValues, GetUModsForNotif.ResponseValues> {

    private final UModsRepository mUModsRepository;
    private final AppUserRepository mAppUserRepository;

    @Inject
    public GetUModsForNotif(@NonNull UModsRepository tasksRepository,
                            @NonNull AppUserRepository appUserRepository,
                            @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.io());
        mUModsRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null!");
        mAppUserRepository = checkNotNull(appUserRepository, "appUserRepository cannot be null!");
    }

    @Override
    public Observable<ResponseValues> buildUseCase(final RequestValues values) {

        /*
        if (values.isForceUpdate()) {
            mUModsRepository.refreshUMods();
        } else {
            mUModsRepository.cachedFirst();
        }
         */
        mUModsRepository.cachedFirst();
        //TODO Replace actual isOpen logic or remove it completely
//isOpen == true means that a module is connected to the LAN and advertising through mDNS and is open to access request...
        //TODO first should gather all umods in database whose locations are closer than ... then
        return mUModsRepository.getCurrentLocation()
                .onErrorResumeNext(Observable.error(new PhoneCurrentLocationUnknownException()))
                .switchIfEmpty(Observable.error(new PhoneCurrentLocationUnknownException()))
                .flatMap(location -> {
                    if (location == null){
                        return Observable.error(new PhoneCurrentLocationUnknownException());
                    }
                    if (location.getAccuracy() > 60.0f){
                        return Observable.error(new PhoneCurrentLocationIsInaccurateException());
                    }
                    long diffInMillies = Math.abs(new Date().getTime() - location.getTime());
                    long diffInMinutes = TimeUnit.MINUTES.convert(diffInMillies, TimeUnit.MILLISECONDS);
                    Log.d("GET_UMODS_NOIF", "LOCATION AGE: " + diffInMinutes);
                    if (diffInMinutes > 5L){
                        return Observable.error(new TooOldPhoneLocationException(diffInMinutes));
                    }
                    return Observable.just(location);
                })
                .flatMap(location -> mUModsRepository.getUModsOneByOne()
                .switchIfEmpty(Observable.error(new EmptyUModDataBaseException()))
                .filter(UMod::isOngoingNotificationEnabled)
                .switchIfEmpty(Observable.error(new NoUModsAreNotifEnabledException()))
                .filter(uMod -> {
                    float distanceToUMod;
                    if (uMod.getuModLocation() != null
                            && uMod.getuModLocation().getLatitude() != 0.0
                            && uMod.getuModLocation().getLongitude() != 0.0){
                        distanceToUMod = location.distanceTo(uMod.getuModLocation());
                        Log.d("GET_UMODS_NOIF", "DISTANCE TO "+uMod.getAlias()+" :  " + distanceToUMod);
                        return distanceToUMod < 320.0f;
                    }
                    return false;
                })
                .switchIfEmpty(Observable.error(new AllUModsTooFarAwayException()))
                .filter(uMod -> {
                    //TODO Replace actual isOpen logic or remove it completely
                    //isOpen == true means that a module is connected to the LAN and advertising through mDNS and is open to access request...
                    return !uMod.isInAPMode();
                })
                //Asks to the esp if the admin has authorized me when PENDING.
                .flatMap(uMod -> {
                    if(uMod.getAppUserLevel() == UModUser.Level.PENDING && !uMod.isInAPMode()){
                        return mAppUserRepository.getAppUser()
                                .flatMap(appUser -> {
                                    GetUserLevelRPC.Arguments getMyLevelArgs = new GetUserLevelRPC.Arguments(appUser.getUserName());
                                    return mUModsRepository.getUserLevel(uMod,getMyLevelArgs)//TODO should be called from other UseCase??
                                            .flatMap(result -> {
                                                Log.d("getumods1x1Notif_uc", "Get User Status Success: " + result.toString());
                                                uMod.setAppUserLevel(result.getUserLevel());
                                                mUModsRepository.saveUMod(uMod);
                                                return Observable.just(uMod);
                                            })
                                            .onErrorResumeNext(throwable -> {
                                                Log.e("getumods1x1Notif_uc", "Get User Status Fail: " + throwable.getMessage()
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
                                            })
                                            .onErrorResumeNext(throwable -> {
                                                //If it is an unhandled error then return the umod untouched (pending) so the flow isn't interrupted.
                                                uMod.setAppUserLevel(UModUser.Level.PENDING);
                                                return Observable.just(uMod);
                                            });
                                });

                    } else {
                        return Observable.just(uMod);
                    }
                }))

                .map(ResponseValues::new);
    }

    public static final class RequestValues implements RxUseCase.RequestValues {

        private final boolean mForceUpdate;

        public RequestValues(boolean forceUpdate) {
            mForceUpdate = forceUpdate;
        }

        public boolean isForceUpdate() {
            return mForceUpdate;
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

    public static class NoUModsAreNotifEnabledException extends Exception{
        NoUModsAreNotifEnabledException() {
            super("All UMods are notification disabled.");
        }
    }

    public static class EmptyUModDataBaseException extends Exception{
        EmptyUModDataBaseException() {
            super("There isn't any umod configured yet.");
        }
    }

    public static class AllUModsTooFarAwayException extends Exception{
        AllUModsTooFarAwayException(){
            super("All the modules are at least 300 mts from the phone.");
        }
    }

    public static class PhoneCurrentLocationUnknownException extends Exception{
        PhoneCurrentLocationUnknownException(){
            super("The phone location is unknown.");
        }
    }

    public static class PhoneCurrentLocationIsInaccurateException extends Exception{
        PhoneCurrentLocationIsInaccurateException(){
            super("The phone location is much too inaccurate.");
        }
    }

    public static class TooOldPhoneLocationException extends Exception{
        private long minutes;
        TooOldPhoneLocationException(long minutes){
            super("Phone location is from: " + minutes + " minutes ago.");
            this.minutes = minutes;
        }
        public long getMinutes(){
            return this.minutes;
        }
    }
}
