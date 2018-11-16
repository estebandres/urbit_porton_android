package com.urbit_iot.porton.umods.domain.usecase;

import android.util.Log;

import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.appuser.domain.AppUser;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.APIUserType;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.umods.UModsFilterType;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;
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

import io.reactivex.internal.operators.observable.ObservableJust;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.observers.AssertableSubscriber;
import rx.schedulers.Schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;
import java.io.IOException;
import android.util.Log;

public class GetUModsOneByOneTest {

    private GetUModsOneByOne getUModsOneByOne;

    @Mock
    private UModsRepository uModsRepositoryMock;

    @Mock
    private AppUserRepository appUserRepositoryMock;

    @Spy
    private SchedulerProvider baseSchedulerProviderSpy;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Before
    public void setUp() throws Exception {
        System.out.print(baseSchedulerProviderSpy);
        Mockito.doReturn(Schedulers.immediate()).when(baseSchedulerProviderSpy).ui();
        this.getUModsOneByOne = new GetUModsOneByOne(uModsRepositoryMock,appUserRepositoryMock, baseSchedulerProviderSpy);


    }
    /*
    Cuando en el nombre de la funcion el modulo dice fresh es porque se llama a refrescar los mmodulos, caso contrario
    (stored) usa solo los de la cache o ddbb
     */

    @Test
    public void Given_StoredUmodFoundAndUserIsAuthorized_WhenGet1x1isRequest_ThenUModIsEmitted(){
        /*
        Also includes  umods that has not been in APMODE and found by LAN or MQTT (forces update)
        And UMods with no pendind request
         */
        //Given
        UMod cacheUmod = new UMod("777");
        cacheUmod.setuModSource(UMod.UModSource.CACHE);
        cacheUmod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        cacheUmod.setState(UMod.State.STATION_MODE);

        when(uModsRepositoryMock.getUModsOneByOne())
                .thenReturn(Observable.just(cacheUmod));
        AppUser appUser = new AppUser("123456789","1234","321");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        UModsFilterType filtro = UModsFilterType.ALL_UMODS;
        GetUModsOneByOne.RequestValues request = new GetUModsOneByOne.RequestValues(false,filtro);

        //When
        AssertableSubscriber<GetUModsOneByOne.ResponseValues> responseSub= this.getUModsOneByOne.buildUseCase(request).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(2)).getUModsOneByOne();
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,never()).refreshUMods();//Since the completable that refreshes the repo is never subscribed...
        verifyNoMoreInteractions(uModsRepositoryMock);
        verify(baseSchedulerProviderSpy,times(1)).io();
        verify(baseSchedulerProviderSpy,times(1)).ui();
        verifyNoMoreInteractions(baseSchedulerProviderSpy);
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(appUserRepositoryMock);

    }

    @Test
    public void Given_UnauthorizedStoredUModFound_When_Get1x1IsRequested_ThenUModIsNOTEmmitted(){
        //Given
        UMod cacheUmod = new UMod("777");
        cacheUmod.setuModSource(UMod.UModSource.CACHE);
        cacheUmod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
        when(uModsRepositoryMock.getUModsOneByOne())
                .thenReturn(Observable.just(cacheUmod));
        AppUser appUser = new AppUser("123456789","1234","321");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        UModsFilterType filtro = UModsFilterType.ALL_UMODS;
        GetUModsOneByOne.RequestValues request = new GetUModsOneByOne.RequestValues(false,filtro);

        //When
        AssertableSubscriber<GetUModsOneByOne.ResponseValues> responseSub= this.getUModsOneByOne.buildUseCase(request).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(2)).getUModsOneByOne();
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,never()).refreshUMods();//Since the completable that refreshes the repo is never subscribed...
        verifyNoMoreInteractions(uModsRepositoryMock);
        verify(baseSchedulerProviderSpy,times(1)).io();
        verify(baseSchedulerProviderSpy,times(1)).ui();
        verifyNoMoreInteractions(baseSchedulerProviderSpy);
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(appUserRepositoryMock);
    }

    @Test
    public void Given_FreshPendindUModFoundAndLevelUpdateSucces_WhenGet1x1isRequest_ThenUModIsEmittedAndSave(){
        //Given
        UMod lanUmod = new UMod("555");
        lanUmod.setuModSource(UMod.UModSource.LAN_SCAN);
        lanUmod.setAppUserLevel(UModUser.Level.PENDING);
        lanUmod.setInAPMode(false);
        when(uModsRepositoryMock.getUModsOneByOne())
                .thenReturn(Observable.empty())
                .thenReturn(Observable.just(lanUmod));
        GetUserLevelRPC.Result result = new GetUserLevelRPC.Result("alexyh", APIUserType.Admin);
        when(uModsRepositoryMock.getUserLevel(any(UMod.class), any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.just(result));
        AppUser appUser = new AppUser("123456789","1234","321");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        UModsFilterType filtro = UModsFilterType.ALL_UMODS;
        GetUModsOneByOne.RequestValues request = new GetUModsOneByOne.RequestValues(true,filtro);

        //When
        AssertableSubscriber<GetUModsOneByOne.ResponseValues> responseSub= this.getUModsOneByOne.buildUseCase(request).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(2)).getUModsOneByOne();
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).saveUMod(lanUmod);
        verifyNoMoreInteractions(uModsRepositoryMock);
        verify(baseSchedulerProviderSpy,times(1)).io();
        verify(baseSchedulerProviderSpy,times(1)).ui();
        verifyNoMoreInteractions(baseSchedulerProviderSpy);
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(appUserRepositoryMock);
        assertEquals(UModUser.Level.ADMINISTRATOR,lanUmod.getAppUserLevel());
    }

    @Test
    public void Given_FreshPendingUModFoundAndLevelUpdateFailsWithNoHttp_WhenGet1x1isRequest_ThenUModIsEmitted(){
        //Given
        UMod lanUmod = new UMod("555");
        lanUmod.setuModSource(UMod.UModSource.LAN_SCAN);
        lanUmod.setAppUserLevel(UModUser.Level.PENDING);
        lanUmod.setInAPMode(false);
        when(uModsRepositoryMock.getUModsOneByOne())
                .thenReturn(Observable.empty())
                .thenReturn(Observable.just(lanUmod));
        IOException ioException = new IOException("First IO Exception");
        when(uModsRepositoryMock.getUserLevel(any(UMod.class), any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(ioException));
        AppUser appUser = new AppUser("123456789","1234","321");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        UModsFilterType filtro = UModsFilterType.ALL_UMODS;
        GetUModsOneByOne.RequestValues request = new GetUModsOneByOne.RequestValues(true,filtro);

        //When
        AssertableSubscriber<GetUModsOneByOne.ResponseValues> responseSub= this.getUModsOneByOne.buildUseCase(request).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(2)).getUModsOneByOne();
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock,never()).saveUMod(lanUmod);
        verifyNoMoreInteractions(uModsRepositoryMock);
        verify(baseSchedulerProviderSpy,times(1)).io();
        verify(baseSchedulerProviderSpy,times(1)).ui();
        verifyNoMoreInteractions(baseSchedulerProviderSpy);
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(appUserRepositoryMock);
        assertEquals(UModUser.Level.PENDING,lanUmod.getAppUserLevel());
    }

    @Test
    public void Given_FreshPendindUModFoundAndLevelUpdateFailsWithHttpButNo500_WhenGet1x1isRequest_ThenUModIsEmitted(){
        //Given
        UMod lanUmod = new UMod("555");
        lanUmod.setuModSource(UMod.UModSource.LAN_SCAN);
        lanUmod.setAppUserLevel(UModUser.Level.PENDING);
        lanUmod.setInAPMode(false);
        when(uModsRepositoryMock.getUModsOneByOne())
                .thenReturn(Observable.empty())
                .thenReturn(Observable.just(lanUmod));
        HttpException httpException = new HttpException(
                Response.error(
                        400,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 400")
                )
        );
        when(uModsRepositoryMock.getUserLevel(any(UMod.class), any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(httpException));
        AppUser appUser = new AppUser("123456789","1234","321");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        UModsFilterType filtro = UModsFilterType.ALL_UMODS;
        GetUModsOneByOne.RequestValues request = new GetUModsOneByOne.RequestValues(true,filtro);

        //When
        AssertableSubscriber<GetUModsOneByOne.ResponseValues> responseSub= this.getUModsOneByOne.buildUseCase(request).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(2)).getUModsOneByOne();
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock,never()).saveUMod(lanUmod);
        verifyNoMoreInteractions(uModsRepositoryMock);
        verify(baseSchedulerProviderSpy,times(1)).io();
        verify(baseSchedulerProviderSpy,times(1)).ui();
        verifyNoMoreInteractions(baseSchedulerProviderSpy);
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(appUserRepositoryMock);
        assertEquals(UModUser.Level.PENDING,lanUmod.getAppUserLevel());

    }

    @Test
    public void Given_FreshPendindUModFoundAndLevelUpdateFailsWithHttp404_WhenGet1x1isRequest_ThenUModIsEmitted(){
        //Given
        UMod lanUmod = new UMod("555");
        lanUmod.setuModSource(UMod.UModSource.LAN_SCAN);
        lanUmod.setAppUserLevel(UModUser.Level.PENDING);
        lanUmod.setInAPMode(false);
        when(uModsRepositoryMock.getUModsOneByOne())
                .thenReturn(Observable.empty())
                .thenReturn(Observable.just(lanUmod));
        HttpException httpException = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 404")
                )
        );
        when(uModsRepositoryMock.getUserLevel(any(UMod.class), any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(httpException));
        AppUser appUser = new AppUser("123456789","1234","321");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        UModsFilterType filtro = UModsFilterType.ALL_UMODS;
        GetUModsOneByOne.RequestValues request = new GetUModsOneByOne.RequestValues(true,filtro);

        //When
        AssertableSubscriber<GetUModsOneByOne.ResponseValues> responseSub= this.getUModsOneByOne.buildUseCase(request).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(2)).getUModsOneByOne();
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock,never()).saveUMod(lanUmod);
        verifyNoMoreInteractions(uModsRepositoryMock);
        verify(baseSchedulerProviderSpy,times(1)).io();
        verify(baseSchedulerProviderSpy,times(1)).ui();
        verifyNoMoreInteractions(baseSchedulerProviderSpy);
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(appUserRepositoryMock);
        assertEquals(UModUser.Level.UNAUTHORIZED,lanUmod.getAppUserLevel());
    }
}