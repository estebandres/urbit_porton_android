package com.urbit_iot.porton.data.source.lan;

import android.support.annotation.NonNull;

import android.util.Log;

import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.AdminCreateUserRPC;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.DeleteUserRPC;
import com.urbit_iot.porton.data.rpc.EnableUpdateRPC;
import com.urbit_iot.porton.data.rpc.FactoryResetRPC;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.GetUsersRPC;
import com.urbit_iot.porton.data.rpc.OTACommitRPC;
import com.urbit_iot.porton.data.rpc.SetGateStatusRPC;
import com.urbit_iot.porton.data.rpc.SetWiFiRPC;
import com.urbit_iot.porton.data.rpc.SysGetInfoRPC;
import com.urbit_iot.porton.data.rpc.TriggerRPC;
import com.urbit_iot.porton.data.rpc.UpdateUserRPC;
import com.urbit_iot.porton.data.source.UModsDataSource;
import com.urbit_iot.porton.util.networking.UrlHostSelectionInterceptor;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the data mSource that adds a latency simulating network.
 */
public class UModsLANDataSource implements UModsDataSource {

    @NonNull
    private UModsDNSSDScanner mUModsDNSSDScanner;

    @NonNull
    private UModsBLEScanner mUModsBLEScanner;

    @NonNull
    private UModsWiFiScanner mUModsWiFiScanner;

    @NonNull
    private UModsTCPScanner mUModsTCPScanner;

    @NonNull
    private UModsService defaultUModsService;

    @NonNull
    private UModsService appUserUModsService;

    @NonNull
    private UrlHostSelectionInterceptor urlHostSelectionInterceptor;

    @NonNull
    private Observable.Transformer<UMod,UMod> uModLANBrander;

    /*
    @NonNull
    private Observable.Transformer<UMod,UMod> lanOperationDisabler;
    */


    @Inject
    public UModsLANDataSource(@NonNull UModsDNSSDScanner uModsDNSSDScanner,
                              @NonNull UModsBLEScanner uModsBLEScanner,
                              @NonNull UModsWiFiScanner uModsWiFiScanner,
                              @NonNull UrlHostSelectionInterceptor urlHostSelectionInterceptor,
                              @NonNull @Named("default") UModsService defaultUModsService,
                              @NonNull @Named("app_user") UModsService AppUserUModsService,
                              @NonNull UModsTCPScanner mUModsTCPScanner){ //@NonNull @DigestAuth Credentials digestAuthCredentials) {
        mUModsDNSSDScanner = checkNotNull(uModsDNSSDScanner,"uModsDNSSDScanner should not be null.");
        mUModsBLEScanner = checkNotNull(uModsBLEScanner, " uModsBLEScanner should not be null.");
        this.mUModsWiFiScanner = checkNotNull(uModsWiFiScanner, " uModsWiFiScanner should not be null.");
        this.defaultUModsService = checkNotNull(defaultUModsService, " defaultUModsService should not be null.");
        this.appUserUModsService = checkNotNull(AppUserUModsService, " AppUserUModsService should not be null.");
        this.urlHostSelectionInterceptor = checkNotNull(urlHostSelectionInterceptor, " urlHostSelectionInterceptor should not be null.");
        this.mUModsTCPScanner = mUModsTCPScanner;
        //this.digestAuthCredentials = checkNotNull(digestAuthCredentials, " digestAuthCredentials should not be null.");
        this.uModLANBrander = new Observable.Transformer<UMod, UMod>() {
            @Override
            public Observable<UMod> call(Observable<UMod> uModObservable) {
                return uModObservable
                        .map(uMod -> {
                            uMod.setuModSource(UMod.UModSource.LAN_SCAN);
                            return uMod;
                        });
            }
        };
    }


    @Override
    public Observable<List<UMod>> getUMods() {
        return Observable.mergeDelayError(
                mUModsDNSSDScanner.browseLANForUMods(),
                mUModsBLEScanner.bleScanForUMods())
                .compose(this.uModLANBrander)
                .toList();
    }

    @Override
    public Observable<UMod> getUModsOneByOne(){
        mUModsTCPScanner.setupCalculator();
        return Observable.mergeDelayError(
                mUModsDNSSDScanner.browseLANForUMods(),//Doesn't return null umods
                mUModsWiFiScanner.browseWiFiForUMods())//Doesn't return null umods
                .compose(this.uModLANBrander);
    }

    /**
     * @param uModUUID UUID del modulo a buscar por lan y BT
     * @return un observable de una lista con un el modulo buscado y marcado
     */
    @Override
    public Observable<UMod> getUMod(@NonNull final String uModUUID) {
        return Observable.mergeDelayError(
                mUModsWiFiScanner.browseWiFiForUMod(uModUUID),
                mUModsDNSSDScanner.browseLANForUMod(uModUUID), Observable.empty())
                .doOnNext(uMod -> Log.e("lan_data-source", uMod.toString()))
                .first()
                .compose(this.uModLANBrander);
    }

    @Override
    public void saveUMod(@NonNull UMod uMod) {
    }

    @Override
    public Observable<UMod> updateUModAlias(@NonNull String uModUUID, @NonNull String newAlias) {
        return null;
    }

    @Override
    public void partialUpdate(@NonNull UMod uMod) {

    }

    @Override
    public void setUModNotificationStatus(@NonNull String uModUUID, @NonNull Boolean notificationEnabled) {

    }


    @Override
    public void clearAlienUMods() {

    }

    @Override
    public void refreshUMods() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    @Override
    public void deleteAllUMods() {
    }

    @Override
    public void deleteUMod(@NonNull String uModUUID) {
    }


    @Override
    public Observable<GetUserLevelRPC.Result>
    getUserLevel(@NonNull UMod uMod, @NonNull GetUserLevelRPC.Arguments request) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.defaultUModsService.getAppUserLevel(request);
    }

    @Override
    public Observable<TriggerRPC.Result> triggerUMod(@NonNull UMod uMod, @NonNull TriggerRPC.Arguments request) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        if (uMod.getAppUserLevel() == UModUser.Level.ADMINISTRATOR){
            return this.appUserUModsService.adminTriggerUMod(request);
        } else {
            return this.appUserUModsService.userTriggerUMod(request);
        }
    }

    @Override
    public Observable<CreateUserRPC.Result> createUModUser(@NonNull UMod uMod, @NonNull CreateUserRPC.Arguments request) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.defaultUModsService.createUser(request);
    }

    @Override
    public Observable<UpdateUserRPC.Result> updateUModUser(@NonNull UMod uMod, @NonNull UpdateUserRPC.Arguments request) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.appUserUModsService.postUpdateUser(request);
    }

    @Override
    public Observable<DeleteUserRPC.Result> deleteUModUser(@NonNull UMod uMod, @NonNull DeleteUserRPC.Arguments request) {

        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.appUserUModsService.deleteUser(request);
    }

    @Override
    public Observable<GetUsersRPC.Result> getUModUsers(@NonNull UMod uMod, @NonNull GetUsersRPC.Arguments requestArgs) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.appUserUModsService.getUsers(requestArgs);
    }


    @Override
    public Observable<SysGetInfoRPC.Result> getSystemInfo(@NonNull UMod uMod, @NonNull SysGetInfoRPC.Arguments request) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return defaultUModsService.getSystemInfo(request);
    }

    @Override
    public Observable<SetWiFiRPC.Result> setWiFiAP(UMod uMod, SetWiFiRPC.Arguments request) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return appUserUModsService.postWiFiAPCredentials(request);
    }

    @Override
    public Observable<File> getFirmwareImageFile(UMod uMod) {
        return null;
    }

    @Override
    public Observable<Response<ResponseBody>> postFirmwareUpdateToUMod(UMod uMod, File newFirmwareFile) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        //RequestBody requestFile = RequestBody.create(MediaType.parse("application/zip"),newFirmwareFile);
        //MultipartBody.Part multipartBody = MultipartBody.Part.create(requestFile);
        RequestBody fileRequestBody = RequestBody.create(MediaType.parse("multipart/form"),newFirmwareFile);
        MultipartBody.Part fileMultipartBody =
                MultipartBody.Part.createFormData("file",newFirmwareFile.getName(), fileRequestBody);

        RequestBody timeoutBody =
                RequestBody.create(MediaType.parse("text/plain"), String.valueOf(75L));
        return this.appUserUModsService.startFirmwareUpdate(timeoutBody, fileMultipartBody);
    }

    @Override
    public Observable<Response<ResponseBody>> otaCommit(UMod uMod, OTACommitRPC.Arguments request) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());//TODO could be composed
        return this.defaultUModsService.otaCommit(request);
    }

    @Override
    public Observable<FactoryResetRPC.Result> factoryResetUMod(UMod uMod, FactoryResetRPC.Arguments request) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());//TODO could be composed
        return this.appUserUModsService.postFactoryReset(request);
    }

    @Override
    public Observable<AdminCreateUserRPC.Result>
    createUModUserByName(UMod uMod, AdminCreateUserRPC.Arguments createUserArgs) {
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.appUserUModsService.postAdminCreateUser(createUserArgs);
    }

    public Observable<SetGateStatusRPC.Result>
    setUModGateStatus(UMod uMod, SetGateStatusRPC.Arguments reqArguments){
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.appUserUModsService.postSetUModGateStatus(reqArguments);
    }

    public Observable<EnableUpdateRPC.Result>
    enableUModUpdate(UMod uMod, EnableUpdateRPC.Arguments arguments){
        this.urlHostSelectionInterceptor.setHost(uMod.getConnectionAddress());
        return this.appUserUModsService.postEnableUpdate(arguments);
    }
}