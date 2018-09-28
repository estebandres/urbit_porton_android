package com.urbit_iot.onekey.data.source;

import android.location.Location;
import android.support.v4.util.Pair;
import android.util.Log;

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
import com.urbit_iot.onekey.data.rpc.SetWiFiRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;
import com.urbit_iot.onekey.data.source.gps.LocationService;
import com.urbit_iot.onekey.data.source.internet.UModMqttService;
import com.urbit_iot.onekey.data.source.lan.UModsBLEScanner;
import com.urbit_iot.onekey.data.source.lan.UModsDNSSDScanner;
import com.urbit_iot.onekey.data.source.lan.UModsLANDataSource;
import com.urbit_iot.onekey.data.source.lan.UModsService;
import com.urbit_iot.onekey.data.source.lan.UModsTCPScanner;
import com.urbit_iot.onekey.data.source.lan.UModsWiFiScanner;
import com.urbit_iot.onekey.util.networking.UrlHostSelectionInterceptor;

import net.bytebuddy.asm.Advice;
import net.eusashead.iot.mqtt.paho.OnError;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;


import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by andresteve07 on 08/10/18.
 */
//@RunWith(MockitoJUnitRunner.class)
public class UModsRepositoryTest {

    @Mock
    private UModsDataSource lanDataSourceMock;

    @Mock
    private UModsDataSource internetDataSourceMock;

    @Mock
    private UModsDataSource dataBaseDataSourceMock;

    @Mock
    private PhoneConnectivityInfo connectivityInfoMock;

    @Mock
    private UModMqttService mqttServiceMock;

    @Mock
    private LocationService locationServiceMock;

    @Rule public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    private UModsRepository uModsRepository;
    
    @BeforeClass
    static public void setupFakeUMods(){

    }

    @Before
    public void setUp() throws Exception {
        uModsRepository = new UModsRepository(
                lanDataSourceMock,
                dataBaseDataSourceMock,
                internetDataSourceMock,
                locationServiceMock,
                connectivityInfoMock,
                mqttServiceMock);

    }

    @After
    public void tearDown() throws Exception {

        reset(lanDataSourceMock,
                internetDataSourceMock,
                dataBaseDataSourceMock,
                connectivityInfoMock,
                mqttServiceMock,
                locationServiceMock);

        uModsRepository = null;
    }

    @Test
    public void Given_ExistingCachedModules_When_FetchedOneByOneFromCacheOrDB_Then_CachedModulesAreReturned(){
        //Given
        UMod testingUMod = new UMod("123456");
        uModsRepository.mCachedUMods.put("123456",testingUMod);
        //When
        Observable<UMod> testObservable = uModsRepository.getUModsOneByOneFromCacheOrDB();
        //Then
        testObservable.test()
                .assertValueCount(1)
                .assertValue(testingUMod)
                .assertCompleted();
        assertEquals(1,uModsRepository.mCachedUMods.size());
        assertEquals(uModsRepository.mCachedUMods.get("123456"),testingUMod);
    }

    @Test
    public void Given_EmptyCache_When_FetchedOneByOneFromCacheOrDB_Then_DBModulesAreReturnedAndCacheIsUpdated(){
        //Given
        uModsRepository.mCachedUMods.clear();
        when(dataBaseDataSourceMock.getUModsOneByOne()).thenReturn(Observable.just(new UMod("888")));
        //When
        Observable<UMod> testObservable = uModsRepository.getUModsOneByOneFromCacheOrDB();
        //Then
        testObservable.test()
                .assertValueCount(1)
                .assertCompleted()
                //If an observable terminates on completion then it is impossible to incur on error
                // the next assertion wouldn't be necessary but is left to illustrate the comment.
                .assertNoErrors()
                //Any Termination event unsubscribes the observable chain so
                // the next assertion would not be necessary but is left to illustrate the comment.
                .assertUnsubscribed()
                .assertValue(new UMod("888"));
        assertEquals(1,uModsRepository.mCachedUMods.size());
        assertEquals(uModsRepository.mCachedUMods.get("888"),new UMod("888"));
    }

    @Test
    public void Given_EmptyCacheAndEmptyDB_When_FetchedOneByOneFromCacheOrDB_Then_NoModulesAreReturned(){
        //Given
        uModsRepository.mCachedUMods.clear();
        when(dataBaseDataSourceMock.getUModsOneByOne()).thenReturn(Observable.empty());
        //When
        Observable<UMod> testObservable = uModsRepository.getUModsOneByOneFromCacheOrDB();
        //Then
        testObservable.test()
                .assertCompleted()
                .assertNoValues();
        assertEquals(0, uModsRepository.mCachedUMods.size());
    }

    @Test
    public void Given_CachedVersionExistsForStationModeLanDiscovery_When_FetchedFromLanAndInternet_Then_UModsAreReturnedAndDBIsUpdated(){
        //Given
        UMod cachedVersionMock = new UMod("777");
        cachedVersionMock.setAppUserLevel(UModUser.Level.AUTHORIZED);
        uModsRepository.mCachedUMods.put("777", cachedVersionMock);
        UMod discoveredUMod = new UMod("777","192.168.5.9",true);
        //Set LAN discovery
        discoveredUMod.setuModSource(UMod.UModSource.LAN_SCAN);
        //Set discovery state STATION_MODE
        discoveredUMod.setState(UMod.State.STATION_MODE);
        when(lanDataSourceMock.getUModsOneByOne()).thenReturn(Observable.just(discoveredUMod));
        when(internetDataSourceMock.getUModsOneByOne()).thenReturn(Observable.empty());//Zero discoveries by MQTT scan
        when(connectivityInfoMock.getWifiAPSSID()).thenReturn("SteveWiFi");
        //When
        Observable<UMod> testObservable = uModsRepository.getUModsOneByOneFromLanAndInternetAndUpdateDBAndCache();
        Flowable<UMod> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        testFlowable.test()
                .assertValue(uMod -> uMod.isLanOperationEnabled()
                        && uMod.getWifiSSID().equals("SteveWiFi"))
                .assertComplete()
                .assertValueCount(1);
        verify(lanDataSourceMock,times(1)).getUModsOneByOne();
        verify(internetDataSourceMock,times(1)).getUModsOneByOne();
        verify(dataBaseDataSourceMock, times(1)).saveUMod(any(UMod.class));
        assertFalse(uModsRepository.mCacheIsDirty);
    }

    @Test
    public void Given_ThereIsNoCachedVersionForLanDiscovery_When_FetchedFromLanAndInternet_Then_DiscoveryIsReturnedUnmodified(){
        //Given
        uModsRepository.mCachedUMods.clear();
        UMod discoveredUMod = new UMod("777","192.168.5.9",true);
        discoveredUMod.setuModSource(UMod.UModSource.LAN_SCAN);//Set LAN discovery
        discoveredUMod.setState(UMod.State.STATION_MODE);//Set discovery state STATION_MODE
        when(lanDataSourceMock.getUModsOneByOne()).thenReturn(Observable.just(discoveredUMod));
        when(internetDataSourceMock.getUModsOneByOne()).thenReturn(Observable.empty());//Zero discoveries by MQTT scan
        //When
        Observable<UMod> testObservable = uModsRepository.getUModsOneByOneFromLanAndInternetAndUpdateDBAndCache();
        Flowable<UMod> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        testFlowable.test()
                .assertValue(uMod -> uMod.getuModSource() == UMod.UModSource.LAN_SCAN
                        && uMod.getState() == UMod.State.STATION_MODE)
                .assertComplete()
                .assertValueCount(1);
        assertTrue(uModsRepository.mCachedUMods.size() == 0);
        assertFalse(uModsRepository.mCacheIsDirty);
        verify(lanDataSourceMock,times(1)).getUModsOneByOne();
        verify(internetDataSourceMock,times(1)).getUModsOneByOne();
        verify(dataBaseDataSourceMock,never()).saveUMod(any(UMod.class));
    }

    @Test
    public void Given_CachedVersionExistsForAPModeLanDiscovery_When_FetchedFromLanAndInternet_Then_DiscoveryIsReturned(){
        //Given
        UMod cachedVersionMock = new UMod("777");
        cachedVersionMock.setAppUserLevel(UModUser.Level.AUTHORIZED);
        uModsRepository.mCachedUMods.put("777", cachedVersionMock);
        UMod discoveredUMod = new UMod("777");
        discoveredUMod.setuModSource(UMod.UModSource.LAN_SCAN);//Set LAN discovery
        discoveredUMod.setState(UMod.State.AP_MODE);//Set discovery state STATION_MODE
        when(lanDataSourceMock.getUModsOneByOne()).thenReturn(Observable.just(discoveredUMod));
        when(internetDataSourceMock.getUModsOneByOne()).thenReturn(Observable.empty());//Zero discoveries by MQTT scan
        //When
        Observable<UMod> testObservable = uModsRepository.getUModsOneByOneFromLanAndInternetAndUpdateDBAndCache();
        Flowable<UMod> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        testFlowable.test()
                .assertValue(uMod -> uMod.getuModSource() == UMod.UModSource.LAN_SCAN
                        && uMod.getState() == UMod.State.AP_MODE)
                .assertComplete()
                .assertValueCount(1);
        verify(lanDataSourceMock,times(1)).getUModsOneByOne();
        verify(internetDataSourceMock,times(1)).getUModsOneByOne();
        verify(dataBaseDataSourceMock, never()).saveUMod(any(UMod.class));
        assertFalse(uModsRepository.mCacheIsDirty);
    }

    @Test
    public void Given_CachedVersionExistsForInvalidStateLanDiscovery_When_FetchedFromLanAndInternet_Then_ErrorIsPropagated(){
        //Given
        UMod cachedVersionMock = new UMod("777");
        cachedVersionMock.setAppUserLevel(UModUser.Level.AUTHORIZED);
        uModsRepository.mCachedUMods.put("777", cachedVersionMock);
        UMod discoveredUMod = new UMod("777");
        discoveredUMod.setuModSource(UMod.UModSource.LAN_SCAN);//Set LAN discovery
        discoveredUMod.setState(UMod.State.UNKNOWN);//Set discovery state STATION_MODE
        when(lanDataSourceMock.getUModsOneByOne()).thenReturn(Observable.just(discoveredUMod));
        when(internetDataSourceMock.getUModsOneByOne()).thenReturn(Observable.empty());//Zero discoveries by MQTT scan
        //When
        Observable<UMod> testObservable = uModsRepository.getUModsOneByOneFromLanAndInternetAndUpdateDBAndCache();
        Flowable<UMod> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        testFlowable.test()
                .assertError(Exception.class).assertError(throwable -> throwable.getMessage().contains("Invalid"));
        verify(lanDataSourceMock,times(1)).getUModsOneByOne();
        verify(internetDataSourceMock,times(1)).getUModsOneByOne();
        verify(dataBaseDataSourceMock, never()).saveUMod(any(UMod.class));
        assertFalse(uModsRepository.mCacheIsDirty);
    }

    @Test
    public void Given_CachedVersionExistsForUnknownSourceDiscovery_When_FetchedFromLanAndInternet_Then_ErrorIsPropagated(){
        //Given
        UMod cachedVersionMock = new UMod("777");
        cachedVersionMock.setAppUserLevel(UModUser.Level.AUTHORIZED);
        uModsRepository.mCachedUMods.put("777", cachedVersionMock);
        UMod discoveredUMod = new UMod("777");
        discoveredUMod.setuModSource(UMod.UModSource.UNKNOWN);//Set LAN discovery
        discoveredUMod.setState(UMod.State.UNKNOWN);//Set discovery state STATION_MODE
        when(lanDataSourceMock.getUModsOneByOne()).thenReturn(Observable.just(discoveredUMod));
        when(internetDataSourceMock.getUModsOneByOne()).thenReturn(Observable.empty());//Zero discoveries by MQTT scan
        //When
        Observable<UMod> testObservable = uModsRepository.getUModsOneByOneFromLanAndInternetAndUpdateDBAndCache();
        Flowable<UMod> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        testFlowable.test()
                .assertError(Exception.class).assertError(throwable -> throwable.getMessage().contains("Unknown"));
        verify(lanDataSourceMock,times(1)).getUModsOneByOne();
        verify(internetDataSourceMock,times(1)).getUModsOneByOne();
        verify(dataBaseDataSourceMock, never()).saveUMod(any(UMod.class));
        assertFalse(uModsRepository.mCacheIsDirty);
    }

    @Test
    public void Given_CachedVersionExistsForMqttDiscovery_When_FetchedFromLanAndInternet_Then_DiscoveryIsIgnored(){
        //Given
        UMod cachedVersionMock = new UMod("777");
        cachedVersionMock.setAppUserLevel(UModUser.Level.AUTHORIZED);
        uModsRepository.mCachedUMods.put("777", cachedVersionMock);

        UMod discoveredUMod = new UMod("777");
        discoveredUMod.setAppUserLevel(UModUser.Level.INVITED);
        discoveredUMod.setuModSource(UMod.UModSource.MQTT_SCAN);
        discoveredUMod.setState(UMod.State.STATION_MODE);
        discoveredUMod.setConnectionAddress(null);

        when(lanDataSourceMock.getUModsOneByOne()).thenReturn(Observable.empty());//Zero discoveries by lan scan
        when(internetDataSourceMock.getUModsOneByOne()).thenReturn(Observable.just(discoveredUMod));
        //When
        Observable<UMod> testObservable = uModsRepository.getUModsOneByOneFromLanAndInternetAndUpdateDBAndCache();
        Flowable<UMod> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        testFlowable.test()
                .assertNoValues()
                .assertComplete();
        verify(mqttServiceMock,times(1)).cancelMyInvitation(discoveredUMod);
        verify(lanDataSourceMock,times(1)).getUModsOneByOne();
        verify(internetDataSourceMock,times(1)).getUModsOneByOne();
        verify(dataBaseDataSourceMock, never()).saveUMod(any(UMod.class));
        assertFalse(uModsRepository.mCacheIsDirty);
    }

    //TODO test other cases
    @Test
    public void Given_ModuleResponseHasStaIp_When_RefreshingUModsOnline_Then_UModIsUpdatedAndSavedToDB() throws InterruptedException {
        UMod cachedUMod = new UMod("999");
        Date sevenSecondsAgo = new Date(System.currentTimeMillis()-7000);
        cachedUMod.setLastUpdateDate(sevenSecondsAgo);
        uModsRepository.mCachedUMods.put(cachedUMod.getUUID(), cachedUMod);
        SysGetInfoRPC.Result rpcResult = new SysGetInfoRPC.Result(
                null, null, null, null, null,
                0, 0, 0, 0, 0,0,
                new SysGetInfoRPC.Result.Wifi(
                        "192.168.8.8",
                        "",
                        "got ip",
                        "SteveWiFi"),
                null);
        when(internetDataSourceMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.just(rpcResult).delay(500L, TimeUnit.MILLISECONDS));
        Date dateBeforeSubscription = new Date();
        //When
        Observable<UMod> uModsObservable = uModsRepository.getUModsOneByOneFromCacheOrDiskAndRefreshOnline();
        Flowable<UMod> uModsFlowable = RxJavaInterop.toV2Flowable(uModsObservable);

        //uModsObservable.test().assertCompleted();

        uModsFlowable
                .test()
                .await()
                .assertValueCount(1)
                .assertComplete()
                .assertValue(uMod ->
                        !uMod.getLastUpdateDate().equals(sevenSecondsAgo)
                        && uMod.getConnectionAddress() != null
                        && uMod.getConnectionAddress().equals(rpcResult.getWifi().getStaIp())
                        && uMod.getWifiSSID() != null
                        && uMod.getWifiSSID().equals(rpcResult.getWifi().getSsid())
                        && uMod.getState() == UMod.State.STATION_MODE
                        && uMod.getuModSource() == UMod.UModSource.MQTT_SCAN
                        );

        verify(dataBaseDataSourceMock, times(1)).saveUMod(any(UMod.class));
        assertFalse(uModsRepository.mCachedUMods.get("999").getLastUpdateDate().equals(sevenSecondsAgo));
    }


    @Test
    //TODO este test queda ignorado porque en distintas computadoras con distintos procesadores tarda distintos tiempos
    public void Given_FourModulesRespondWithStaIp_When_RefreshingUModsOnline_Then_CompletionTimeShouldBeShorten() throws InterruptedException {
        UMod cachedUMod = new UMod("999");
        //Same UMod is stored four times in the cache
        uModsRepository.mCachedUMods.put("1", cachedUMod);
        uModsRepository.mCachedUMods.put("2", cachedUMod);
        uModsRepository.mCachedUMods.put("3", cachedUMod);
        uModsRepository.mCachedUMods.put("4", cachedUMod);
        SysGetInfoRPC.Result rpcResult = new SysGetInfoRPC.Result(
                null, null, null, null, null,
                0, 0, 0, 0, 0,0,
                new SysGetInfoRPC.Result.Wifi(
                        "192.168.8.8",
                        "",//TODO WARNING: mongoose returns apIP as empty string when staIp is available
                        "got ip",
                        "SteveWiFi"),
                null);
        when(internetDataSourceMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.just(rpcResult).delay(500L, TimeUnit.MILLISECONDS));

        //When
        Observable<UMod> uModsObservable = uModsRepository.getUModsOneByOneFromCacheOrDiskAndRefreshOnline();
        Flowable<UMod> uModsFlowable = RxJavaInterop.toV2Flowable(uModsObservable);

        //uModsObservable.test().assertCompleted();

        Instant beforeSubscription = Instant.now();
        uModsFlowable
                .test()
                .await()
                .assertValueCount(4)
                .assertComplete();

        Long executionTimeMillis = Duration.between(beforeSubscription,Instant.now()).toMillis();
        //TODO why does it take always around 550?? Is 600 right? What if I run the same test on older slower hardware??
        Log.d("TEST", "TIME " + executionTimeMillis);
        assertTrue( executionTimeMillis < 600L);
        verify(dataBaseDataSourceMock, times(4)).saveUMod(any(UMod.class));
    }

    @Test
    public void Given_CacheIsCleanAndHasItems_When_GetOneByOne_Then_CacheUModsAreEmitted(){
        //GIVEN
        UMod cachedUMod = new UMod("777");
        uModsRepository.mCachedUMods.put(cachedUMod.getUUID(),cachedUMod);
        uModsRepository.mCacheIsDirty = false;
        //WHEN
        Observable<UMod> uModObservable = uModsRepository.getUModsOneByOne();
        //THEN
        uModObservable.test().assertCompleted().assertValueCount(1).assertValue(new UMod("777"));
        verify(internetDataSourceMock,times(1)).saveUMod(any(UMod.class));
    }

    @Test
    public void Given_CacheIsCleanAndHasNoItems_When_GetOneByOne_Then_DBUModsAreEmitted(){
        //GIVEN
        UMod databaseUMod = new UMod("888");
        uModsRepository.mCachedUMods.clear();
        uModsRepository.mCacheIsDirty = false;
        when(dataBaseDataSourceMock.getUModsOneByOne()).thenReturn(Observable.just(databaseUMod));
        //WHEN
        Observable<UMod> uModObservable = uModsRepository.getUModsOneByOne();
        //THEN
        uModObservable.test().assertCompleted().assertValueCount(1).assertValue(new UMod("888"));
        verify(internetDataSourceMock,times(1)).saveUMod(any(UMod.class));
    }


    @Test
    public void Given_CacheIsDirtyThereAreLanDiscoveriesAndNoDBModules_When_GetOneByOne_Then_DiscoveriesAreEmitted(){
        //GIVEN
        UMod dnssdDiscoveryUMod = new UMod("888","192.168.8.8",true);
        uModsRepository.mCacheIsDirty = true;
        when(lanDataSourceMock.getUModsOneByOne()).thenReturn(Observable.just(dnssdDiscoveryUMod));
        //This forces no internet refreshes.
        uModsRepository.mCachedUMods.clear();
        when(dataBaseDataSourceMock.getUModsOneByOne()).thenReturn(Observable.empty());
        //WHEN
        Observable<UMod> uModObservable = uModsRepository.getUModsOneByOne();
        //THEN
        uModObservable.test().assertCompleted().assertValueCount(1).assertValue(new UMod("888"));
        assertFalse(uModsRepository.mCacheIsDirty);
    }

    @Test
    public void Given_CacheIsDirtyAndThereIsOneLanDiscoveriesAndOneMqttRefresh_When_GetOneByOne_Then_BothUModsAreEmitted(){
        //GIVEN
        uModsRepository.mCacheIsDirty = true;
        UMod dnssdDiscoveryUMod = new UMod("888","192.168.8.8",true);
        when(lanDataSourceMock.getUModsOneByOne()).thenReturn(Observable.just(dnssdDiscoveryUMod));
        //This forces one internet refresh.
        UMod cachedUMod = new UMod("777");
        uModsRepository.mCachedUMods.put(cachedUMod.getUUID(), cachedUMod);
        SysGetInfoRPC.Result rpcResult = new SysGetInfoRPC.Result(
                null, null, null, null, null,
                0, 0, 0, 0, 0,0,
                new SysGetInfoRPC.Result.Wifi(
                        "192.168.7.7",
                        "",
                        "got ip",
                        "SteveWiFi"),
                null);
        when(internetDataSourceMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.just(rpcResult).delay(500L, TimeUnit.MILLISECONDS));
        //WHEN
        Observable<UMod> uModObservable = uModsRepository.getUModsOneByOne();
        //THEN
        uModObservable.test().awaitTerminalEvent().assertCompleted().assertValueCount(2);
    }


    @Test
    public void Given_APUMod_When_GetDataSourceChoices_Then_LanOnly(){
        //GIVEN
        UMod apModeUMod = new UMod("444");
        apModeUMod.setState(UMod.State.AP_MODE);
        //WHEN
        Observable<Pair<UModsDataSource,UModsDataSource>> datasourcesObservable = uModsRepository.getDataSourceChoices(apModeUMod);
        Flowable<Pair<UModsDataSource,UModsDataSource>> datasourceChoicesFlowable = RxJavaInterop.toV2Flowable(datasourcesObservable);
        //THEN
        datasourceChoicesFlowable.test()
                .assertValue(dataSourcePair -> dataSourcePair.first == lanDataSourceMock
                    && dataSourcePair.second == null)
                .assertValueCount(1)
                .assertComplete();
    }

    @Test
    public void Given_NotAPUModAndInvited_When_GetDataSourceChoices_Then_InternetOnly(){
        //GIVEN
        UMod invitedModeUMod = new UMod("555");
        invitedModeUMod.setState(UMod.State.STATION_MODE);
        invitedModeUMod.setAppUserLevel(UModUser.Level.INVITED);
        //WHEN
        Observable<Pair<UModsDataSource,UModsDataSource>> datasourcesObservable = uModsRepository.getDataSourceChoices(invitedModeUMod);
        Flowable<Pair<UModsDataSource,UModsDataSource>> datasourceChoicesFlowable = RxJavaInterop.toV2Flowable(datasourcesObservable);
        //THEN
        datasourceChoicesFlowable.test()
                .assertValue(dataSourcePair -> dataSourcePair.first == internetDataSourceMock
                        && dataSourcePair.second == null)
                .assertValueCount(1)
                .assertComplete();
    }
    @Test
    public void Given_NotAPUModNotInvitedPhoneOnWifiAndLanDiscovered_When_GetDataSourceChoices_Then_LanFirstAndInternetSecond(){
        //GIVEN
        UMod invitedModeUMod = new UMod("666");
        invitedModeUMod.setuModSource(UMod.UModSource.LAN_SCAN);
        invitedModeUMod.setState(UMod.State.STATION_MODE);
        invitedModeUMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(connectivityInfoMock.getConnectionType()).thenReturn(PhoneConnectivityInfo.ConnectionType.WIFI);
        //WHEN
        Observable<Pair<UModsDataSource,UModsDataSource>> datasourcesObservable = uModsRepository.getDataSourceChoices(invitedModeUMod);
        Flowable<Pair<UModsDataSource,UModsDataSource>> datasourceChoicesFlowable = RxJavaInterop.toV2Flowable(datasourcesObservable);
        //THEN
        datasourceChoicesFlowable.test()
                .assertValue(dataSourcePair -> dataSourcePair.first == lanDataSourceMock
                        && dataSourcePair.second == internetDataSourceMock)
                .assertValueCount(1)
                .assertComplete();
    }

    @Test
    public void Given_NotAPUModNotInvitedPhoneOnWifiNotLanDiscoveredAndMatchingSSIDs_When_GetDataSourceChoices_Then_LanFirstAndInternetSecond(){
        //GIVEN
        UMod invitedModeUMod = new UMod("666");
        invitedModeUMod.setState(UMod.State.STATION_MODE);
        invitedModeUMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(connectivityInfoMock.getConnectionType()).thenReturn(PhoneConnectivityInfo.ConnectionType.WIFI);
        invitedModeUMod.setuModSource(UMod.UModSource.CACHE);
        invitedModeUMod.setWifiSSID("SteveWifi");
        when(connectivityInfoMock.getWifiAPSSID()).thenReturn("SteveWifi");
        invitedModeUMod.setLanOperationEnabled(true);
        //WHEN
        Observable<Pair<UModsDataSource,UModsDataSource>> datasourcesObservable = uModsRepository.getDataSourceChoices(invitedModeUMod);
        Flowable<Pair<UModsDataSource,UModsDataSource>> datasourceChoicesFlowable = RxJavaInterop.toV2Flowable(datasourcesObservable);
        //THEN
        datasourceChoicesFlowable.test()
                .assertValue(dataSourcePair -> dataSourcePair.first == lanDataSourceMock
                        && dataSourcePair.second == internetDataSourceMock)
                .assertValueCount(1)
                .assertComplete();
    }

    @Test
    public void Given_NotAPUModNotInvitedPhoneOnWifiNotLanDiscoveredAndNoMatchingSSIDs_When_GetDataSourceChoices_Then_InternetOnly(){
        //GIVEN
        UMod invitedModeUMod = new UMod("666");
        invitedModeUMod.setState(UMod.State.STATION_MODE);
        invitedModeUMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(connectivityInfoMock.getConnectionType()).thenReturn(PhoneConnectivityInfo.ConnectionType.WIFI);
        invitedModeUMod.setuModSource(UMod.UModSource.CACHE);
        invitedModeUMod.setWifiSSID("SteveWifi");
        when(connectivityInfoMock.getWifiAPSSID()).thenReturn("Incu");
        invitedModeUMod.setLanOperationEnabled(true);
        //WHEN
        Observable<Pair<UModsDataSource,UModsDataSource>> datasourcesObservable = uModsRepository.getDataSourceChoices(invitedModeUMod);
        Flowable<Pair<UModsDataSource,UModsDataSource>> datasourceChoicesFlowable = RxJavaInterop.toV2Flowable(datasourcesObservable);
        //THEN
        datasourceChoicesFlowable.test()
                .assertValue(dataSourcePair -> dataSourcePair.first == internetDataSourceMock
                        && dataSourcePair.second == null)
                .assertValueCount(1)
                .assertComplete();
    }

    @Test
    public void Given_NotAPUModNotInvitedPhoneOnWifiNotLanDiscoveredAndLanDisabled_When_GetDataSourceChoices_Then_InternetOnly(){
        //GIVEN
        UMod invitedModeUMod = new UMod("666");
        invitedModeUMod.setState(UMod.State.STATION_MODE);
        invitedModeUMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(connectivityInfoMock.getConnectionType()).thenReturn(PhoneConnectivityInfo.ConnectionType.WIFI);
        invitedModeUMod.setuModSource(UMod.UModSource.CACHE);
        invitedModeUMod.setWifiSSID("SteveWifi");
        when(connectivityInfoMock.getWifiAPSSID()).thenReturn("SteveWifi");
        invitedModeUMod.setLanOperationEnabled(false);
        //WHEN
        Observable<Pair<UModsDataSource,UModsDataSource>> datasourcesObservable = uModsRepository.getDataSourceChoices(invitedModeUMod);
        Flowable<Pair<UModsDataSource,UModsDataSource>> datasourceChoicesFlowable = RxJavaInterop.toV2Flowable(datasourcesObservable);
        //THEN
        datasourceChoicesFlowable.test()
                .assertValue(dataSourcePair -> dataSourcePair.first == internetDataSourceMock
                        && dataSourcePair.second == null)
                .assertValueCount(1)
                .assertComplete();
    }

    @Test
    public void Given_AUniqueDataSourceRPCExecutionSuccess_When_ExecuteRPC_Then_AResponseIsEmitted(){
        //Given
        UMod testingUMod = new UMod("999");
        UModsDataSource uniqueDataSource = mock(UModsDataSource.class);
        UModsRepository.RPCExecutor executor = mock(UModsRepository.RPCExecutor.class);
        when(executor.getTargetUMod()).thenReturn(testingUMod);
        when(executor.executeRPC()).thenReturn(Observable.just(new RPC.Response(654,null,null)));
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        doReturn(Observable.just(new Pair<UModsDataSource, UModsDataSource>(uniqueDataSource,null)))
                .when(uModsRepositorySpy).getDataSourceChoices(testingUMod);
        //When
        Observable<RPC.Response> observable = uModsRepositorySpy.executeRPC(executor);
        Flowable<RPC.Response> flowable = RxJavaInterop.toV2Flowable(observable);
        flowable.test()
                .assertValueCount(1)
                .assertValue(response -> response.getResponseId() == 654)
                .assertComplete();

        verify(executor,times(1)).setCurrentDataSource(uniqueDataSource);
        verify(executor,times(1)).executeRPC();
    }


    @Test
    public void Given_AUniqueDataSourceRPCExecutionFailsOnHttpError_When_ExecuteRPC_Then_HttpErrorIsForwarded(){
        //Given
        UMod testingUMod = new UMod("999");
        UModsDataSource uniqueDataSource = mock(UModsDataSource.class);
        UModsRepository.RPCExecutor executor = mock(UModsRepository.RPCExecutor.class);

        HttpException httpException = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 500")
                )
        );

        when(executor.getTargetUMod()).thenReturn(testingUMod);
        when(executor.executeRPC()).thenReturn(Observable.error(httpException));

        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        doReturn(Observable.just(new Pair<UModsDataSource, UModsDataSource>(uniqueDataSource,null)))
                .when(uModsRepositorySpy).getDataSourceChoices(testingUMod);
        //When
        Observable<RPC.Response> observable = uModsRepositorySpy.executeRPC(executor);
        Flowable<RPC.Response> flowable = RxJavaInterop.toV2Flowable(observable);
        flowable.test()
                .assertError(HttpException.class)
                .assertError(throwable -> ((HttpException)throwable).code() == 500);

        verify(executor,times(1)).setCurrentDataSource(uniqueDataSource);
        verify(executor,times(1)).executeRPC();
    }

    @Test
    public void Given_AUniqueDataSourceRPCExecutionFailureIsNotHttpError_When_ExecuteRPC_Then_ExceptionIsForwarded(){
        //Given
        UMod testingUMod = new UMod("999");
        UModsDataSource uniqueDataSource = mock(UModsDataSource.class);
        UModsRepository.RPCExecutor executor = mock(UModsRepository.RPCExecutor.class);
        IOException ioException = new IOException("Testing IO Exception");
        when(executor.getTargetUMod()).thenReturn(testingUMod);
        when(executor.executeRPC()).thenReturn(Observable.error(ioException));

        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        doReturn(Observable.just(new Pair<UModsDataSource, UModsDataSource>(uniqueDataSource,null)))
                .when(uModsRepositorySpy).getDataSourceChoices(testingUMod);

        //When
        Observable<RPC.Response> observable = uModsRepositorySpy.executeRPC(executor);
        Flowable<RPC.Response> flowable = RxJavaInterop.toV2Flowable(observable);
        //Then
        flowable.test()
                .assertError(IOException.class)
                .assertError(throwable -> throwable.getMessage().contains("Testing"));

        verify(executor,times(1)).setCurrentDataSource(uniqueDataSource);
        verify(executor,times(1)).executeRPC();
    }

    @Test
    public void Given_TwoDataSourcesRPCExecutionFailureInFirstAndIsNotHttpError_When_ExecuteRPC_Then_ResponseIsEmittedBySecondDS(){
        //Given
        UMod testingUMod = new UMod("999");
        UModsDataSource firstDataSource = mock(UModsDataSource.class);
        UModsDataSource secondDataSource = mock(UModsDataSource.class);
        UModsRepository.RPCExecutor executor = mock(UModsRepository.RPCExecutor.class);
        IOException ioException = new IOException("Testing IO Exception");
        when(executor.getTargetUMod()).thenReturn(testingUMod);
        when(executor.executeRPC()).thenReturn(Observable.error(ioException),
                Observable.just(new RPC.Response(888,null,null)));

        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        doReturn(Observable.just(new Pair<>(firstDataSource, secondDataSource)))
                .when(uModsRepositorySpy).getDataSourceChoices(testingUMod);

        //When
        Observable<RPC.Response> observable = uModsRepositorySpy.executeRPC(executor);
        Flowable<RPC.Response> flowable = RxJavaInterop.toV2Flowable(observable);
        //Then
        flowable.test()
                .assertValueCount(1)
                .assertValue(response -> response.getResponseId() == 888)
                .assertComplete();

        verify(executor,times(1)).setCurrentDataSource(firstDataSource);
        verify(executor,times(1)).setCurrentDataSource(secondDataSource);
        verify(executor,times(2)).executeRPC();
        verifyNoMoreInteractions(executor);
    }

    @Test
    public void Given_TwoDataSourcesRPCExecutionFailsOnBothSources_When_ExecuteRPC_Then_ExceptionIsForwarded(){
        //Given
        UMod testingUMod = new UMod("999");
        UModsDataSource firstDataSource = mock(UModsDataSource.class);
        UModsDataSource secondDataSource = mock(UModsDataSource.class);
        UModsRepository.RPCExecutor executor = mock(UModsRepository.RPCExecutor.class);
        IOException firstIOException = new IOException("First IO Exception");
        IOException secondIOException = new IOException("Second IO Exception");
        when(executor.getTargetUMod()).thenReturn(testingUMod);
        when(executor.executeRPC()).thenReturn(Observable.error(firstIOException),
                Observable.error(secondIOException));

        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        doReturn(Observable.just(new Pair<>(firstDataSource, secondDataSource)))
                .when(uModsRepositorySpy).getDataSourceChoices(testingUMod);

        //When
        Observable<RPC.Response> observable = uModsRepositorySpy.executeRPC(executor);
        Flowable<RPC.Response> flowable = RxJavaInterop.toV2Flowable(observable);
        //Then
        flowable.test()
                .assertNoValues()
                .assertError(IOException.class)
                .assertError(throwable -> throwable.getMessage().contains("Second"));

        verify(executor,times(1)).setCurrentDataSource(firstDataSource);
        verify(executor,times(1)).setCurrentDataSource(secondDataSource);
        verify(executor,times(2)).executeRPC();
        verifyNoMoreInteractions(executor);
    }

    @Test
    public void When_TriggerUMod_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypes(){
        //When
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class));
        //Then
        ArgumentCaptor<UModsRepository.RPCExecutor> executorCaptor = ArgumentCaptor.forClass(UModsRepository.RPCExecutor.class);
        verify(uModsRepositorySpy,atMost(1)).executeRPC(executorCaptor.capture());
        UModsRepository.RPCExecutor capturedExecutor = executorCaptor.getValue();
        assertTrue(capturedExecutor.getRequest() instanceof TriggerRPC.Request);
    }

    @Test
    public void When_getUserLevel_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypes(){
        //When
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.getUserLevel(any(UMod.class), any(GetUserLevelRPC.Arguments.class));
        //Then
        ArgumentCaptor<UModsRepository.RPCExecutor> executorCaptor = ArgumentCaptor.forClass(UModsRepository.RPCExecutor.class);
        verify(uModsRepositorySpy,atMost(1)).executeRPC(executorCaptor.capture());
        UModsRepository.RPCExecutor capturedExecutor = executorCaptor.getValue();
        assertTrue(capturedExecutor.getRequest() instanceof GetUserLevelRPC.Request);
    }

    @Test
    public void When_deleteUModUser_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypes(){
        //When
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.deleteUModUser(any(UMod.class), any(DeleteUserRPC.Arguments.class));
        //Then
        ArgumentCaptor<UModsRepository.RPCExecutor> executorCaptor = ArgumentCaptor.forClass(UModsRepository.RPCExecutor.class);
        verify(uModsRepositorySpy,atMost(1)).executeRPC(executorCaptor.capture());
        UModsRepository.RPCExecutor capturedExecutor = executorCaptor.getValue();
        assertTrue(capturedExecutor.getRequest() instanceof DeleteUserRPC.Request);
    }

    @Test
    public void When_updateUModUser_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypes(){
        //When
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.updateUModUser(any(UMod.class), any(UpdateUserRPC.Arguments.class));
        //Then
        ArgumentCaptor<UModsRepository.RPCExecutor> executorCaptor = ArgumentCaptor.forClass(UModsRepository.RPCExecutor.class);
        verify(uModsRepositorySpy,atMost(1)).executeRPC(executorCaptor.capture());
        UModsRepository.RPCExecutor capturedExecutor = executorCaptor.getValue();
        assertTrue(capturedExecutor.getRequest() instanceof UpdateUserRPC.Request);
    }

    @Test
    public void When_getUModUser_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypes(){
        //When
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.getUModUsers(any(UMod.class), any(GetUsersRPC.Arguments.class));
        //Then
        ArgumentCaptor<UModsRepository.RPCExecutor> executorCaptor = ArgumentCaptor.forClass(UModsRepository.RPCExecutor.class);
        verify(uModsRepositorySpy,atMost(1)).executeRPC(executorCaptor.capture());
        UModsRepository.RPCExecutor capturedExecutor = executorCaptor.getValue();
        assertTrue(capturedExecutor.getRequest() instanceof GetUsersRPC.Request);
    }

    @Test
    public void When_getSystemInfo_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypes(){
        //When
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.getSystemInfo(any(UMod.class), any(SysGetInfoRPC.Arguments.class));
        //Then
        ArgumentCaptor<UModsRepository.RPCExecutor> executorCaptor = ArgumentCaptor.forClass(UModsRepository.RPCExecutor.class);
        verify(uModsRepositorySpy,atMost(1)).executeRPC(executorCaptor.capture());
        UModsRepository.RPCExecutor capturedExecutor = executorCaptor.getValue();
        assertTrue(capturedExecutor.getRequest() instanceof SysGetInfoRPC.Request);
    }

    @Test
    public void When_setWiFiAP_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypes(){
        //When
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.setWiFiAP(any(UMod.class), any(SetWiFiRPC.Arguments.class));
        //Then
        ArgumentCaptor<UModsRepository.RPCExecutor> executorCaptor = ArgumentCaptor.forClass(UModsRepository.RPCExecutor.class);
        verify(uModsRepositorySpy,atMost(1)).executeRPC(executorCaptor.capture());
        UModsRepository.RPCExecutor capturedExecutor = executorCaptor.getValue();
        assertTrue(capturedExecutor.getRequest() instanceof SetWiFiRPC.Request);
    }

    @Test
    public void When_getFirmwareImageFile_Then_InternetDSIsCalledWithSameUMod(){
        //Given
        UMod moduloMock= new UMod("777");
        //When
        Observable<File> fileObservable = this.uModsRepository.getFirmwareImageFile(moduloMock);
        //Then
        verify(internetDataSourceMock,times(1)).getFirmwareImageFile(moduloMock);
        verifyNoMoreInteractions(internetDataSourceMock);
        verifyZeroInteractions(dataBaseDataSourceMock);
        verifyZeroInteractions(lanDataSourceMock);
    }

    @Test
    public void When_postFirmwareUpdateToUMod_Then_LanDSEsLlamadaConUModYArchivo(){
        //Given
        UMod moduloMock= new UMod("777");
        File archivoMock= new File("ArchivoDescargado");
        //When
        uModsRepository.postFirmwareUpdateToUMod(moduloMock,archivoMock);
        //Then
        verify(lanDataSourceMock,times(1)).postFirmwareUpdateToUMod(moduloMock,archivoMock);
        verifyNoMoreInteractions(lanDataSourceMock);
        verifyZeroInteractions(dataBaseDataSourceMock);
        verifyZeroInteractions(internetDataSourceMock);
    }

    @Test
    public void When_otaCommit_Then_LanDSEsLlamadaConUModAndArguments(){
        //GIVEN
        UMod moduloMock= new UMod("777");
        OTACommitRPC.Arguments arguments= new OTACommitRPC.Arguments();

        //WHEN
        uModsRepository.otaCommit(moduloMock,arguments);

        //THEN
        verify(lanDataSourceMock,times(1)).otaCommit(moduloMock,arguments);
    }

    @Test
    public void When_factoryResetUMod_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypes(){
        //When
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.factoryResetUMod(any(UMod.class), any(FactoryResetRPC.Arguments.class));
        //Then
        ArgumentCaptor<UModsRepository.RPCExecutor> executorCaptor = ArgumentCaptor.forClass(UModsRepository.RPCExecutor.class);
        verify(uModsRepositorySpy,atMost(1)).executeRPC(executorCaptor.capture());
        UModsRepository.RPCExecutor capturedExecutor = executorCaptor.getValue();
        assertTrue(capturedExecutor.getRequest() instanceof FactoryResetRPC.Request);
    }

    @Test
    public void When_getCurrentLocation_Then_locationServiceIsCalled(){
        //When
        this.uModsRepository.getCurrentLocation();
        //Then
        verify(locationServiceMock,times(1)).getCurrentLocation();

    }

    @Test
    public void When_getAddressFromLocation_Then_locationServiceIsCalledConDireccion(){
        //GIVEN
        Location locationMock= new Location("11");
        //When
        this.uModsRepository.getAddressFromLocation(locationMock);
        //Then
        verify(locationServiceMock,times(1)).getAddressFromLocation(locationMock);

    }

    @Test
    public void When_createUModUserByName_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypes(){
        //When
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.createUModUserByName(any(UMod.class), any(AdminCreateUserRPC.Arguments.class));
        //Then
        ArgumentCaptor<UModsRepository.RPCExecutor> executorCaptor = ArgumentCaptor.forClass(UModsRepository.RPCExecutor.class);
        verify(uModsRepositorySpy,atMost(1)).executeRPC(executorCaptor.capture());
        UModsRepository.RPCExecutor capturedExecutor = executorCaptor.getValue();
        assertTrue(capturedExecutor.getRequest() instanceof AdminCreateUserRPC.Request);
    }

    @Test
    public void When_saveUMod_Then_SeCargaModuloEnCache(){
        //When
        UMod moduloMockSaveUMod= new UMod("111");
        uModsRepository.mCachedUMods.clear();

        //Then
        uModsRepository.saveUMod(moduloMockSaveUMod);

        verify(dataBaseDataSourceMock,times(1)).saveUMod(moduloMockSaveUMod);
        assertEquals(1,uModsRepository.mCachedUMods.size());
    }


    @Test
    public void Given_TeniendoModuloEnCacheConUUID_When_updateUModAlias_Then_CambiaElAliasDelModulo(){
        //GIVEN
        UMod moduloMockUpdateUModAlias= new UMod("222");
        when(dataBaseDataSourceMock.updateUModAlias(moduloMockUpdateUModAlias.getUUID(),"NuevoAlias"))
                .thenReturn(Observable.just(moduloMockUpdateUModAlias));
        //WHEN
        uModsRepository.updateUModAlias("222", "NuevoAlias").test();
        //THEN
        verify(dataBaseDataSourceMock,times(1)).updateUModAlias("222","NuevoAlias");
        assertEquals(1, uModsRepository.mCachedUMods.size());
    }


    @Test
    public void Given_NoTeniendoModuloCache_When_setUModNotificationStatus_Then_SeteoNotificaionPermanenteDelModuloEnBD(){
        //Given
        uModsRepository.mCachedUMods.clear();

        //When
        uModsRepository.setUModNotificationStatus("123",true);

        //Then
        verify(dataBaseDataSourceMock,times(1)).setUModNotificationStatus("123",true);
    }

    @Test
    public void Given_TeniendoModuloCache_When_setUModNotificationStatus_Then_SeteoNotificacionPermanenteAlModuloCacheadoYBD(){
        //GIVEN
        UMod moduloMockSetNotificationStatus= new UMod("222");
        moduloMockSetNotificationStatus.setOngoingNotificationStatus(true);
        uModsRepository.mCachedUMods.put(moduloMockSetNotificationStatus.getUUID(),moduloMockSetNotificationStatus);
        //WHEN
        uModsRepository.setUModNotificationStatus(moduloMockSetNotificationStatus.getUUID(),false);
        //THEN
        verify(dataBaseDataSourceMock, times(1)).setUModNotificationStatus("222",false);
        assertFalse(moduloMockSetNotificationStatus.isOngoingNotificationEnabled());
    }


    @Test
    public void Given_TeniendoModulosCache_When_clearAlienUMods_Then_BorroCacheModulosNoAutorizados(){
        //GIVEN
        UMod moduloMockAutorizado = new UMod("999");
        moduloMockAutorizado.setAppUserLevel(UModUser.Level.AUTHORIZED);
        uModsRepository.mCachedUMods.put(moduloMockAutorizado.getUUID(),moduloMockAutorizado);


        UMod moduloMockNoAutorizado = new UMod("666");
        moduloMockNoAutorizado.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
        uModsRepository.mCachedUMods.put(moduloMockNoAutorizado.getUUID(),moduloMockNoAutorizado);
        //When

        uModsRepository.clearAlienUMods();

        //Then
        verify(lanDataSourceMock,times(1)).clearAlienUMods();
        verify(dataBaseDataSourceMock,times(1)).clearAlienUMods();

        assertEquals(1,  uModsRepository.mCachedUMods.size());
    }

    @Test
    public void Given_cacheLimpia_When_getUMod_Then_SeObtienenDB(){
        //GIVEN
        UMod moduloMockGetUMod = new UMod("111");
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.mCacheIsDirty=false;

        //WHEN
        uModsRepositorySpy.getUMod(moduloMockGetUMod.getUUID()).test();

        //Then
        verify(uModsRepositorySpy,times(1)).getSingleUModFromCacheOrDB(moduloMockGetUMod.getUUID());
    }

    @Test
    public void Given_cacheSucia_When_getUMod_Then_SeObtieneDeLanOrBDEntry(){
        //GIVEN
        UMod moduloMockGetUMod = new UMod("111");
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.mCacheIsDirty=true;

        //WHEN
        uModsRepositorySpy.getUMod(moduloMockGetUMod.getUUID()).test();

        //Then
        verify(uModsRepositorySpy,times(1))
                .getSingleUModFromLanAndUpdateDBEntry(moduloMockGetUMod.getUUID());
    }

    @Test
    public void Given_ModulosEnCache_When_getSingleUModFromLanAndUpdateDBEntry_Then_DevuelveSoloEse(){
        //GIVEN
        UMod moduloMockUno = new UMod("111");
        UMod moduloMockDos = new UMod("222");

        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        when(uModsRepositorySpy.getUModsOneByOneFromLanAndInternetAndUpdateDBAndCache())
                .thenReturn(Observable.just(moduloMockUno,moduloMockDos,moduloMockUno));

        //WHEN
        uModsRepositorySpy.getSingleUModFromLanAndUpdateDBEntry(moduloMockUno.getUUID()).test()
                .assertCompleted()
                .assertValueCount(1)
                .assertValue(moduloMockUno);
    }

    @Test
    public void Given_TengoModuloBuscadoCache_When_getSingleUModFromCacheOrDB_Then_DevuelveModuloMarcadoDeCache(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        uModsRepository.mCachedUMods.put(moduloMock.getUUID(),moduloMock);

        Observable<UMod> testObservable = uModsRepository.getSingleUModFromCacheOrDB(moduloMock.getUUID());
        Flowable<UMod> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        testFlowable.test()
                .assertValueCount(1)
                .assertValue(moduloMock)
                .assertComplete();
        assertEquals(UMod.UModSource.CACHE,moduloMock.getuModSource());
    }

    @Test
    public void Given_NoTengoModuloBuscadoCache_When_getSingleUModFromCacheOrDB_Then_DevuelveModuloMarcadoDeDB(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        when(dataBaseDataSourceMock.getUMod(moduloMock.getUUID())).thenReturn(Observable.just(moduloMock));
        uModsRepository.mCachedUMods.clear();


        //WHEN
        uModsRepository.getSingleUModFromCacheOrDB(moduloMock.getUUID()).test()
                .assertValueCount(1)
                .assertValue(moduloMock)
                .assertCompleted();
    }

    @Test
    public void Given_TengoModuloBuscadoCacheDistintoUUID_When_getSingleUModFromCacheOrDB_Then_DevuelveModuloMarcadoDeDB(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        UMod moduloMockDos= new UMod("222");

        when(dataBaseDataSourceMock.getUMod(moduloMockDos.getUUID())).thenReturn(Observable.just(moduloMockDos));
        uModsRepository.mCachedUMods.put(moduloMock.getUUID(),moduloMock);


        //WHEN
        uModsRepository.getSingleUModFromCacheOrDB(moduloMockDos.getUUID()).test()
                .assertValueCount(1)
                .assertValue(moduloMockDos)
                .assertCompleted();
    }

    @Test
    public void Given_TeniendoModulosEnCacheAndBDAndLan_When_deleteAllUMods_Then_SeBorranDeAllSources(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        uModsRepository.mCachedUMods.put(moduloMock.getUUID(),moduloMock);

        //WHEN
        uModsRepository.deleteAllUMods();

        //THEN
        verify(dataBaseDataSourceMock,times(1)).deleteAllUMods();
        verify(lanDataSourceMock,times(1)).deleteAllUMods();
        assertEquals(0,uModsRepository.mCachedUMods.size());
    }

    @Test
    public void Given_TeniendoModuloCargadoCacheAndBD_When_deleteUMod_Then_SeBorraDeCacheAndBD(){
        //GIVEN
        UMod moduloMockUno= new UMod("111");
        UMod moduloMockDos= new UMod("222");
        uModsRepository.mCachedUMods.put(moduloMockUno.getUUID(),moduloMockUno);
        uModsRepository.mCachedUMods.put(moduloMockDos.getUUID(),moduloMockDos);

        //WHEN
        uModsRepository.deleteUMod(moduloMockUno.getUUID());

        //THEN
        assertEquals(1,uModsRepository.mCachedUMods.size());
        verify(dataBaseDataSourceMock,times(1)).deleteUMod(moduloMockUno.getUUID());
    }

    @Test
    public void When_createUModUser_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypes(){
        //When
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        uModsRepositorySpy.createUModUser(any(UMod.class), any(CreateUserRPC.Arguments.class));
        //Then
        ArgumentCaptor<UModsRepository.RPCExecutor> executorCaptor = ArgumentCaptor.forClass(UModsRepository.RPCExecutor.class);
        verify(uModsRepositorySpy,atMost(1)).executeRPC(executorCaptor.capture());
        UModsRepository.RPCExecutor capturedExecutor = executorCaptor.getValue();
        assertTrue(capturedExecutor.getRequest() instanceof CreateUserRPC.Request);


    }

    @Test
    public void When_createUModUser_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypesConErrorHTTP(){
        //When
        UMod testUMod = new UMod("111");
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        //uModsRepositorySpy.createUModUser(any(UMod.class), any(CreateUserRPC.Arguments.class));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"),"409");
        Response response = Response.error(500,responseBody);
        doReturn(Observable.error(new HttpException(response))).when(uModsRepositorySpy).executeRPC(any(UModsRepository.RPCExecutor.class));
       // When
        uModsRepositorySpy.createUModUser(testUMod,new CreateUserRPC.Arguments("fake_credentials")).test()
        .assertError(HttpException.class).assertNoValues();
        //Then
        verify(mqttServiceMock,times(1)).cancelMyInvitation(testUMod);
    }

    @Test
    public void When_createUModUser_Then_ExecuteRPCIsCalledOnlyOnceWithProperTypesConErrorNoHTTP(){
        //When
        UMod testUMod = new UMod("111");
        UModsRepository uModsRepositorySpy = spy(uModsRepository);
        doReturn(Observable.error(new Exception())).when(uModsRepositorySpy).executeRPC(any(UModsRepository.RPCExecutor.class));
        // When
        uModsRepositorySpy.createUModUser(testUMod,new CreateUserRPC.Arguments("fake_credentials")).test()
                .assertError(Exception.class).assertNoValues();

    }



}