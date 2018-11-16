package com.urbit_iot.porton.umods.domain.usecase;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.appuser.domain.AppUser;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.APIUserType;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.TriggerRPC;
import com.urbit_iot.porton.data.source.PhoneConnectivity;
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

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Flowable;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.adapter.rxjava.HttpException;
import retrofit2.Response;
import retrofit2.http.HTTP;
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


public class RequestAccessTest {

    private RequestAccess requestAccessUC;

    @Mock
    private UModsRepository uModsRepositoryMock;

    @Mock
    private AppUserRepository appUserRepositoryMock;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Spy
    private SchedulerProvider schedulerProvider;


    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(Schedulers.immediate()).when(schedulerProvider).ui();
        this.requestAccessUC = new RequestAccess(uModsRepositoryMock, appUserRepositoryMock, schedulerProvider);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void Given_CreateUserIsSuccessful_When_AccessIsSolicited_Then_CreationResultIsEmitted(){
        //Given
        UMod umod = new UMod("777");
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        CreateUserRPC.Result createResult = new CreateUserRPC.Result(APIUserType.User);
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class))).
                thenReturn(Observable.just(createResult));
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                    .assertValueCount(1);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).saveUMod(umod);
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
        assertEquals(UModUser.Level.AUTHORIZED,umod.getAppUserLevel());
        String mqttResponse = GlobalConstants.URBIT_PREFIX + umod.getUUID() + "/response/" + appUser.getUserName();
        assertEquals(mqttResponse,umod.getMqttResponseTopic());

    }

    @Test
    public void Given_CreateUserFailsBecauseAlreadyExist_When_AccessIsSolicited_Then_UserLevelUpdatedAndCreationResultEmitted(){
        //Given
        HttpException httpException = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 409")
                )
        );
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class))).
                thenReturn(Observable.error(httpException));
        UMod umod = new UMod("777");
        umod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        GetUserLevelRPC.Result getUserLevelResult = new GetUserLevelRPC.Result("Alexyh",APIUserType.Admin);
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.just(getUserLevelResult));
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).saveUMod(umod);
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
        assertEquals(UModUser.Level.ADMINISTRATOR,umod.getAppUserLevel());
    }

    @Test
    public void
    Given_CreateUserFailsBecauseAlreadyExistButFailsGetLevelBecauseImmediatelyDeleted_When_AccessIsSolicited_Then_LevelUpdatedAndErrorEmitted(){
        //Given
        HttpException httpExceptionCreateUser = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 409")
                )
        );

        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class))).
                thenReturn(Observable.error(httpExceptionCreateUser));
        UMod umod = new UMod("777");
        umod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        HttpException httpExceptionUserLevel = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 404")
                )
        );
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(httpExceptionUserLevel));
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(HttpException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).saveUMod(umod);
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
        assertEquals(UModUser.Level.UNAUTHORIZED,umod.getAppUserLevel());
    }

    @Test
    public void Given_CreateUserFailsBecauseUModDidNotWriteButSecondTrySuccessful_WhenAccessIsSolicited_Then_CreationResultEmitted(){
        //Given
        HttpException httpExceptionCreateUser = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 500")
                )
        );
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        CreateUserRPC.Result createResult = new CreateUserRPC.Result(APIUserType.Admin);

        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class)))
                .thenReturn(Observable.error(httpExceptionCreateUser))
                .thenReturn(Observable.just(createResult));
        UMod umod = new UMod("777");
        umod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));

        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(2)).refreshUMods();
        verify(uModsRepositoryMock,times(2)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(appUserRepositoryMock,times(2)).getAppUser();
        verify(uModsRepositoryMock,times(1)).saveUMod(umod);
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
        assertEquals(UModUser.Level.ADMINISTRATOR,umod.getAppUserLevel());
        String mqttResponse = GlobalConstants.URBIT_PREFIX + umod.getUUID() + "/response/" + appUser.getUserName();
        assertEquals(mqttResponse,umod.getMqttResponseTopic());
    }

    @Test
    public void Given_CreateUserFailsBecauseMaxCapacityIsReached_WhenAccessIsSolicited_Then_ErrorIsEmitted(){
        //Given
        HttpException httpExceptionCreateUser = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 412")
                )
        );
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class)))
                .thenReturn(Observable.error(httpExceptionCreateUser));
        UMod umod = new UMod("777");
        umod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        io.reactivex.subscribers.TestSubscriber<RequestAccess.ResponseValues> responseSub = RxJavaInterop.toV2Flowable(this.requestAccessUC.buildUseCase(requestValues)).test();

        //Then
        responseSub.assertError(RequestAccess.MaxUModUsersQuantityReachedException.class)
                .assertValueCount(0)
        .assertError(throwable -> ((RequestAccess.MaxUModUsersQuantityReachedException)throwable).getUModUUID().equals(umod.getUUID()));
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
    }

    @Test
    public void Given_CreateUserFailsBecauseFirsrTimeOutButThenIsSuccessful_When_AccessIsSolicited_Then_CreationResultIsEmitted(){
        //Given
        IOException ioException = new IOException("");
        CreateUserRPC.Result createResult = new CreateUserRPC.Result(APIUserType.Admin);
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class)))
                .thenReturn(Observable.error(ioException))
                .thenReturn(Observable.just(createResult));
        UMod umod = new UMod("777");
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        umod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(2)).refreshUMods();
        verify(uModsRepositoryMock,times(2)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).saveUMod(umod);
        verify(appUserRepositoryMock,times(2)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
        assertEquals(UModUser.Level.ADMINISTRATOR,umod.getAppUserLevel());
        String mqttResponse = GlobalConstants.URBIT_PREFIX + umod.getUUID() + "/response/" + appUser.getUserName();
        assertEquals(mqttResponse,umod.getMqttResponseTopic());
    }

    @Test
    public void Given_CreateUserFailsTwiceBecauseTimeOut_When_AccessIsSolicited_Then_ErrorIsEmitted(){
        //Given
        IOException ioException = new IOException("");
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class)))
                .thenReturn(Observable.error(ioException));
        UMod umod = new UMod("777");
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        umod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(IOException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(2)).refreshUMods();
        verify(uModsRepositoryMock,times(2)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(appUserRepositoryMock,times(2)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
    }

    @Test
    public void Given_CreateUserFailsTwiceBecauseCouldnotWrite_When_AccessIsSolicited_Then_ErrorIsEmitted(){
        //Given
        HttpException httpExceptionCreateUser1 = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 500")
                )
        );
        HttpException httpExceptionCreateUser2 = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 500")
                )
        );
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class)))
                .thenReturn(Observable.error(httpExceptionCreateUser1))
                .thenReturn(Observable.error(httpExceptionCreateUser2));
        UMod umod = new UMod("777");
        umod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(RequestAccess.FailedToWriteUserFilesException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(2)).refreshUMods();
        verify(uModsRepositoryMock,times(2)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(appUserRepositoryMock,times(2)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
    }

    @Test
    public void Given_CreateUserFailsWithUnconsideredHttpException_WhenAccessIsSolicited_Then_ErrorIsEmitted(){
        //Given
        HttpException notConsideredHttp = new HttpException(
                Response.error(
                        501,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 501")
                )
        );
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class)))
                .thenReturn(Observable.error(notConsideredHttp));
        UMod umod = new UMod("777");
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(HttpException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
    }

    @Test
    public void
    Given_CreateUserFailsBecauseAlreadyExistButGetLevelFailsWithUnconsideredHttp_When_AccessIsSolicited_Then_ErrorEmitted(){
        //Given
        HttpException httpExceptionCreateUser = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 409")
                )
        );
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class))).
                thenReturn(Observable.error(httpExceptionCreateUser));
        UMod umod = new UMod("777");
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        HttpException httpExceptionUserLevel = new HttpException(
                Response.error(
                        501,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 501")
                )
        );
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(httpExceptionUserLevel));
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(HttpException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(appUserRepositoryMock,times(1)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
    }

    @Test
    public void
    Given_CreateUserFailsBecauseAlreadyExistAndGetLevelFirstFailsBecauseTimeOutThenBecauseUserWasDeleted_When_AccessIsSolicited_Then_ErrorIsEmitted(){
        //Given
        HttpException httpExceptionCreateUser1 = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 409")
                )
        );
        HttpException httpExceptionCreateUser2 = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 409")
                )
        );
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class))).
                thenReturn(Observable.error(httpExceptionCreateUser1))
                .thenReturn(Observable.error(httpExceptionCreateUser2));
        UMod umod = new UMod("777");
        umod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        IOException IOExceptionUserLevel = new IOException("laralara");
        HttpException httpExceptionUserLevel = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 404")
                )
        );
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(IOExceptionUserLevel))
                .thenReturn(Observable.error(httpExceptionUserLevel));
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(HttpException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(2)).refreshUMods();
        verify(uModsRepositoryMock,times(2)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).saveUMod(umod);
        verify(appUserRepositoryMock,times(2)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
        assertEquals(UModUser.Level.UNAUTHORIZED,umod.getAppUserLevel());
    }

    @Test
    public void
    Given_CreateUserFailsBecauseAlreadyExistAndGetLevelFirstFailsTwiceBecauseTimeOut_When_AccessIsSolicited_Then_ErrorEmitted(){
        //Given
        HttpException httpExceptionCreateUser1 = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 409")
                )
        );
        HttpException httpExceptionCreateUser2 = new HttpException(
                Response.error(
                        500,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 409")
                )
        );
        when(uModsRepositoryMock.createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class))).
                thenReturn(Observable.error(httpExceptionCreateUser1))
                .thenReturn(Observable.error(httpExceptionCreateUser2));
        UMod umod = new UMod("777");
        umod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        when(uModsRepositoryMock.getUMod("777")).thenReturn(Observable.just(umod));
        AppUser appUser = new AppUser("5555","Alexyh","adsl");
        when(appUserRepositoryMock.getAppUser()).thenReturn(Observable.just(appUser));
        IOException iOExceptionUserLevel1 = new IOException("laralara");
        IOException ioExceptionUserLevel2 = new IOException("laralara2");
        when(uModsRepositoryMock.getUserLevel(any(UMod.class),any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.error(iOExceptionUserLevel1))
                .thenReturn(Observable.error(ioExceptionUserLevel2));
        RequestAccess.RequestValues requestValues= new RequestAccess.RequestValues(umod.getUUID());

        //When
        AssertableSubscriber<RequestAccess.ResponseValues>  responseSub= this.requestAccessUC.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(IOException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod("777");
        verify(uModsRepositoryMock,times(2)).refreshUMods();
        verify(uModsRepositoryMock,times(2)).createUModUser(any(UMod.class),any(CreateUserRPC.Arguments.class));
        verify(appUserRepositoryMock,times(2)).getAppUser();
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(appUserRepositoryMock);
    }
}