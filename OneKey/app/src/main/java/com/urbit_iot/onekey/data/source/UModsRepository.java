/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.urbit_iot.onekey.data.source;

import android.location.Address;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.support.v4.util.Pair;

import com.google.common.base.Strings;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.AdminCreateUserRPC;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.rpc.GetUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.GetUsersRPC;
import com.urbit_iot.onekey.data.rpc.OTACommitRPC;

import com.urbit_iot.onekey.data.rpc.RPC;
import com.urbit_iot.onekey.data.rpc.SetGateStatusRPC;

import com.urbit_iot.onekey.data.rpc.SetWiFiRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.source.gps.LocationService;
import com.urbit_iot.onekey.data.source.internet.UModMqttService;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.dagger.Internet;
import com.urbit_iot.onekey.util.dagger.Local;
import com.urbit_iot.onekey.util.dagger.LanOnly;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.adapter.rxjava.HttpException;
import retrofit2.Response;
import rx.Completable;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 * <p/>
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 */
@Singleton
public class UModsRepository implements UModsDataSource {

    @NonNull
    final UModsDataSource mUModsLANDataSource;

    @NonNull
    private final UModsDataSource mUModsLocalDataSource;

    @NonNull
    private final UModsDataSource mUModsInternetDataSource;

    @NonNull
    private Observable.Transformer<UMod,UMod> uModCacheBrander;

    @NonNull
    private LocationService locationService;

    @NonNull
    private PhoneConnectivityInfo connectivityInfo;


    @NonNull
    private UModMqttService uModMqttService;

    /**
     * This variable has package local visibility so it can be accessed from tests.
     */
    //TODO explore posibility of implement a self expiring cache as a new DataSource
    @VisibleForTesting
    @NonNull
    Map<String, UMod> mCachedUMods;

    /**
     * Marks the cache as invalid, to force an update the next time data is requested. This variable
     * has package local visibility so it can be accessed from tests.
     */
    @VisibleForTesting
    boolean mCacheIsDirty = false;

    /**
     * By marking the constructor with {@code @Inject}, Dagger will try to inject the dependencies
     * required to create an instance of the UModsRepository. Because {@link UModsDataSource} is an
     * interface, we must provide to Dagger a way to build those arguments, this is done in
     * {@link UModsRepositoryModule}.
     * <P>
     * When two arguments or more have the same type, we must provide to Dagger a way to
     * differentiate them. This is done using a qualifier e.g. @LanOnly, @Local and @Internet.
     * <p>
     * Dagger strictly enforces that arguments not marked with {@code @Nullable} are not injected
     * with {@code @Nullable} values.
     */
    @Inject
    public UModsRepository(@LanOnly UModsDataSource uModsRemoteDataSource,
                           @Local UModsDataSource uModsLocalDataSource,
                           @Internet UModsDataSource uModsInternetDataSource,
                           @NonNull LocationService locationService,
                           @NonNull PhoneConnectivityInfo connectivityInfo,
                           @NonNull UModMqttService uModMqttService) {
        mUModsLANDataSource = checkNotNull(uModsRemoteDataSource);
        mUModsLocalDataSource = checkNotNull(uModsLocalDataSource);
        this.mUModsInternetDataSource = checkNotNull(uModsInternetDataSource);
        this.locationService = locationService;
        this.connectivityInfo = connectivityInfo;
        this.uModMqttService = uModMqttService;
        this.mCachedUMods = new LinkedHashMap<>();
        this.uModCacheBrander = uModObservable -> uModObservable
                .map((Func1<UMod, UMod>) uMod -> {
                    uMod.setuModSource(UMod.UModSource.CACHE);
                    return uMod;
                });
    }

    /**
     * Gets tasks from cache, local data source (SQLite) or remote data source, whichever is
     * available first.
     */
    @Override
    public Observable<List<UMod>> getUMods() {
        // Respond immediately with cache if available and not dirty
        if (!mCacheIsDirty) {
            return Observable.from(mCachedUMods.values()).toList();
        }

        Observable<List<UMod>> freshUMods = getAndSaveRemoteUMods();

        if (mCacheIsDirty) {
            return freshUMods;
        } else {
            // Query the local storage if available. If not, query the network.
            Observable<List<UMod>> localTasks = getAndCacheLocalUMods();
            return Observable.concat(localTasks, freshUMods)
                    .filter(uMods -> !uMods.isEmpty()).first();
        }
    }

    /**
     * Retrieves all cached modules if cache exists or fetches the entries on DB and updates the cache.
     * @return an Observable that emits all umods on cache or DB one by one.
     */
    //@RxLogObservable
    Observable<UMod> getUModsOneByOneFromCacheOrDB(){
        if (!mCachedUMods.isEmpty()){//CACHE MULTIPLE HIT
            Log.d("umods_rep","Map cache");
            return Observable.from(mCachedUMods.values())
                        .compose(this.uModCacheBrander);
        }
        //CACHE MISS
        Log.d("umods_rep","DB");
        return mUModsLocalDataSource.getUModsOneByOne()//never emits null umods.
                .doOnNext(uMod -> mCachedUMods.put(uMod.getUUID(), uMod));
    }

    /**
     * Fetches modules from cache or database, launches a new thread for each module and execute
     * Sys.GetInfo to test connectivity, maps each result to an updated umod and saves it.
     * If an error is produce by the request then the module is ignored.
     * @return an observable that emits online umods
     */
    Observable<UMod> getUModsOneByOneFromCacheOrDiskAndRefreshOnline(){//refresh umod data by
        return this.getUModsOneByOneFromCacheOrDB()
                .flatMap(value -> Observable.just(value)
                        .subscribeOn(Schedulers.io())//TODO use scheduler provider by dagger)
                        .flatMap(uMod -> mUModsInternetDataSource.getSystemInfo(uMod,new SysGetInfoRPC.Arguments())
                                .flatMap(result -> {
                                    if (!Strings.isNullOrEmpty(result.getWifi().getStaIp())){
                                        uMod.setLastUpdateDate(new Date());
                                        uMod.setConnectionAddress(result.getWifi().getStaIp());
                                        uMod.setWifiSSID(result.getWifi().getSsid());
                                        uMod.setState(UMod.State.STATION_MODE);
                                        uMod.setuModSource(UMod.UModSource.MQTT_SCAN);
                                    }
                                    return Observable.just(uMod);
                                })
                                .onErrorResumeNext(throwable -> Observable.empty())
                        )
                )
                //TODO test if necessary
                //.takeUntil(Observable.timer(12L, TimeUnit.SECONDS))
                .doOnNext(uMod -> {
                    Log.d("MQTT_SCAN", "H: " + Thread.currentThread().getName() +" FOUND: " + uMod);
                    mUModsLocalDataSource.saveUMod(uMod);
                    mCachedUMods.put(uMod.getUUID(),uMod);
                });
    }

    /**
     * Merges all modules discovered by Lan(DNS-SD + TCP-SCAN + WIFI-SCAN) and Internet(MQTT Invitations),
     * determines if the module can communicate by LAN HTTP, dismisses accepted MQTT invitations,
     * fetches cached versions of discovered modules and updates relevant attributes.
     * Finally, saves(updates¿?) the resulting modules on DB and updates the cache. Then set the cache flag as clean.
     * WARNING: Security backdoor, when an MQTT invitation is active anyone could download the app
     * and register the same phone number intercepting such invitation and gaining access.
     * @return an observable that emits newly discovered modules.
     */
    Observable<UMod> getUModsOneByOneFromLanAndInternetAndUpdateDBAndCache(){
        return Observable.mergeDelayError(mUModsLANDataSource.getUModsOneByOne(),
                mUModsInternetDataSource.getUModsOneByOne())//Neither of this sources return null umods.
                .flatMap(discoveredUMod -> {
                    UMod cachedUMod = mCachedUMods.get(discoveredUMod.getUUID());
                    //CACHE HIT
                    //If a cached version of the discovered umod exists then...
                    if (cachedUMod != null){
                        switch (discoveredUMod.getuModSource()){
                            case LAN_SCAN:
                                //If LAN HTTP is possible
                                switch (discoveredUMod.getState()){
                                    case STATION_MODE:
                                        cachedUMod.setLanOperationEnabled(true);
                                        cachedUMod.setWifiSSID(connectivityInfo.getWifiAPSSID());
                                        break;
                                    case AP_MODE:
                                        return Observable.just(discoveredUMod);
                                    default:
                                        return Observable.error(new Exception("Invalid state for lan discovered umod."));
                                }
                                break;
                            case MQTT_SCAN:
                                // if cached then belongsToAppUser is always true
                                // This 'retries' cancellation
                                uModMqttService.cancelMyInvitation(discoveredUMod);
                                return Observable.empty();
                            default:
                                return Observable.error(new Exception("Unknown Source for discovered umod."));
                        }

                        //TODO check if all necessary fields are being updated.
                        //** IMPORTANT: AppUserLevel should remain as in DB **
                        cachedUMod.setuModSource(discoveredUMod.getuModSource());
                        //An accepted invitation never reaches this setter
                        cachedUMod.setConnectionAddress(discoveredUMod.getConnectionAddress());
                        cachedUMod.setState(discoveredUMod.getState());
                        cachedUMod.setOpen(discoveredUMod.isOpen());//Always true. TODO Review isOpen logic!
                        cachedUMod.setLastUpdateDate(discoveredUMod.getLastUpdateDate());
                        //CACHE UPDATE
                        mUModsLocalDataSource.saveUMod(cachedUMod);
                        mCachedUMods.put(cachedUMod.getUUID(),cachedUMod);

                        return Observable.just(cachedUMod);
                    } else {//CACHE MISS
                        return Observable.just(discoveredUMod);
                    }
                })
                .doOnTerminate(() -> mCacheIsDirty = false);
    }

    //TODO try to replace Online Refresh by getGateStatus
    @Override
    //@RxLogObservable
    /**
     * Returns an observable with founded umods.
     * mCacheIsDirty determines if search is by cache or by lan scan and internet
     * @return an observable that emits modules.
     */
    public Observable<UMod> getUModsOneByOne() {
        // Defer here is absolutely necessary because the returned observable is conditional
        // and could be retried(.retry())!
        return Observable.defer(() -> {
            Observable<UMod> cacheOrDBUModObs = Observable.defer(this::getUModsOneByOneFromCacheOrDB)
                    .doOnNext(uMod -> Log.d("CACHED_UMODS","FOUND: "+ uMod.toString()));
            //If cache is dirty (forced update) then lookup for UMods on LAN (dnssd,ble)
            Observable<UMod> lanUModObs = Observable.defer(this::getUModsOneByOneFromLanAndInternetAndUpdateDBAndCache);
            Log.d("umods_rep", "get1by1");
            // Respond immediately with cache if available and not dirty
            if (!mCacheIsDirty) {
                Log.d("umods_rep", "cached first" + mCachedUMods);
                return cacheOrDBUModObs
                        .doOnNext(mUModsInternetDataSource::saveUMod);//This subscribes to the umod mqtt topics
            }
            Log.d("umods_rep", "lanbrowse first");
            return Observable.mergeDelayError(
                    getUModsOneByOneFromCacheOrDiskAndRefreshOnline(),
                    lanUModObs);
        });
    }

    private Observable<List<UMod>> getAndCacheLocalUMods() {
        return mUModsLocalDataSource.getUMods()
                .flatMap(uMods -> Observable.from(uMods)
                        .doOnNext(uMod -> mCachedUMods.put(uMod.getUUID(), uMod))
                        .toList());
    }

    private Observable<List<UMod>> getAndSaveRemoteUMods() {
        return mUModsLANDataSource
                .getUMods()
                .flatMap(new Func1<List<UMod>, Observable<List<UMod>>>() {
                    @Override
                    public Observable<List<UMod>> call(List<UMod> uMods) {
                        return Observable.from(uMods).doOnNext(new Action1<UMod>() {
                            @Override
                            public void call(UMod uMod) {
                                mUModsLocalDataSource.saveUMod(uMod);
                                mCachedUMods.put(uMod.getUUID(), uMod);
                            }
                        }).toList();
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        mCacheIsDirty = false;
                    }
                });
    }

    /**
     * Saves an umod instance to the DB and updates the cache.
     * @param uMod module instance to be saved
     */
    @Override
    public void saveUMod(@NonNull UMod uMod) {
        checkNotNull(uMod);
        mUModsLocalDataSource.saveUMod(uMod);
        //mUModsInternetDataSource.saveUMod(uMod);

        mCachedUMods.put(uMod.getUUID(), uMod);
    }

    /**
     * Actualiza el alias del modulo, y lo guarda en la cache.
     * @param uModUUID El UUID del modulo al que le vamos a cambiar el alias
     * @param newAlias  El nuevo alias que le vamos a poner
     * @return Observable con ese modulo
     */
    @Override
    public Observable<UMod> updateUModAlias(@NonNull String uModUUID, @NonNull String newAlias) {
        return mUModsLocalDataSource.updateUModAlias(uModUUID,newAlias)
                .doOnNext(uMod -> mCachedUMods.put(uMod.getUUID(), uMod));
    }



    @Override
    public void partialUpdate(@NonNull UMod uMod) {
        mUModsLocalDataSource.partialUpdate(uMod);
    }

    /**
     * Setea notificacion de estado para modulo con esa UUID
     * @param uModUUID modulo al cual se le debe setear una notificacion de estado
     * @param notificationEnabled
     */
    @Override
    public void setUModNotificationStatus(@NonNull String uModUUID,
                                          @NonNull Boolean notificationEnabled) {
        UMod cachedUMod = mCachedUMods.get(uModUUID);
        if (cachedUMod != null){//&& !cachedUMod.isOldRegister()? would it be useful?
            cachedUMod.setOngoingNotificationStatus(notificationEnabled);
        }
        mUModsLocalDataSource.setUModNotificationStatus(uModUUID, notificationEnabled);
    }



    /**
     *  Borro de la cache los modulos que tienen nivel de no Autorizado
     */
    @Override
    public void clearAlienUMods() {
        mUModsLANDataSource.clearAlienUMods();
        mUModsLocalDataSource.clearAlienUMods();
        Iterator<Map.Entry<String, UMod>> it = mCachedUMods.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, UMod> entry = it.next();
            if (entry.getValue().getAppUserLevel() == UModUser.Level.UNAUTHORIZED) {
                it.remove();
            }
        }
    }

    /**
     * Gets umods from local data source unless the table is new or empty. In that case it
     * uses the network data source.
     * @return observable that emits a single umod for the given UID.
     */
    @Override
    //@RxLogObservable
    public Observable<UMod> getUMod(@NonNull final String uModUUID) {
        checkNotNull(uModUUID);
        // Defer here is absolutely necessary because the returned observable is conditional
        // and could be retried(.retry())!
        return Observable.defer(() -> {
            // Respond immediately with cache
            if (!mCacheIsDirty){
                Log.d("rep_umods", "HOLOc");
                return getSingleUModFromCacheOrDB(uModUUID);
                //.doOnNext(mUModsInternetDataSource::saveUMod);
            } else {
                return getSingleUModFromLanAndUpdateDBEntry(uModUUID);
            }
        });
    }

    /**
     * Filtra de todos los modulos el que pedimos por parametro traidos de Lan or Internet y actualizando cache
     * @param uModUUID El modulo a devolver
     * @return  el modulo que buscamos
     */
    public Observable<UMod> getSingleUModFromLanAndUpdateDBEntry(final String uModUUID){
        return getUModsOneByOneFromLanAndInternetAndUpdateDBAndCache()
                .filter(uMod -> uMod.getUUID().equals(uModUUID))
                .first();
    }

    /**
     * Si la cache no es vacia y contiene el UUID devuelve un observable con ese modulo marcado
     * Si la cache es vacia o no contiene el UUID obtiene el modulo de la BD
     * @param uModUUID
     * @return
     */
    public Observable<UMod> getSingleUModFromCacheOrDB(String uModUUID){
        UMod cachedUMod;
        if (!mCachedUMods.isEmpty() && mCachedUMods.containsKey(uModUUID)){
            cachedUMod = mCachedUMods.get(uModUUID);
            return Observable.just(cachedUMod)
                    .compose(this.uModCacheBrander);
        }
        Log.d("rep_umods", "algo");
        return mUModsLocalDataSource.getUMod(uModUUID)
                .filter(uMod -> uMod != null)
                .doOnNext(uMod -> mCachedUMods.put(uMod.getUUID(), uMod));
    }

    @Override
    public void refreshUMods() {
        mCacheIsDirty = true;
    }

    public void cachedFirst() {
        mCacheIsDirty = false;
    }

    /**
     * Borra todos los modulos de LANDS, BDDS y CacheDS
     */
    @Override
    public void deleteAllUMods() {
        mUModsLANDataSource.deleteAllUMods();
        mUModsLocalDataSource.deleteAllUMods();
        mCachedUMods.clear();
    }

    /**
     * Deletes an UMod entry by UUID
     * @param uModUUID string ID for the UMod entry to be deleted.
     */
    @Override
    public void deleteUMod(@NonNull String uModUUID) {
        mUModsLocalDataSource.deleteUMod(checkNotNull(uModUUID));
        mCachedUMods.remove(uModUUID);
    }

    //--------------------------- RPC EXECUTION --------------------------------------
    //TODO review logic!

    /**
     * Calculates the desire order of datasource choices for rpc execution.
     * If the module is in AP_MODE then lan datasource
     *
     * @param uMod
     * @return
     */
    @NonNull
    Observable<Pair<UModsDataSource,UModsDataSource>> getDataSourceChoices(UMod uMod){
        return Observable.defer(() -> {
            if(uMod.isInAPMode()){
                return Observable.just(new Pair<>(mUModsLANDataSource, null));
            } else {//Assumes a module is either in AP_MODE or STATION_MODE
                mUModsInternetDataSource.saveUMod(uMod);//Checks for subscription
                if (uMod.getAppUserLevel() == UModUser.Level.INVITED){
                    return Observable.just(new Pair<>(mUModsInternetDataSource,null));
                }
                switch (connectivityInfo.getConnectionType()){
                    case WIFI:
                        if (uMod.getuModSource() == UMod.UModSource.LAN_SCAN){//When found by DNSSD then try lan http first
                            return Observable.just(new Pair<>(mUModsLANDataSource, mUModsInternetDataSource));
                            //return Observable.just(new Pair<>(mUModsInternetDataSource,null));
                        }
                        String wifiAPSSID = connectivityInfo.getWifiAPSSID();
                        if (uMod.isLanOperationEnabled()
                                && uMod.getWifiSSID() != null
                                && wifiAPSSID != null
                                && uMod.getWifiSSID().equals(wifiAPSSID)//case sensitive comparision
                                ){//When on the same wifi then go for local http first
                            //TODO TCP connect and close for faster ARP lookup...PLUS checks LAN operability.
                            //Reduces LAN requests reliability and quickness, rethink approach.
                            /*
                            return this.testConnectionToModule(uMod)
                                    .andThen(Observable.just(new Pair<>(mUModsLANDataSource, mUModsInternetDataSource)))
                                    .onErrorResumeNext(throwable -> {
                                       if (throwable instanceof IOException){
                                           uMod.setLanOperationEnabled(false);
                                           saveUMod(uMod);
                                           return Observable.just(new Pair<>(mUModsInternetDataSource,null));
                                       }
                                       return Observable.error(throwable);
                                    });
                            */
                            return Observable.just(new Pair<>(mUModsLANDataSource, mUModsInternetDataSource));
                        } else {//When on wifi but not in the same as umod then go for mqtt
                            return Observable.just(new Pair<>(mUModsInternetDataSource,null));
                        }
                    case MOBILE:
                        return Observable.just(new Pair<>(mUModsInternetDataSource,null));
                    default:
                        return Observable.error(new Exception("Phone is unconnected!!"));
                }
            }
        });
    }

    public abstract class RPCExecutor<T extends RPC.Response, R extends RPC.Request>{
        private UMod targetUMod;
        private R request;
        private T response;
        private UModsDataSource currentDataSource;

        public RPCExecutor(UMod targetUMod, R request) {
            this.targetUMod = targetUMod;
            this.request = request;
        }

        public abstract Observable<T> executeRPC();

        public UMod getTargetUMod() {
            return targetUMod;
        }

        public UModsDataSource getCurrentDataSource() {
            return currentDataSource;
        }

        public void setCurrentDataSource(UModsDataSource currentDataSource) {
            this.currentDataSource = currentDataSource;
        }

        public R getRequest() {
            return request;
        }

        public T getResponse() {
            return response;
        }

        public void setResponse(T response) {
            this.response = response;
        }
    }

    <T extends RPC.Response,R extends RPC.Request> Observable<T>
    executeRPC(@NonNull RPCExecutor<T,R> executor) {
        Observable<T> executionObservable = getDataSourceChoices(executor.getTargetUMod()).flatMap(uModsDataSourcePair -> {
            executor.setCurrentDataSource(uModsDataSourcePair.first);
            return executor.executeRPC()
                    .onErrorResumeNext(throwable -> {
                        if (throwable instanceof HttpException
                                || uModsDataSourcePair.second == null){
                            return Observable.error(throwable);
                        }
                        executor.setCurrentDataSource(uModsDataSourcePair.second);
                        return executor.executeRPC();
                    });
        });
        return executionObservable;
    }


    @Override
    public Observable<GetUserLevelRPC.Result>
    getUserLevel(@NonNull UMod uMod, @NonNull GetUserLevelRPC.Arguments arguments) {
        GetUserLevelRPC.Request getUserLevelRequest =
                new GetUserLevelRPC.Request(arguments,null,null,0);
        RPCExecutor<GetUserLevelRPC.Response, GetUserLevelRPC.Request> getUserLevelExecutor =
                new RPCExecutor<GetUserLevelRPC.Response, GetUserLevelRPC.Request>(uMod, getUserLevelRequest) {
            @Override
            public Observable<GetUserLevelRPC.Response> executeRPC() {
                return this.getCurrentDataSource().getUserLevel(getTargetUMod(), arguments)
                        .map(result -> new GetUserLevelRPC.Response(
                                        result, 0, null, null));
            }
        };
        return this.executeRPC(getUserLevelExecutor)
                .map(GetUserLevelRPC.Response::getResponseResult);
    }

    @Override
    public Observable<TriggerRPC.Result>
    triggerUMod(@NonNull UMod uMod, @NonNull TriggerRPC.Arguments arguments) {
        TriggerRPC.Request triggerRequest =
                new TriggerRPC.Request(null,true,arguments,null,0);
        RPCExecutor<TriggerRPC.Response, TriggerRPC.Request> triggerExecutor =
                new RPCExecutor<TriggerRPC.Response, TriggerRPC.Request>(uMod, triggerRequest) {
                    @Override
                    public Observable<TriggerRPC.Response> executeRPC() {
                        TriggerRPC.Response triggerResponse = new TriggerRPC.Response(null,0,null,null);
                        setResponse(triggerResponse);
                        return this.getCurrentDataSource().triggerUMod(this.getTargetUMod(), arguments)
                                .map(result -> {
                                    triggerResponse.setResponseResult(result);
                                    return triggerResponse;
                                });
                    }
                };
        return this.executeRPC(triggerExecutor)
                .map(TriggerRPC.Response::getResponseResult);
    }

    //TODO Revisar logica
    @Override
    public Observable<CreateUserRPC.Result>
    createUModUser(@NonNull UMod uMod, @NonNull CreateUserRPC.Arguments arguments) {
        /*return getDataSourceChoices(uMod)
                .flatMap(dataSourcePair -> dataSourcePair.first.createUModUser(uMod,request)
                        .onErrorResumeNext(throwable -> {
                            Log.e("REPO","createUModUser",throwable);
                            if (throwable instanceof HttpException
                                    || dataSourcePair.second == null){
                                return Observable.error(throwable);
                            }
                            return dataSourcePair.second.createUModUser(uMod,request);
                        })
                )*/
        CreateUserRPC.Request createUserRequest =
                new CreateUserRPC.Request(arguments,null,null,0);
        RPCExecutor<CreateUserRPC.Response, CreateUserRPC.Request> createUserExecutor =
                new RPCExecutor<CreateUserRPC.Response, CreateUserRPC.Request>(uMod, createUserRequest) {
                    @Override
                    public Observable<CreateUserRPC.Response> executeRPC() {
                        CreateUserRPC.Response createUserResponse = new CreateUserRPC.Response(null,0,null,null);
                        setResponse(createUserResponse);
                        return this.getCurrentDataSource().createUModUser(this.getTargetUMod(), arguments)
                                .map(result -> {
                                    createUserResponse.setResponseResult(result);
                                    return createUserResponse;
                                });
                    }
                };
        return this.executeRPC(createUserExecutor)
                .map(CreateUserRPC.Response::getResponseResult)
                /*
                .doOnNext(result -> {
                    this.uModMqttService.cancelMyInvitation(uMod);
                })
                */
                //If the user was already created i.e. mqtt invited then user creation will fail with 409 error code.
                //The invitation should be cancelled.
                .onErrorResumeNext(throwable -> {
                    if (throwable instanceof HttpException){
                        /*
                        The error body is meant to be accessed only once. For that reason
                        the response exception should be reconstructed.
                         */
                        String errorMessage = "";
                        try {
                            errorMessage = ((HttpException) throwable).response().errorBody().string();
                        }catch (IOException exc){
                            exc.printStackTrace();
                        }
                        int httpErrorCode = ((HttpException) throwable).response().code();
                        if (httpErrorCode == 500 && errorMessage.contains("409")){
                            this.uModMqttService.cancelMyInvitation(uMod);
                        }
                        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"),errorMessage);
                        Response response = Response.error(500,responseBody);
                        return Observable.error(new HttpException(response));
                    }
                    return Observable.error(throwable);
                });
    }


    @Override
    public Observable<UpdateUserRPC.Result>
    updateUModUser(@NonNull UMod uMod, @NonNull UpdateUserRPC.Arguments arguments) {
        UpdateUserRPC.Request updateUserRequest =
                new UpdateUserRPC.Request(arguments,null,null,0);
        RPCExecutor<UpdateUserRPC.Response, UpdateUserRPC.Request> updateUserExecutor =
                new RPCExecutor<UpdateUserRPC.Response, UpdateUserRPC.Request>(uMod, updateUserRequest) {
                    @Override
                    public Observable<UpdateUserRPC.Response> executeRPC() {
                        return this.getCurrentDataSource().updateUModUser(this.getTargetUMod(),arguments)
                                .map(result -> new UpdateUserRPC.Response(
                                        result, 0, null, null));
                    }
                };
        return this.executeRPC(updateUserExecutor)
                .map(UpdateUserRPC.Response::getResponseResult);

    }

    @Override
    public Observable<DeleteUserRPC.Result>
    deleteUModUser(@NonNull UMod uMod, @NonNull DeleteUserRPC.Arguments arguments) {

        DeleteUserRPC.Request deleteUserRequest =
                new DeleteUserRPC.Request(arguments,null,null,0);
        RPCExecutor<DeleteUserRPC.Response, DeleteUserRPC.Request> deleteUserExecutor =
                new RPCExecutor<DeleteUserRPC.Response, DeleteUserRPC.Request>(uMod, deleteUserRequest) {
                    @Override
                    public Observable<DeleteUserRPC.Response> executeRPC() {
                        return this.getCurrentDataSource().deleteUModUser(this.getTargetUMod(),arguments)
                                .map(result -> new DeleteUserRPC.Response(
                                        result, 0, null, null));
                    }
                };
        return this.executeRPC(deleteUserExecutor)
                .map(DeleteUserRPC.Response::getResponseResult);
    }

    @Override
    public Observable<GetUsersRPC.Result>
    getUModUsers(@NonNull UMod uMod, @NonNull GetUsersRPC.Arguments arguments) {
        GetUsersRPC.Request getUserRequest =
                new GetUsersRPC.Request(arguments,null,null,0);
        RPCExecutor<GetUsersRPC.Response, GetUsersRPC.Request> getUserExecutor =
                new RPCExecutor<GetUsersRPC.Response, GetUsersRPC.Request>(uMod, getUserRequest) {
                    @Override
                    public Observable<GetUsersRPC.Response> executeRPC() {
                        return this.getCurrentDataSource().getUModUsers(this.getTargetUMod(),arguments)
                                .map(result -> new GetUsersRPC.Response(
                                        result, 0, null, null));
                    }
                };
        return this.executeRPC(getUserExecutor)
                .map(GetUsersRPC.Response::getResponseResult);
    }

    @Override
    public Observable<SysGetInfoRPC.Result>
    getSystemInfo(@NonNull UMod uMod, @NonNull SysGetInfoRPC.Arguments arguments) {
        SysGetInfoRPC.Request getSystemInfoRequest =
                new SysGetInfoRPC.Request(arguments,null,null,0);
        RPCExecutor<SysGetInfoRPC.Response, SysGetInfoRPC.Request> getSystemInfoExecutor =
                new RPCExecutor<SysGetInfoRPC.Response, SysGetInfoRPC.Request>(uMod, getSystemInfoRequest) {
                    @Override
                    public Observable<SysGetInfoRPC.Response> executeRPC() {
                        return this.getCurrentDataSource().getSystemInfo(this.getTargetUMod(),arguments)
                                .map(result -> new SysGetInfoRPC.Response(
                                        result, 0, null, null));
                    }
                };
        return this.executeRPC(getSystemInfoExecutor)
                .map(SysGetInfoRPC.Response::getResponseResult);
    }

    @Override
    public Observable<SetWiFiRPC.Result> setWiFiAP(UMod uMod, SetWiFiRPC.Arguments arguments) {
        SetWiFiRPC.Request setWiFiRequest =
                new SetWiFiRPC.Request(arguments,null,0);
        RPCExecutor<SetWiFiRPC.Response, SetWiFiRPC.Request> setWiFiExecutor =
                new RPCExecutor<SetWiFiRPC.Response, SetWiFiRPC.Request>(uMod, setWiFiRequest) {
                    @Override
                    public Observable<SetWiFiRPC.Response> executeRPC() {
                        return this.getCurrentDataSource().setWiFiAP(this.getTargetUMod(),arguments)
                                .map(result -> new SetWiFiRPC.Response(
                                        result, 0, null, null));
                    }
                };
        return this.executeRPC(setWiFiExecutor)
                .map(SetWiFiRPC.Response::getResponseResult);
    }

    @Override
    public Observable<File> getFirmwareImageFile(UMod uMod) {
        return mUModsInternetDataSource.getFirmwareImageFile(uMod);
    }

    @Override
    public Observable<Response<ResponseBody>> postFirmwareUpdateToUMod(UMod uMod, File newFirmwareFile) {
        return mUModsLANDataSource.postFirmwareUpdateToUMod(uMod, newFirmwareFile);

    }

    @Override
    public Observable<Response<ResponseBody>> otaCommit(UMod uMod, OTACommitRPC.Arguments arguments) {
        return this.mUModsLANDataSource.otaCommit(uMod, arguments);
    }

    @Override
    public Observable<FactoryResetRPC.Result> factoryResetUMod(UMod uMod, FactoryResetRPC.Arguments arguments) {
        FactoryResetRPC.Request factoryResetRequest =
                new FactoryResetRPC.Request(arguments,null,null,0);
        RPCExecutor<FactoryResetRPC.Response, FactoryResetRPC.Request> factoryResetExecutor =
                new RPCExecutor<FactoryResetRPC.Response, FactoryResetRPC.Request>(uMod, factoryResetRequest) {
                    @Override
                    public Observable<FactoryResetRPC.Response> executeRPC() {
                        return this.getCurrentDataSource().factoryResetUMod(this.getTargetUMod(),arguments)
                                .map(result -> new FactoryResetRPC.Response(
                                        result, 0, null, null));
                    }
                };
        return this.executeRPC(factoryResetExecutor)
                .map(FactoryResetRPC.Response::getResponseResult);
    }

    @Override
    public Observable<Location> getCurrentLocation() {
        return this.locationService.getCurrentLocation();
    }

    @Override
    public Observable<Address> getAddressFromLocation(Location location) {
        return this.locationService.getAddressFromLocation(location);
    }

    @Override
    public Observable<AdminCreateUserRPC.Result> createUModUserByName(UMod uMod, AdminCreateUserRPC.Arguments arguments) {
        AdminCreateUserRPC.Request adminCreateUserRequest =
                new AdminCreateUserRPC.Request(arguments,null,null,0);
        RPCExecutor<AdminCreateUserRPC.Response, AdminCreateUserRPC.Request> adminCreateUSerExecutor =
                new RPCExecutor<AdminCreateUserRPC.Response, AdminCreateUserRPC.Request>(uMod, adminCreateUserRequest) {
                    @Override
                    public Observable<AdminCreateUserRPC.Response> executeRPC() {
                        return this.getCurrentDataSource().createUModUserByName(this.getTargetUMod(),arguments)
                                .map(result -> new AdminCreateUserRPC.Response(
                                        result, 0, null, null));
                    }
                };
        return this.executeRPC(adminCreateUSerExecutor)
                .map(AdminCreateUserRPC.Response::getResponseResult);
    }


    public Completable testConnectionToModule(UMod umod) {
        return Completable.fromCallable(() -> {
            Socket clientSocket;
            clientSocket = new Socket();
            try {
                Log.d("DNSSD_SCAN", "TESTING CONN ON: " + Thread.currentThread().getName());
                clientSocket.connect(
                        new InetSocketAddress(
                                umod.getConnectionAddress(),
                                GlobalConstants.UMOD__TCP_ECHO_PORT),
                        1500);
            } finally {
                clientSocket.close();
            }
            return true;
        });
    }

    public Observable<SetGateStatusRPC.Result>
    setUModGateStatus(UMod uMod, SetGateStatusRPC.Arguments reqArguments){
        return mUModsLANDataSource.setUModGateStatus(uMod, reqArguments);
    }
}