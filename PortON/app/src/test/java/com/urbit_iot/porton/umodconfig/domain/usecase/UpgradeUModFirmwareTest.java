package com.urbit_iot.porton.umodconfig.domain.usecase;

import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.EnableUpdateRPC;
import com.urbit_iot.porton.data.rpc.OTACommitRPC;
import com.urbit_iot.porton.data.rpc.SysGetInfoRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.SchedulerProvider;

import java.io.File;
import java.util.concurrent.TimeUnit;


import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Flowable;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
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

import rx.schedulers.TestScheduler;

import static java.lang.Thread.sleep;
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


public class UpgradeUModFirmwareTest {
    private UpgradeUModFirmware  upgradeUModFirmware;

    @Mock
    private UModsRepository uModsRepositoryMock;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Spy
    private SchedulerProvider schedulerProvider;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(Schedulers.immediate()).when(schedulerProvider).ui();
        this.upgradeUModFirmware = new UpgradeUModFirmware(uModsRepositoryMock,schedulerProvider);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(uModsRepositoryMock);
        verify(schedulerProvider,times(1)).io();
        verify(schedulerProvider,times(1)).ui();
        verifyNoMoreInteractions(schedulerProvider);
        reset(schedulerProvider, uModsRepositoryMock);
    }

    @Test
    public void Given_UserIsNotAdmin_When_UpgradeIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        UpgradeUModFirmware.RequestValues requestValues = new UpgradeUModFirmware.RequestValues(uMod.getUUID());

        //When
        AssertableSubscriber <UpgradeUModFirmware.ResponseValues >responseValues  = this.upgradeUModFirmware.buildUseCase(requestValues).test();

        //Then
        responseValues.assertError(UpgradeUModFirmware.UserIsNotAdminException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());

        verify(uModsRepositoryMock,times(1)).refreshUMods();
    }

    @Test
    public void Given_UserIsAdminButSWVersionIsNotValid_When_UpgradeIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        uMod.setSWVersion("");
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        File file = new File("/user/lastest_update.zip");
        when(uModsRepositoryMock.getFirmwareImageFile(uMod))
                .thenReturn(Observable.just(file));
        UpgradeUModFirmware.RequestValues requestValues = new UpgradeUModFirmware.RequestValues(uMod.getUUID());

        //When
        AssertableSubscriber <UpgradeUModFirmware.ResponseValues >responseValues  = this.upgradeUModFirmware.buildUseCase(requestValues).test();

        //Then
        responseValues.assertError(UpgradeUModFirmware.UModSwVersionUnavailable.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(1)).getFirmwareImageFile(uMod);

        verify(uModsRepositoryMock,times(1)).refreshUMods();
    }

    @Test
    public void Given_UserIsAdminButUpgradeIsNotNecessary_When_UpgradeIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        uMod.setSWVersion("1.7");
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        File file = new File("/user/alexa/version1.7");
        when(uModsRepositoryMock.getFirmwareImageFile(uMod))
                .thenReturn(Observable.just(file));
        UpgradeUModFirmware.RequestValues requestValues = new UpgradeUModFirmware.RequestValues(uMod.getUUID());

        //When
        AssertableSubscriber <UpgradeUModFirmware.ResponseValues >responseValues  = this.upgradeUModFirmware.buildUseCase(requestValues).test();

        //Then
        responseValues.assertError(UpgradeUModFirmware.UpgradeIsntNeccessary.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(1)).getFirmwareImageFile(uMod);

        verify(uModsRepositoryMock,times(1)).refreshUMods();
    }

    @Test
    public void Given_UserIsAdmitButPostResponseInformsError_When_UpgradeIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        uMod.setSWVersion("1.7");
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        File file = new File("/user/alexa/version1.8");
        when(uModsRepositoryMock.getFirmwareImageFile(uMod))
                .thenReturn(Observable.just(file));
        UpgradeUModFirmware.RequestValues requestValues = new UpgradeUModFirmware.RequestValues(uMod.getUUID());
        EnableUpdateRPC.Result enableResult = new EnableUpdateRPC.Result("Success");
        when(uModsRepositoryMock.enableUModUpdate(any(UMod.class),any(EnableUpdateRPC.Arguments.class)))
                .thenReturn(Observable.just(enableResult));
        ResponseBody body = ResponseBody.create(MediaType.parse("text/plain"),"actualizacion");
        Response<ResponseBody> response = Response.error(401,body);
        when(uModsRepositoryMock.postFirmwareUpdateToUMod(uMod,file))
                .thenReturn(Observable.just(response));

        Observable<UpgradeUModFirmware.ResponseValues> responsevalues = this.upgradeUModFirmware.buildUseCase(requestValues);
        Flowable<UpgradeUModFirmware.ResponseValues> testFlowable = RxJavaInterop.toV2Flowable(responsevalues);

        TestScheduler testScheduler = new TestScheduler();
        Mockito.doReturn(testScheduler).when(schedulerProvider).computation();

        //When
        io.reactivex.subscribers.TestSubscriber tester = testFlowable.test();
        //AssertableSubscriber <UpgradeUModFirmware.ResponseValues >responseValues  = this.upgradeUModFirmware.buildUseCase(requestValues).test();


        //Then
        testScheduler.advanceTimeBy(500L,TimeUnit.MILLISECONDS);//Delay before post
        tester.assertError(UpgradeUModFirmware.PostFirmwareFailException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(1)).getFirmwareImageFile(uMod);
        verify(uModsRepositoryMock,times(1)).enableUModUpdate(any(UMod.class),any(EnableUpdateRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).postFirmwareUpdateToUMod(uMod,file);

        //responseValues.assertError(Exception.class)
        //      .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).refreshUMods();
    }

    @Test
    public void
    Given_UserIsAdminAndUModInformsFileWasTransferredOKAndUModKeepThatUpgradedVersion_When_UpgradeIsRequired_Then_ResultIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        uMod.setSWVersion("1.7");
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        File file = new File("/user/alexa/version1.8");
        when(uModsRepositoryMock.getFirmwareImageFile(uMod))
                .thenReturn(Observable.just(file));
        UpgradeUModFirmware.RequestValues requestValues = new UpgradeUModFirmware.RequestValues(uMod.getUUID());
        EnableUpdateRPC.Result enableResult = new EnableUpdateRPC.Result("Success");
        when(uModsRepositoryMock.enableUModUpdate(any(UMod.class),any(EnableUpdateRPC.Arguments.class)))
                .thenReturn(Observable.just(enableResult));
        ResponseBody body = ResponseBody.create(MediaType.parse("text/plain"),"actualizacion");
        Response<ResponseBody> response = Response.success(body);
        when(uModsRepositoryMock.postFirmwareUpdateToUMod(uMod,file))
                .thenReturn(Observable.just(response));
        SysGetInfoRPC.Result sysInfoResult = new SysGetInfoRPC.Result(null,"1.8",null,null,null,null,
                0,0,0,0,0,null,null);
        when(uModsRepositoryMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.just(sysInfoResult));
        ResponseBody otaBody = ResponseBody.create(MediaType.parse("text/plain"),"success");
        Response<ResponseBody> otaResponse = Response.success(otaBody);
        when(uModsRepositoryMock.otaCommit(any(UMod.class),any(OTACommitRPC.Arguments.class)))
                .thenReturn(Observable.just(otaResponse));

        TestScheduler testScheduler = new TestScheduler();
        Mockito.doReturn(testScheduler).when(schedulerProvider).computation();

        //When
        AssertableSubscriber <UpgradeUModFirmware.ResponseValues >responseValues  = this.upgradeUModFirmware.buildUseCase(requestValues).test();

        //Then
        testScheduler.advanceTimeBy(500L,TimeUnit.MILLISECONDS);//Delay before post
        testScheduler.advanceTimeBy(25L,TimeUnit.SECONDS);
        responseValues.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(1)).getFirmwareImageFile(uMod);
        verify(uModsRepositoryMock,times(1)).enableUModUpdate(any(UMod.class),any(EnableUpdateRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).postFirmwareUpdateToUMod(uMod,file);
        verify(uModsRepositoryMock,times(1)).getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).otaCommit(any(UMod.class),any(OTACommitRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).refreshUMods();
        assertEquals(sysInfoResult.getFwVersion(),uMod.getSWVersion());
    }

    @Test
    public void Given_UserIsAdminAndUModInformsFileHasTransferredButUModGetBackPreviousVersion_When_UpgradeIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        uMod.setSWVersion("1.7");
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        File file = new File("/user/alexa/version1.8");
        when(uModsRepositoryMock.getFirmwareImageFile(uMod))
                .thenReturn(Observable.just(file));
        UpgradeUModFirmware.RequestValues requestValues = new UpgradeUModFirmware.RequestValues(uMod.getUUID());
        EnableUpdateRPC.Result enableResult = new EnableUpdateRPC.Result("Success");
        when(uModsRepositoryMock.enableUModUpdate(any(UMod.class),any(EnableUpdateRPC.Arguments.class)))
                .thenReturn(Observable.just(enableResult));
        ResponseBody body = ResponseBody.create(MediaType.parse("text/plain"),"actualizacion");
        Response<ResponseBody> response = Response.success(body);
        when(uModsRepositoryMock.postFirmwareUpdateToUMod(uMod,file))
                .thenReturn(Observable.just(response));
        SysGetInfoRPC.Result sysInfoResult = new SysGetInfoRPC.Result(null,"1.8",null,null,null,null,
                0,0,0,0,0,null,null);
        when(uModsRepositoryMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.just(sysInfoResult));
        ResponseBody otaBody = ResponseBody.create(MediaType.parse("text/plain"),"success");
        Response<ResponseBody> otaResponse = Response.error(404,otaBody);
        when(uModsRepositoryMock.otaCommit(any(UMod.class),any(OTACommitRPC.Arguments.class)))
                .thenReturn(Observable.just(otaResponse));

        TestScheduler testScheduler = new TestScheduler();
        Mockito.doReturn(testScheduler).when(schedulerProvider).computation();

        Observable<UpgradeUModFirmware.ResponseValues> responsevalues = this.upgradeUModFirmware.buildUseCase(requestValues);
        Flowable<UpgradeUModFirmware.ResponseValues> testFlowable = RxJavaInterop.toV2Flowable(responsevalues);

        //When
        io.reactivex.subscribers.TestSubscriber tester = testFlowable.test();
        //AssertableSubscriber <UpgradeUModFirmware.ResponseValues >responseValues  = this.upgradeUModFirmware.buildUseCase(requestValues).test();

        //Then
        testScheduler.advanceTimeBy(500L,TimeUnit.MILLISECONDS);//Delay before post
        testScheduler.advanceTimeBy(25L,TimeUnit.SECONDS);
        tester.assertError(UpgradeUModFirmware.OtaCommitException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(1)).getFirmwareImageFile(uMod);
        verify(uModsRepositoryMock,times(1)).enableUModUpdate(any(UMod.class),any(EnableUpdateRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).postFirmwareUpdateToUMod(uMod,file);
        verify(uModsRepositoryMock,times(1)).getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).otaCommit(any(UMod.class),any(OTACommitRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).refreshUMods();
        assertEquals(sysInfoResult.getFwVersion(),uMod.getSWVersion());
    }

    @Test
    public void
    Given_UserIsAdminAndPostSuccessfulButUModInformsNoUpgradeHasBeenMade_When_UpgradeIsRequired_Then_ErrorIsEmitted(){
        /*
        el modulo informa que ninguna actualizacion ha sido hecha porque el resultado
         software version de sysInfo es la misma a la que tenia antes el modulo
         */
        //Given
        UMod uMod = new UMod("777");
        uMod.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        uMod.setSWVersion("1.7");
        when(uModsRepositoryMock.getUMod(uMod.getUUID()))
                .thenReturn(Observable.just(uMod));
        File file = new File("/user/alexa/version1.8");
        when(uModsRepositoryMock.getFirmwareImageFile(uMod))
                .thenReturn(Observable.just(file));
        UpgradeUModFirmware.RequestValues requestValues = new UpgradeUModFirmware.RequestValues(uMod.getUUID());
        EnableUpdateRPC.Result enableResult = new EnableUpdateRPC.Result("Success");
        when(uModsRepositoryMock.enableUModUpdate(any(UMod.class),any(EnableUpdateRPC.Arguments.class)))
                .thenReturn(Observable.just(enableResult));
        ResponseBody body = ResponseBody.create(MediaType.parse("text/plain"),"actualizacion");
        Response<ResponseBody> response = Response.success(body);
        when(uModsRepositoryMock.postFirmwareUpdateToUMod(uMod,file))
                .thenReturn(Observable.just(response));
        SysGetInfoRPC.Result sysInfoResult = new SysGetInfoRPC.Result(null,"1.7",null,null,null,null,
                0,0,0,0,0,null,null);
        when(uModsRepositoryMock.getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class)))
                .thenReturn(Observable.just(sysInfoResult));

        TestScheduler testScheduler = new TestScheduler();
        Mockito.doReturn(testScheduler).when(schedulerProvider).computation();

        Observable<UpgradeUModFirmware.ResponseValues> responsevalues = this.upgradeUModFirmware.buildUseCase(requestValues);
        Flowable<UpgradeUModFirmware.ResponseValues> testFlowable = RxJavaInterop.toV2Flowable(responsevalues);

        //When
        io.reactivex.subscribers.TestSubscriber tester = testFlowable.test();
        //AssertableSubscriber <UpgradeUModFirmware.ResponseValues >responseValues  = this.upgradeUModFirmware.buildUseCase(requestValues).test();

        //Then
        testScheduler.advanceTimeBy(500L,TimeUnit.MILLISECONDS);//Delay before post
        testScheduler.advanceTimeBy(25L,TimeUnit.SECONDS);
        tester.assertError(UpgradeUModFirmware.InconsistentVersionAfterUpgradeException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(1)).getFirmwareImageFile(uMod);
        verify(uModsRepositoryMock,times(1)).enableUModUpdate(any(UMod.class),any(EnableUpdateRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).postFirmwareUpdateToUMod(uMod,file);
        verify(uModsRepositoryMock,times(1)).getSystemInfo(any(UMod.class),any(SysGetInfoRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).refreshUMods();
    }
    
}