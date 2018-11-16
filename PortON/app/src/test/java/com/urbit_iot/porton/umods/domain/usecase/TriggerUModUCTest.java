package com.urbit_iot.porton.umods.domain.usecase;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.appuser.domain.AppUser;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.APIUserType;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.TriggerRPC;
import com.urbit_iot.porton.data.source.PhoneConnectivity;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.SchedulerProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.IOException;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Flowable;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.adapter.rxjava.HttpException;
import retrofit2.Response;
import rx.Observable;
import rx.observers.AssertableSubscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TriggerUModUCTest {

    private TriggerUModUC triggerUModUC;

    @Mock
    private UModsRepository uModsRepositoryMock;

    @Mock
    private AppUserRepository appUserRepositoryMock;

    @Mock
    private PhoneConnectivity connectivityInfoMock;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Spy
    private SchedulerProvider schedulerProvider;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(Schedulers.immediate()).when(schedulerProvider).ui();
        this.triggerUModUC = new TriggerUModUC(uModsRepositoryMock, schedulerProvider, appUserRepositoryMock, connectivityInfoMock);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void Given_ThereIsAStoredModuleAndTriggerSuccess_When_Triggered_Then_ResultIsEmitted() {
        //Given
        UMod retrievedUMod = new UMod("888");
        retrievedUMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(uModsRepositoryMock.getUMod("888")).thenReturn(Observable.just(retrievedUMod));
        when(uModsRepositoryMock.triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.just(new TriggerRPC.Result("Success!")));
        TriggerUModUC.RequestValues requestValues = new TriggerUModUC.RequestValues("888");

        //When
        AssertableSubscriber<TriggerUModUC.ResponseValues>  responseSub= this.triggerUModUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock, times(1)).cachedFirst();
        verify(uModsRepositoryMock, times(1)).getUMod("888");
        verify(uModsRepositoryMock, times(1)).triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class));
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyZeroInteractions(appUserRepositoryMock);
        verifyZeroInteractions(connectivityInfoMock);
    }

    @Test
    public void Given_FirstTriggerAttemptFails_When_TriggerExecution_Then_ResultIsEmittedOnRetry() {
        //Given
        UMod retrievedUMod = new UMod("888");
        retrievedUMod.setAppUserLevel(UModUser.Level.AUTHORIZED);

        retrievedUMod.setWifiSSID("SteveWifi");

        when(uModsRepositoryMock.getUMod("888")).thenReturn(Observable.just(retrievedUMod));
        when(uModsRepositoryMock.triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.error(new IOException()))
                .thenReturn(Observable.just(new TriggerRPC.Result("Success!")));
        when(connectivityInfoMock.getConnectionType()).thenReturn(PhoneConnectivity.ConnectionType.WIFI);
        when(connectivityInfoMock.getWifiAPSSID()).thenReturn("SteveWifi");
        TriggerUModUC.RequestValues requestValues = new TriggerUModUC.RequestValues("888");
        //When
        AssertableSubscriber<TriggerUModUC.ResponseValues> responseSub = this.triggerUModUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock, times(1)).cachedFirst();
        verify(uModsRepositoryMock, times(1)).refreshUMods();
        verify(uModsRepositoryMock, times(2)).triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class));
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyZeroInteractions(appUserRepositoryMock);
        verifyZeroInteractions(connectivityInfoMock);
    }

    @Test
    public void Given_TriggerFailsBecauseUserWasDeleted_When_TriggerExecution_Then_UserIsSavedUnauthorizedAndDeletedUserExceptionIsPropagated() {
        //Given
        UMod retrievedUMod = new UMod("888");
        HttpException httpException = new HttpException(
                Response.error(
                        401,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 401")
                )
        );

        when(uModsRepositoryMock.getUMod("888")).thenReturn(Observable.just(retrievedUMod));
        when(uModsRepositoryMock.triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.error(httpException));
        TriggerUModUC.RequestValues requestValues = new TriggerUModUC.RequestValues("888");
        //When
        io.reactivex.subscribers.TestSubscriber response = RxJavaInterop.toV2Flowable(this.triggerUModUC.buildUseCase(requestValues)).test();

        //Then
        response.assertError(TriggerUModUC.DeletedUserException.class)
                .assertError(throwable ->
                        ((TriggerUModUC.DeletedUserException) throwable)
                                .getInaccessibleUMod() == retrievedUMod)
                .assertValueCount(0);
        assertEquals(UModUser.Level.UNAUTHORIZED, retrievedUMod.getAppUserLevel());
        verify(uModsRepositoryMock, times(1)).cachedFirst();
        verify(uModsRepositoryMock, times(0)).refreshUMods();
        verify(uModsRepositoryMock, times(1)).triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class));
        verify(uModsRepositoryMock, times(1)).saveUMod(any(UMod.class));

        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyZeroInteractions(appUserRepositoryMock);
        verifyZeroInteractions(connectivityInfoMock);
    }

    @Test
    public void  Given_TriggerFailsBecauseUserLevelWasChanged_When_TriggerExecution_Then_UserLevelIsUpdatedAndTriggerResultIsEmitted() {
        //Given
        UMod retrievedUMod = new UMod("888");
        retrievedUMod.setWifiSSID("SteveWifi");
        retrievedUMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        HttpException httpException = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 500")
                )
        );
        AppUser appUserMock = new AppUser("123456", "123123", "123456789");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUserMock));
        when(uModsRepositoryMock.getUserLevel(any(UMod.class), any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.just(new GetUserLevelRPC.Result("Steve", APIUserType.Admin)));
        when(uModsRepositoryMock.getUMod("888")).thenReturn(Observable.just(retrievedUMod));
        when(uModsRepositoryMock.triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.error(httpException))
                .thenReturn(Observable.just(new TriggerRPC.Result("Success")));
        TriggerUModUC.RequestValues requestValues = new TriggerUModUC.RequestValues("888");

        //When
        AssertableSubscriber<TriggerUModUC.ResponseValues> response = this.triggerUModUC.buildUseCase(requestValues).test();

        //Then
        response.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock, times(1)).cachedFirst();
        verify(uModsRepositoryMock, times(1)).getUMod("888");
        verify(uModsRepositoryMock, times(2)).triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class));
        verify(appUserRepositoryMock, times(1)).getAppUser();
        verify(uModsRepositoryMock, times(1)).saveUMod(retrievedUMod);
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyZeroInteractions(appUserRepositoryMock);
        verifyZeroInteractions(connectivityInfoMock);
    }

    @Test
    public void Given_TriggerFailsBecauseUserLevelWasChangedAndUpdateNotSuccesful_When_TriggerExecution_Then_TriggerResultIsEmittedOnRetry() {
        //Given
        UMod retrievedUMod = new UMod("888");
        retrievedUMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        retrievedUMod.setWifiSSID("SteveWifi");
        HttpException httpException = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 500")
                )
        );
        when(connectivityInfoMock.getConnectionType()).thenReturn(PhoneConnectivity.ConnectionType.WIFI);
        when(connectivityInfoMock.getWifiAPSSID()).thenReturn("SteveWifi");
        AppUser appUserMock = new AppUser("123456", "123123", "123456789");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUserMock));
        GetUserLevelRPC.Arguments arguments = new GetUserLevelRPC.Arguments("Steve");
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class))).thenReturn(Observable.error(new IOException()));
        when(uModsRepositoryMock.getUMod("888")).thenReturn(Observable.just(retrievedUMod));
        when(uModsRepositoryMock.triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.error(httpException))
                .thenReturn(Observable.just(new TriggerRPC.Result("Success")));
        TriggerUModUC.RequestValues requestValues = new TriggerUModUC.RequestValues("888");

        //When
        AssertableSubscriber<TriggerUModUC.ResponseValues> response = this.triggerUModUC.buildUseCase(requestValues).test();

        //Then
        response.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock, times(1)).cachedFirst();
        verify(uModsRepositoryMock, times(1)).getUMod("888");
        verify(uModsRepositoryMock, times(2)).triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class));
        verify(appUserRepositoryMock, times(1)).getAppUser();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock, never()).saveUMod(retrievedUMod);
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyZeroInteractions(appUserRepositoryMock);
        verifyZeroInteractions(connectivityInfoMock);
    }

    @Test
    public void Given_TriggerFailsBecauseUserLevelWasChangedAndImmediatlyDeleted__When_TriggeredExecution_Then_UserIsSavedUnauthorizedAndDeletedUserExceptionIsPropagated(){
        UMod retrievedUMod = new UMod("888");
        retrievedUMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        retrievedUMod.setWifiSSID("SteveWifi");
        HttpException httpException = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 500")
                )
        );
        HttpException UserLevelResponse = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 404")
                )
        );
        AppUser appUserMock = new AppUser("123456", "123123", "123456789");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUserMock));
        GetUserLevelRPC.Arguments arguments = new GetUserLevelRPC.Arguments("Steve");
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class))).thenReturn(Observable.error(UserLevelResponse));
        when(uModsRepositoryMock.getUMod("888")).thenReturn(Observable.just(retrievedUMod));
        when(uModsRepositoryMock.triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.error(httpException));
        TriggerUModUC.RequestValues requestValues = new TriggerUModUC.RequestValues("888");

        //When
        AssertableSubscriber<TriggerUModUC.ResponseValues> response = this.triggerUModUC.buildUseCase(requestValues).test();

        //Then
        response.assertError(TriggerUModUC.DeletedUserException.class);
        verify(uModsRepositoryMock, times(1)).cachedFirst();
        //getUMod is called just once because retry is done on resubscription.
        verify(uModsRepositoryMock, times(1)).getUMod("888");
        verify(uModsRepositoryMock, times(1)).triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class));
        verify(appUserRepositoryMock, times(1)).getAppUser();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock, times(1)).saveUMod(retrievedUMod);
        verify(uModsRepositoryMock,never()).refreshUMods();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyZeroInteractions(appUserRepositoryMock);
        verifyZeroInteractions(connectivityInfoMock);
    }

    @Test
    public void Given_TriggerFailsAndUserLevelResponseIsAExceptionUnconsidered__When_TriggeredExecution_Then_ErrorIsPropagated(){
        UMod retrievedUMod = new UMod("888");
        retrievedUMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        retrievedUMod.setWifiSSID("SteveWifi");
        HttpException httpException = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 500")
                )
        );
        HttpException UserLevelResponse = new HttpException(
                Response.error(
                        400,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 400")
                )
        );
        AppUser appUserMock = new AppUser("123456", "123123", "123456789");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUserMock));
        GetUserLevelRPC.Arguments arguments = new GetUserLevelRPC.Arguments("Steve");
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class))).thenReturn(Observable.error(UserLevelResponse));
        when(uModsRepositoryMock.getUMod("888")).thenReturn(Observable.just(retrievedUMod));
        when(uModsRepositoryMock.triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.error(httpException));
        TriggerUModUC.RequestValues requestValues = new TriggerUModUC.RequestValues("888");

        //When
        AssertableSubscriber<TriggerUModUC.ResponseValues> response = this.triggerUModUC.buildUseCase(requestValues).test();

        //Then
        response.assertError(HttpException.class);
        verify(uModsRepositoryMock, times(1)).cachedFirst();
        verify(uModsRepositoryMock, times(1)).getUMod("888");
        verify(uModsRepositoryMock, times(1)).triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class));
        verify(appUserRepositoryMock, times(1)).getAppUser();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock, never()).saveUMod(retrievedUMod);
        verify(uModsRepositoryMock,never()).refreshUMods();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyZeroInteractions(appUserRepositoryMock);
        verifyZeroInteractions(connectivityInfoMock);
    }

    @Test
    public void  Given_TriggerFailsWithAUnconsideredHTTPException_When_TriggeredExecution_Then_ErrorIsPropagated(){
        UMod retrievedUMod = new UMod("888");

        HttpException httpException = new HttpException(
                Response.error(
                        405,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 405")
                )
        );

        when(uModsRepositoryMock.getUMod("888")).thenReturn(Observable.just(retrievedUMod));
        when(uModsRepositoryMock.triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.error(httpException));
        TriggerUModUC.RequestValues requestValues = new TriggerUModUC.RequestValues("888");

        //When
        AssertableSubscriber<TriggerUModUC.ResponseValues> response = this.triggerUModUC.buildUseCase(requestValues).test();

        //Then
        response.assertError(HttpException.class);
        verify(uModsRepositoryMock, times(1)).cachedFirst();
        verify(uModsRepositoryMock, times(1)).getUMod("888");
        verify(uModsRepositoryMock, times(1)).triggerUMod(any(UMod.class), any(TriggerRPC.Arguments.class));
        verifyNoMoreInteractions(uModsRepositoryMock);
    }

}