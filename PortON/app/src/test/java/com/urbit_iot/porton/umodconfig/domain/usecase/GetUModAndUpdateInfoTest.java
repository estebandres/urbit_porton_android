package com.urbit_iot.porton.umodconfig.domain.usecase;



import android.location.Location;

import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.appuser.domain.AppUser;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.APIUserType;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.SysGetInfoRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.GlobalConstants;
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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Flowable;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.adapter.rxjava.HttpException;
import retrofit2.Response;
import retrofit2.http.HTTP;
import rx.Observable;
import rx.Subscriber;
import rx.observers.AssertableSubscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class GetUModAndUpdateInfoTest {
    private GetUModAndUpdateInfo getUModAndUpdateInfo;

    @Mock
    UModsRepository uModsRepositoryMock;

    @Mock
    AppUserRepository appUserRepositoryMock;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Spy
    private SchedulerProvider schedulerProvider;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(Schedulers.immediate()).when(schedulerProvider).ui();
        this.getUModAndUpdateInfo = new GetUModAndUpdateInfo(uModsRepositoryMock,appUserRepositoryMock,schedulerProvider);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
        verify(schedulerProvider,times(1)).io();
        verify(schedulerProvider,times(1)).ui();
        verifyNoMoreInteractions(schedulerProvider);
        reset(uModsRepositoryMock,appUserRepositoryMock,schedulerProvider);
    }

    @Test
    public void Given_UModIsInAPModeAndInDifferentNetwork_When_UpdateIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setState(UMod.State.AP_MODE);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        GetUModAndUpdateInfo.RequestValues requestValues = new GetUModAndUpdateInfo.RequestValues(uMod.getUUID(),"Secyt");

        //When
        AssertableSubscriber<GetUModAndUpdateInfo.ResponseValues>  responseSub= this.getUModAndUpdateInfo.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(GetUModAndUpdateInfo.UnconnectedFromAPModeUModException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(2)).getUMod(uMod.getUUID());

        verify(uModsRepositoryMock,times(1)).cachedFirst();
    }

    @Test
    public void Given_UModIsInStationModeAndGetLevelAndSysInfoSuccess_When_UpdateIsRequired_Then_ResultIsEmitted(){
        //Given
        UMod uMod= new UMod("777");
        uMod.setState(UMod.State.STATION_MODE);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        AppUser appUser = new AppUser("123456","123456","987654");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        GetUserLevelRPC.Result levelResult = new GetUserLevelRPC.Result("Alexyh",APIUserType.User);
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.just(levelResult));
        SysGetInfoRPC.Result.Wifi wifi = new SysGetInfoRPC.Result.Wifi(null, null,null,"Secyt");
        SysGetInfoRPC.Result sysInfoResult = new SysGetInfoRPC.Result(null,"2.0","2.2","02:42:f1:cc:63:53",
                null,0,0,0,0,0,10,wifi,null);
        when(uModsRepositoryMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.just(sysInfoResult));
        GetUModAndUpdateInfo.RequestValues requestValues = new GetUModAndUpdateInfo.RequestValues(uMod.getUUID(),"Secyt");

        //When
        AssertableSubscriber<GetUModAndUpdateInfo.ResponseValues>  responseSub= this.getUModAndUpdateInfo.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(2)).getUMod(uMod.getUUID());
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class));

        verify(uModsRepositoryMock,times(2)).saveUMod(uMod);
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        assertEquals(levelResult.getUserLevel(),uMod.getAppUserLevel());
        assertEquals(sysInfoResult.getFwVersion(),uMod.getSWVersion());
        assertEquals(sysInfoResult.getWifi().getSsid(),uMod.getWifiSSID());
        assertEquals(sysInfoResult.getMac(),uMod.getMacAddress());
    }

    @Test
    public void Given_UModInAPModeAndInSameLANAndHasValidStationIp_When_UpdateIsRequired_Then_ResultIsEmitted(){
        /*
        Caso parecido al anterior pero con  AP Mode
         */
        //Given
        UMod uMod= new UMod("777");
        uMod.setState(UMod.State.AP_MODE);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        AppUser appUser = new AppUser("123456","123456","987654");
        when(appUserRepositoryMock.getAppUser())
                .thenReturn(Observable.just(appUser));
        GetUserLevelRPC.Result levelResult = new GetUserLevelRPC.Result("Alexyh",APIUserType.User);
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.just(levelResult));
        SysGetInfoRPC.Result.Wifi wifi = new SysGetInfoRPC.Result.Wifi("192.168.0.1", null,null,"777");
        SysGetInfoRPC.Result sysGetResult = new SysGetInfoRPC.Result(null,"2.0","2.2","02:42:f1:cc:63:53",
                null,0,0,0,0,0,10,wifi,null);
        when(uModsRepositoryMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.just(sysGetResult));
        GetUModAndUpdateInfo.RequestValues requestValues = new GetUModAndUpdateInfo.RequestValues(uMod.getUUID(),"Urbit_777");

        //When
        AssertableSubscriber<GetUModAndUpdateInfo.ResponseValues>  responseSub= this.getUModAndUpdateInfo.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(2)).getUMod(uMod.getUUID());
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class));

        verify(uModsRepositoryMock,times(2)).saveUMod(uMod);
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        assertEquals(sysGetResult.getFwVersion(),uMod.getSWVersion());
        assertEquals(sysGetResult.getWifi().getSsid(),uMod.getWifiSSID());
        assertEquals(sysGetResult.getMac(),uMod.getMacAddress());
        assertEquals(wifi.getStaIp(),uMod.getConnectionAddress());
    }

    @Test
    public void
    Given_UModInAPModeAndSameLANButGetSysInfoFailsFirstWithIOExceptionThenSuccess_When_UpdateIsRequired_Then_ResultIsEmitted(){
        //Given
        UMod uMod= new UMod("777");
        uMod.setState(UMod.State.AP_MODE);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        AppUser appUser = new AppUser("123456","123456","987654");
        when(appUserRepositoryMock.getAppUser())
                .thenReturn(Observable.just(appUser));
        GetUserLevelRPC.Result levelResult = new GetUserLevelRPC.Result("Alexyh",APIUserType.User);
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.just(levelResult));
        SysGetInfoRPC.Result.Wifi wifi = new SysGetInfoRPC.Result.Wifi("192.168.0.1", null,null,"777");
        SysGetInfoRPC.Result sysGetResult = new SysGetInfoRPC.Result(null,"2.0","2.2","02:42:f1:cc:63:53",
                null,0,0,0,0,0,10,wifi,null);
        when(uModsRepositoryMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.error(new IOException("time out")))
                .thenReturn(Observable.just(sysGetResult));
        GetUModAndUpdateInfo.RequestValues requestValues = new GetUModAndUpdateInfo.RequestValues(uMod.getUUID(),"Urbit_777");

        //When
        AssertableSubscriber<GetUModAndUpdateInfo.ResponseValues>  responseSub= this.getUModAndUpdateInfo.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(2)).getUMod(uMod.getUUID());
        verify(appUserRepositoryMock,times(2)).getAppUser();
        verify(uModsRepositoryMock,times(2)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock,times(2)).getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class));

        verify(uModsRepositoryMock,times(3)).saveUMod(uMod);
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        assertEquals(levelResult.getUserLevel(),uMod.getAppUserLevel());
        assertEquals(sysGetResult.getFwVersion(),uMod.getSWVersion());
        assertEquals(sysGetResult.getWifi().getSsid(),uMod.getWifiSSID());
        assertEquals(sysGetResult.getMac(),uMod.getMacAddress());
        assertEquals(wifi.getStaIp(),uMod.getConnectionAddress());
    }

    @Test
    public void Given_UModInAPModeAndSameLANButGetSysInfoFailsTwiceWithIOExc_When_UpdateIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod= new UMod("777");
        uMod.setState(UMod.State.AP_MODE);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        AppUser appUser = new AppUser("123456","123456","987654");
        when(appUserRepositoryMock.getAppUser())
                .thenReturn(Observable.just(appUser));
        GetUserLevelRPC.Result levelResult = new GetUserLevelRPC.Result("Alexyh",APIUserType.User);
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.just(levelResult));
        when(uModsRepositoryMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.error(new IOException("time out")));
        GetUModAndUpdateInfo.RequestValues requestValues = new GetUModAndUpdateInfo.RequestValues(uMod.getUUID(),"Urbit_777");

        //When
        AssertableSubscriber<GetUModAndUpdateInfo.ResponseValues>  responseSub= this.getUModAndUpdateInfo.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(IOException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(2)).getUMod(uMod.getUUID());
        verify(appUserRepositoryMock,times(2)).getAppUser();
        verify(uModsRepositoryMock,times(2)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock,times(2)).getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class));

        verify(uModsRepositoryMock,times(2)).saveUMod(uMod);
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        assertEquals(levelResult.getUserLevel(),uMod.getAppUserLevel());
    }

    @Test
    public void Given_UModInStationModeButGetUserLevelFirstFailsWithIOExcThenSuccess_When_UpdateIsRequired_Then_ResultIsEmitted(){
        //Given
        UMod uMod= new UMod("777");
        uMod.setState(UMod.State.STATION_MODE);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        AppUser appUser = new AppUser("123456","123456","987654");
        when(appUserRepositoryMock.getAppUser())
                .thenReturn(Observable.just(appUser));
        GetUserLevelRPC.Result levelResult = new GetUserLevelRPC.Result("Alexyh",APIUserType.User);
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(new IOException()))
                .thenReturn(Observable.just(levelResult));
        SysGetInfoRPC.Result.Wifi wifi = new SysGetInfoRPC.Result.Wifi(null, null,null,"Secyt");
        SysGetInfoRPC.Result sysGetResult = new SysGetInfoRPC.Result(null,"2.0","2.2","02:42:f1:cc:63:53",
                null,0,0,0,0,0,10,wifi,null);
        when(uModsRepositoryMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.just(sysGetResult));
        GetUModAndUpdateInfo.RequestValues requestValues = new GetUModAndUpdateInfo.RequestValues(uMod.getUUID(),"Secyt");

        //When
        AssertableSubscriber<GetUModAndUpdateInfo.ResponseValues>  responseSub= this.getUModAndUpdateInfo.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(2)).getUMod(uMod.getUUID());
        verify(appUserRepositoryMock,times(2)).getAppUser();
        verify(uModsRepositoryMock,times(2)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class));

        verify(uModsRepositoryMock,times(2)).saveUMod(uMod);
        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        assertEquals(levelResult.getUserLevel(),uMod.getAppUserLevel());
        assertEquals(sysGetResult.getFwVersion(),uMod.getSWVersion());
        assertEquals(sysGetResult.getWifi().getSsid(),uMod.getWifiSSID());
        assertEquals(sysGetResult.getMac(),uMod.getMacAddress());
    }

    @Test
    public void Given_UModInStationModeAndGetUserLevelFailsWithUnhandledHttpException_When_UpdateIsRequired_Then_ErrorIsEmitted(){
        /*
        TODO diferenciar entre unhandled y uncontrolled
        Casos en el que sea un código de error permitido pero no manejado
        y error no permitido
         */
        //Given
        UMod uMod= new UMod("777");
        uMod.setState(UMod.State.STATION_MODE);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        AppUser appUser = new AppUser("123456","123456","987654");
        when(appUserRepositoryMock.getAppUser())
                .thenReturn(Observable.just(appUser));
        HttpException userLevelException = new HttpException(
                Response.error(
                        418,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 418: I'm teapot")
                )
        );
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(userLevelException));
        GetUModAndUpdateInfo.RequestValues requestValues = new GetUModAndUpdateInfo.RequestValues(uMod.getUUID(),"Secyt");

        //When
        AssertableSubscriber<GetUModAndUpdateInfo.ResponseValues>  responseSub= this.getUModAndUpdateInfo.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(HttpException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(2)).getUMod(uMod.getUUID());
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).cachedFirst();
    }

    @Test
    public void Given_UModInStationModeAndGetLevelFailsBecauseUserWasDeleted_When_UpdateIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod= new UMod("777");
        uMod.setState(UMod.State.STATION_MODE);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        AppUser appUser = new AppUser("123456","123456","987654");
        when(appUserRepositoryMock.getAppUser())
                .thenReturn(Observable.just(appUser));
        HttpException userLevelException = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 404")
                )
        );
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(userLevelException));
        GetUModAndUpdateInfo.RequestValues requestValues = new GetUModAndUpdateInfo.RequestValues(uMod.getUUID(),"Secyt");

        //When
        AssertableSubscriber<GetUModAndUpdateInfo.ResponseValues>  responseSub= this.getUModAndUpdateInfo.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(GetUModAndUpdateInfo.DeletedUserException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(2)).getUMod(uMod.getUUID());
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).saveUMod(uMod);
        assertEquals(UModUser.Level.UNAUTHORIZED,uMod.getAppUserLevel());
    }

    @Test
    public void Given_UModInStationModeAndGetLevelFailsBecauseIncompatibleAPI_When_UpdateIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod= new UMod("777");
        uMod.setState(UMod.State.STATION_MODE);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        AppUser appUser = new AppUser("123456","123456","987654");
        when(appUserRepositoryMock.getAppUser())
                .thenReturn(Observable.just(appUser));
        HttpException userLevelException = new HttpException(
                Response.error(
                        400,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 400")
                )
        );
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(userLevelException));
        GetUModAndUpdateInfo.RequestValues requestValues = new GetUModAndUpdateInfo.RequestValues(uMod.getUUID(),"Secyt");

        //When
        AssertableSubscriber<GetUModAndUpdateInfo.ResponseValues>  responseSub= this.getUModAndUpdateInfo.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(GetUModAndUpdateInfo.IncompatibleAPIException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(2)).getUMod(uMod.getUUID());
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verify(uModsRepositoryMock,times(1)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).cachedFirst();
    }

    @Test
    public void
    Given_UModInFactoryResetAndFirstUserTryToCreateUserButFailsWithIOExcThenSuccess_When_UpdateIsRequired_Then_ResultIsEmitted(){
        //Given
        UMod uMod= new UMod("777");
        uMod.setState(UMod.State.AP_MODE);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        AppUser appUser = new AppUser("123456","123456","987654");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        HttpException userLevelException1 = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 404")
                )
        );
        HttpException userLevelException2 = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 404")
                )
        );
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(userLevelException1))
                .thenReturn(Observable.error(userLevelException2));
        CreateUserRPC.Result createResult = new CreateUserRPC.Result(APIUserType.Admin);
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class)))
                .thenReturn(Observable.error(new IOException("time out")))
                .thenReturn(Observable.just(createResult));
        SysGetInfoRPC.Result.Wifi wifi = new SysGetInfoRPC.Result.Wifi("192.168.0.1", null,null,"777");
        SysGetInfoRPC.Result sysGetResult = new SysGetInfoRPC.Result(null,"2.0","2.2","02:42:f1:cc:63:53",
                null,0,0,0,0,0,10,wifi,null);
        when(uModsRepositoryMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.just(sysGetResult));
        GetUModAndUpdateInfo.RequestValues requestValues = new GetUModAndUpdateInfo.RequestValues(uMod.getUUID(),"Urbit_777");

        //When
        AssertableSubscriber<GetUModAndUpdateInfo.ResponseValues>  responseSub= this.getUModAndUpdateInfo.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(2)).getUMod(uMod.getUUID());
        verify(appUserRepositoryMock,times(2)).getAppUser();
        verify(uModsRepositoryMock,times(2)).getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class));
        verify(uModsRepositoryMock,times(2)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).cachedFirst();
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(2)).saveUMod(uMod);
        assertEquals(UModUser.Level.ADMINISTRATOR,uMod.getAppUserLevel());
        assertEquals(sysGetResult.getFwVersion(),uMod.getSWVersion());
        assertEquals(sysGetResult.getWifi().getSsid(),uMod.getWifiSSID());
        assertEquals(sysGetResult.getMac(),uMod.getMacAddress());
        assertEquals(wifi.getStaIp(),uMod.getConnectionAddress());
        assertEquals(GlobalConstants.URBIT_PREFIX + uMod.getUUID()+"/response/"+appUser.getUserName(),uMod.getMqttResponseTopic());
    }
}