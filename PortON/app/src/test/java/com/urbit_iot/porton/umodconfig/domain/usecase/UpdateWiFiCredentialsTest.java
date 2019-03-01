package com.urbit_iot.porton.umodconfig.domain.usecase;


import androidx.core.util.Pair;

import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.SetWiFiRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.SchedulerProvider;

import java.io.IOException;


import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.adapter.rxjava.HttpException;
import retrofit2.Response;
import rx.Observable;
import rx.observers.AssertableSubscriber;
import rx.schedulers.Schedulers;

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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UpdateWiFiCredentialsTest {
    private UpdateWiFiCredentials updateWiFiCredentials;

    @Mock
    private UModsRepository uModsRepositoryMock;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Spy
    private SchedulerProvider schedulerProvider;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(Schedulers.immediate()).when(schedulerProvider).ui();
        this.updateWiFiCredentials = new UpdateWiFiCredentials(uModsRepositoryMock,schedulerProvider);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(uModsRepositoryMock);
        verify(schedulerProvider,times(1)).io();
        verify(schedulerProvider,times(1)).ui();
        verifyNoMoreInteractions(schedulerProvider);
        reset(uModsRepositoryMock,schedulerProvider);
    }


    @Test
    public void Given_UModInAPModeAndUserAdminAndUpdateSuccess_When_SetWifiIsRequired_Then_ResultIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setState(UMod.State.AP_MODE);
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        SetWiFiRPC.Result wifiResult = new SetWiFiRPC.Result("Success");
        when(uModsRepositoryMock.setWiFiAP(any(UMod.class),any(SetWiFiRPC.Arguments.class)))
                .thenReturn(Observable.just(wifiResult));
        UpdateWiFiCredentials.RequestValues requestValues= new UpdateWiFiCredentials.RequestValues(uMod.getUUID(),"Urbit","1234");

        //When
        AssertableSubscriber <UpdateWiFiCredentials.ResponseValues >responseValues  = this.updateWiFiCredentials.buildUseCase(requestValues).test();

        //Then
        responseValues.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(1)).setWiFiAP(any(UMod.class),any(SetWiFiRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).saveUMod(uMod);
        assertEquals(requestValues.getmWiFiSSID(),uMod.getWifiSSID());

    }

    @Test
    public void Given_USerIsNotAdmin_When_UpdateIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setState(UMod.State.AP_MODE);
        uMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        UpdateWiFiCredentials.RequestValues requestValues= new UpdateWiFiCredentials.RequestValues(uMod.getUUID(),"Urbit","1234");

        //When
        AssertableSubscriber <UpdateWiFiCredentials.ResponseValues >responseValues  = this.updateWiFiCredentials.buildUseCase(requestValues).test();

        //Then
        responseValues.assertError(UpdateWiFiCredentials.NotAdminUserOrNotAPModeUModException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());

        verify(uModsRepositoryMock,times(1)).cachedFirst();
    }

    @Test
    public void Given_UserIsAdminButUpdateFailsWithHttp_When_UpdateIsRequired_Then_ErrorIsEmitted(){
        //TODO ver si es necesario explicitar las condiciones para entrar a pedir el update credentials
        //Given
        UMod uMod = new UMod("777");
        uMod.setState(UMod.State.AP_MODE);
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        HttpException wifiResult = new HttpException(
                Response.error(
                        418,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 418: I'm teapot")
                )
        );
        when(uModsRepositoryMock.setWiFiAP(any(UMod.class),any(SetWiFiRPC.Arguments.class)))
                .thenReturn(Observable.error(wifiResult));
        UpdateWiFiCredentials.RequestValues requestValues= new UpdateWiFiCredentials.RequestValues(uMod.getUUID(),"Urbit","1234");

        //When
        AssertableSubscriber <UpdateWiFiCredentials.ResponseValues >responseValues  = this.updateWiFiCredentials.buildUseCase(requestValues).test();

        //Then
        responseValues.assertError(HttpException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(1)).setWiFiAP(any(UMod.class),any(SetWiFiRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).cachedFirst();
    }

    @Test
    public void Given_UpdateFailsBecauseTimeOutThenSuccess_When_UpdateCredentialIsRequired_Then_ResultIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setState(UMod.State.AP_MODE);
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        SetWiFiRPC.Result wifiResult = new SetWiFiRPC.Result("Success");
        when(uModsRepositoryMock.setWiFiAP(any(UMod.class),any(SetWiFiRPC.Arguments.class)))
                .thenReturn(Observable.error(new IOException()))
                .thenReturn(Observable.just(wifiResult));
        UpdateWiFiCredentials.RequestValues requestValues= new UpdateWiFiCredentials.RequestValues(uMod.getUUID(),"Urbit","1234");

        //When
        AssertableSubscriber <UpdateWiFiCredentials.ResponseValues >responseValues  = this.updateWiFiCredentials.buildUseCase(requestValues).test();

        //Then
        responseValues.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(2)).setWiFiAP(any(UMod.class),any(SetWiFiRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).saveUMod(uMod);
        assertEquals(requestValues.getmWiFiSSID(),uMod.getWifiSSID());

    }

    @Test
    public void Given_UpdateFailsTwiceBecauseTimeOut_When_UpdateCredentialsIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setState(UMod.State.AP_MODE);
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        when(uModsRepositoryMock.setWiFiAP(any(UMod.class),any(SetWiFiRPC.Arguments.class)))
                .thenReturn(Observable.error(new IOException()));
        UpdateWiFiCredentials.RequestValues requestValues= new UpdateWiFiCredentials.RequestValues(uMod.getUUID(),"Urbit","1234");

        //When
        AssertableSubscriber <UpdateWiFiCredentials.ResponseValues >responseValues  = this.updateWiFiCredentials.buildUseCase(requestValues).test();

        //Then
        responseValues.assertError(IOException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(2)).setWiFiAP(any(UMod.class),any(SetWiFiRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).refreshUMods();
    }
}