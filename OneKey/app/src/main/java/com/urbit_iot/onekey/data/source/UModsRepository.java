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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.util.dagger.Local;
import com.urbit_iot.onekey.util.dagger.Remote;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

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
    private final UModsDataSource mUModsLANDataSource;

    @NonNull
    private final UModsDataSource mUModsLocalDataSource;

    @NonNull
    private Observable.Transformer<UMod,UMod> uModCacheBrander;

    /**
     * This variable has package local visibility so it can be accessed from tests.
     */
    //TODO explore posibility of implement a self expiring cache as a new DataSource
    @VisibleForTesting
    @Nullable
    Map<String, UMod> mCachedUMods;

    /**
     * Marks the cache as invalid, to force an update the next time data is requested. This variable
     * has package local visibility so it can be accessed from tests.
     */
    @VisibleForTesting
    boolean mCacheIsDirty = false;

    /**
     * By marking the constructor with {@code @Inject}, Dagger will try to inject the dependencies
     * required to create an instance of the TasksRepository. Because {@link UModsDataSource} is an
     * interface, we must provide to Dagger a way to build those arguments, this is done in
     * {@link UModsRepositoryModule}.
     * <P>
     * When two arguments or more have the same type, we must provide to Dagger a way to
     * differentiate them. This is done using a qualifier.
     * <p>
     * Dagger strictly enforces that arguments not marked with {@code @Nullable} are not injected
     * with {@code @Nullable} values.
     */
    @Inject
    public UModsRepository(@Remote UModsDataSource tasksRemoteDataSource,
                           @Local UModsDataSource tasksLocalDataSource) {
        mUModsLANDataSource = checkNotNull(tasksRemoteDataSource);
        mUModsLocalDataSource = checkNotNull(tasksLocalDataSource);
        this.uModCacheBrander = new Observable.Transformer<UMod, UMod>() {
            @Override
            public Observable<UMod> call(Observable<UMod> uModObservable) {
                return uModObservable
                        .map(new Func1<UMod, UMod>() {
                            @Override
                            public UMod call(UMod uMod) {
                                uMod.setuModSource(UMod.UModSource.CACHE);
                                return uMod;
                            }
                        });
            }
        };
    }

    /**
     * Gets tasks from cache, local data source (SQLite) or remote data source, whichever is
     * available first.
     */
    @Override
    public Observable<List<UMod>> getUMods() {
        // Respond immediately with cache if available and not dirty
        if (mCachedUMods != null && !mCacheIsDirty) {
            return Observable.from(mCachedUMods.values()).toList();
        } else if (mCachedUMods == null) {
            mCachedUMods = new LinkedHashMap<>();
        }

        Observable<List<UMod>> remoteTasks = getAndSaveRemoteUMods();

        if (mCacheIsDirty) {
            return remoteTasks;
        } else {
            // Query the local storage if available. If not, query the network.
            Observable<List<UMod>> localTasks = getAndCacheLocalUMods();
            return Observable.concat(localTasks, remoteTasks)
                    .filter(new Func1<List<UMod>, Boolean>() {
                        @Override
                        public Boolean call(List<UMod> uMods) {
                            return !uMods.isEmpty();
                        }
                    }).first();
        }
    }

    private Observable<UMod> getUModsOneByOneFromCacheOrDisk(){
        if (mCachedUMods!=null && !mCachedUMods.isEmpty()){
            Log.d("umods_rep","Map cache");
                return Observable.from(mCachedUMods.values()).compose(this.uModCacheBrander);
        }
        Log.d("umods_rep","DB");
        return mUModsLocalDataSource.getUModsOneByOne()
                .doOnNext(new Action1<UMod>() {
                    @Override
                    public void call(UMod uMod) {
                        mCachedUMods.put(uMod.getUUID(), uMod);
                    }
                });
    }

    private Observable<UMod> getUModsOneByOneFromLanAndUpdateDBAndCache(){
        return mUModsLANDataSource
                .getUModsOneByOne()
                .flatMap(new Func1<UMod, Observable<UMod>>() {
                    @Override
                    public Observable<UMod> call(final UMod lanUMod) {
                        return mUModsLocalDataSource.getUMod(lanUMod.getUUID())
                                .flatMap(new Func1<UMod, Observable<UMod>>() {
                                    @Override
                                    public Observable<UMod> call(UMod dbUMod) {
                                        if(dbUMod == null){
                                            return Observable.just(lanUMod);
                                        } else {//Updates the entry
                                            dbUMod.setConnectionAddress(lanUMod.getConnectionAddress());
                                            dbUMod.setState(lanUMod.getState());
                                            dbUMod.setuModSource(UMod.UModSource.LAN_SCAN);
                                            return Observable.just(dbUMod);
                                        }
                                    }
                                });
                    }
                })
                .doOnNext(new Action1<UMod>() {
                    @Override
                    public void call(UMod uMod) {
                        if(uMod.belongsToAppUser()){
                            //TODO should make only an update over the DB entry: connectionAddress and updateDate only (partial update).
                            Log.d("umods_rep", "db update");
                            mUModsLocalDataSource.saveUMod(uMod);
                        }
                        //TODO move to cacheDataSource when
                        UMod cachedUMod = mCachedUMods.get(uMod.getUUID());
                        if(cachedUMod!=null){
                            cachedUMod.setConnectionAddress(uMod.getConnectionAddress());
                            cachedUMod.setState(uMod.getState());
                            cachedUMod.setLastUpdateDate(uMod.getLastUpdateDate());
                            mCachedUMods.put(cachedUMod.getUUID(),cachedUMod);
                        } else {
                            mCachedUMods.put(uMod.getUUID(),uMod);
                        }
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e("repo_get_lan", throwable.getMessage());
                        mCacheIsDirty = true;//To force the update later.
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        mCacheIsDirty = false;
                    }
                });
    }

    //TODO make a revision of this method!
    @Override
    //@RxLogObservable
    public Observable<UMod> getUModsOneByOne() {
        Observable<UMod> cacheOrDBUModObs = Observable.empty();
        //If cache is dirty (forced update) then lookup for UMods on LAN (dnssd,ble)
        Observable<UMod> lanUModObs = this.getUModsOneByOneFromLanAndUpdateDBAndCache();
        // Respond immediately with cache if available and not dirty
        Log.d("umods_rep", "get1by1");
        if (!mCacheIsDirty) {
            Log.d("umods_rep", "cache" + mCachedUMods);
            cacheOrDBUModObs = getUModsOneByOneFromCacheOrDisk();
            return Observable.concatDelayError(
                    cacheOrDBUModObs.defaultIfEmpty(null),
                    lanUModObs)
                    .filter(new Func1<UMod, Boolean>() {
                        @Override
                        public Boolean call(UMod uMod) {
                            return uMod != null;
                        }
                    });
        }

        if (mCachedUMods == null){
            mCachedUMods = new LinkedHashMap<>();
        }

        Log.d("umods_rep", "lanbrowse");

        return Observable.concatDelayError(cacheOrDBUModObs, lanUModObs)
                .filter(new Func1<UMod, Boolean>() {
                    @Override
                    public Boolean call(UMod uMod) {
                        return uMod != null;
                    }
                });
    }

    private Observable<List<UMod>> getAndCacheLocalUMods() {
        return mUModsLocalDataSource.getUMods()
                .flatMap(new Func1<List<UMod>, Observable<List<UMod>>>() {
                    @Override
                    public Observable<List<UMod>> call(List<UMod> uMods) {
                        return Observable.from(uMods)
                                .doOnNext(new Action1<UMod>() {
                                    @Override
                                    public void call(UMod uMod) {
                                        mCachedUMods.put(uMod.getUUID(), uMod);
                                    }
                                })
                                .toList();
                    }
                });
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

    @Override
    public void saveUMod(@NonNull UMod uMod) {
        checkNotNull(uMod);
        mUModsLANDataSource.saveUMod(uMod);
        mUModsLocalDataSource.saveUMod(uMod);

        // Do in memory cache update to keep the app UI up to date
        if (mCachedUMods == null) {
            mCachedUMods = new LinkedHashMap<>();
        }
        mCachedUMods.put(uMod.getUUID(), uMod);
    }

    @Override
    public void partialUpdate(@NonNull UMod uMod) {
        mUModsLocalDataSource.partialUpdate(uMod);
    }

    @Override
    public void setUModNotificationStatus(@NonNull String uModUUID,
                                          @NonNull Boolean notificationEnabled) {
        UMod cachedUMod = getCachedUMod(uModUUID);
        if (cachedUMod != null){//&& !cachedUMod.isOldRegister()? would it be useful?
            cachedUMod.setOngoingNotificationStatus(notificationEnabled);
        }

        mUModsLocalDataSource.setUModNotificationStatus(uModUUID, notificationEnabled);
    }

    public UMod getCachedUMod(String uModUUID){
        if (mCachedUMods!=null && !mCachedUMods.isEmpty()
                && mCachedUMods.containsKey(uModUUID)) {
            return mCachedUMods.get(uModUUID);
        } else {
            return null;
        }
    }

    @Override
    public void clearAlienUMods() {
        mUModsLANDataSource.clearAlienUMods();
        mUModsLocalDataSource.clearAlienUMods();

        // Do in memory cache update to keep the app UI up to date
        if (mCachedUMods == null) {
            mCachedUMods = new LinkedHashMap<>();
        }
        Iterator<Map.Entry<String, UMod>> it = mCachedUMods.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, UMod> entry = it.next();
            if (entry.getValue().getAppUserLevel() == UModUser.Level.UNAUTHORIZED) {
                it.remove();
            }
        }
    }

    /**
     * Gets tasks from local data source (sqlite) unless the table is new or empty. In that case it
     * uses the network data source. This is done to simplify the sample.
     */
    @Override
    public Observable<UMod> getUMod(@NonNull final String uModUUID) {
        checkNotNull(uModUUID);

        // Respond immediately with cache if available
        if (!mCacheIsDirty){
            return Observable.concatDelayError(getSingleUModFromCacheOrDisk(uModUUID),
                    getSingleUModFromLanAndUpdateDBEntry(uModUUID))
                    .doOnNext(new Action1<UMod>() {
                        @Override
                        public void call(UMod uMod) {
                            Log.e("umod_repo", uMod.toString());
                        }
                    })
                    .doOnError(new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.e("umod_repo", throwable.getMessage());
                        }
                    })
                    .first();
        } else {
            return getSingleUModFromLanAndUpdateDBEntry(uModUUID);
        }


    }
    private Observable<UMod> getSingleUModFromLanAndUpdateDBEntry(final String uModUUID){
        // If lanUmodsDataSource produces a result then tries to update the DB entry for the same UUID
        return mUModsLANDataSource.getUMod(uModUUID)
                .filter(new Func1<UMod, Boolean>() {
                    @Override
                    public Boolean call(UMod uMod) {
                        return uMod != null;
                    }
                })
                .last()
                .flatMap(new Func1<UMod, Observable<UMod>>() {
                    @Override
                    public Observable<UMod> call(final UMod lanUMod) {
                        return mUModsLocalDataSource.getUMod(uModUUID)
                                .flatMap(new Func1<UMod, Observable<UMod>>() {
                                    @Override
                                    public Observable<UMod> call(UMod dbUMod) {
                                        // Given the implementation of the DB DataSource
                                        // it explicitly returns a result or a default null.
                                        // Since lanDS produces incomplete UMods objects it should
                                        // fetch the DB entry and update it.
                                        if(dbUMod == null){
                                            return Observable.just(lanUMod);
                                        } else {//Updates the entry
                                            dbUMod.setConnectionAddress(lanUMod.getConnectionAddress());
                                            dbUMod.setState(lanUMod.getState());
                                            return Observable.just(dbUMod);
                                        }
                                    }
                                })
                                .onErrorResumeNext(Observable.just(lanUMod));
                    }
                })
                .doOnNext(new Action1<UMod>() {
                    @Override
                    public void call(UMod uMod) {//Updates Cache and DB
                        if (uMod.belongsToAppUser()){
                            mCachedUMods.put(uMod.getUUID(),uMod);
                            mUModsLocalDataSource.saveUMod(uMod);
                        }
                    }
                });
    }

    private Observable<UMod> getSingleUModFromCacheOrDisk(String uModUUID){
        UMod cachedUMod;
        if (mCachedUMods!=null && !mCachedUMods.isEmpty()
                && mCachedUMods.containsKey(uModUUID)){
            cachedUMod = mCachedUMods.get(uModUUID);
            if (cachedUMod != null){//&& !cachedUMod.isOldRegister()? would it be useful?
                return Observable.just(cachedUMod);
            }
        }
        return mUModsLocalDataSource.getUMod(uModUUID)
                .filter(new Func1<UMod, Boolean>() {
                    @Override
                    public Boolean call(UMod uMod) {
                        return uMod != null;
                    }
                })
                .doOnNext(new Action1<UMod>() {
                    @Override
                    public void call(UMod uMod) {
                        mCachedUMods.put(uMod.getUUID(), uMod);
                    }
                });
    }

    @Override
    public void refreshUMods() {
        mCacheIsDirty = true;
    }

    @Override
    public void deleteAllUMods() {
        mUModsLANDataSource.deleteAllUMods();
        mUModsLocalDataSource.deleteAllUMods();

        if (mCachedUMods == null) {
            mCachedUMods = new LinkedHashMap<>();
        }
        mCachedUMods.clear();
    }

    @Override
    public void deleteUMod(@NonNull String uModUUID) {
        mUModsLANDataSource.deleteUMod(checkNotNull(uModUUID));
        mUModsLocalDataSource.deleteUMod(checkNotNull(uModUUID));

        mCachedUMods.remove(uModUUID);
    }

    @Override
    public Observable<GetMyUserLevelRPC.Response>
    getUserLevel(@NonNull UMod uMod, @NonNull GetMyUserLevelRPC.Request request) {
        return mUModsLANDataSource.getUserLevel(uMod, request);
    }

    @Override
    public Observable<TriggerRPC.Response>
    triggerUMod(@NonNull UMod uMod, @NonNull TriggerRPC.Request request) {
        return mUModsLANDataSource.triggerUMod(uMod,request);
    }

    @Override
    public Observable<CreateUserRPC.Response> createUModUser(@NonNull UMod uMod, @NonNull CreateUserRPC.Request request) {
        return mUModsLANDataSource.createUModUser(uMod, request);
    }

    @Override
    public Observable<UpdateUserRPC.Response>
    updateUModUser(@NonNull UMod uMod, @NonNull UpdateUserRPC.Request request) {
        return mUModsLANDataSource.updateUModUser(uMod,request);
    }

    @Override
    public Observable<DeleteUserRPC.Response>
    deleteUModUser(@NonNull UMod uMod, @NonNull DeleteUserRPC.Request request) {
        return null;
    }

    @Override
    public Observable<List<UModUser>> getUModUsers(@NonNull UMod uMod) {
        return mUModsLANDataSource.getUModUsers(uMod);
    }

    @Override
    public Observable<SysGetInfoRPC.Response>
    getSystemInfo(@NonNull UMod uMod, @NonNull SysGetInfoRPC.Request request) {
        return mUModsLANDataSource.getSystemInfo(uMod, request);
    }

    @Nullable
    private UMod getUModWithUUID(@NonNull String id) {
        checkNotNull(id);
        if (mCachedUMods == null || mCachedUMods.isEmpty()) {
            return null;
        } else {
            return mCachedUMods.get(id);
        }
    }

    @NonNull
    Observable<UMod> getTaskWithIdFromLocalRepository(@NonNull final String uModUUID) {
        return mUModsLocalDataSource
                .getUMod(uModUUID)
                .doOnNext(new Action1<UMod>() {
                    @Override
                    public void call(UMod uMod) {
                        mCachedUMods.put(uModUUID, uMod);
                    }
                })
                .first();
    }
}