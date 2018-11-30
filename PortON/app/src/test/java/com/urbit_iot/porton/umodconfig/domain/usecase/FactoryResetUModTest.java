package com.urbit_iot.porton.umodconfig.domain.usecase;

import android.support.annotation.NonNull;
import android.util.Log;
import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.rpc.APIUserType;
import com.urbit_iot.porton.data.rpc.FactoryResetRPC;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.GetUsersRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.data.source.internet.UModMqttServiceContract;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;
import com.urbit_iot.porton.util.schedulers.SchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import retrofit2.adapter.rxjava.HttpException;
import rx.Completable;
import rx.Observable;
import static com.google.common.base.Preconditions.checkNotNull;

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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


public class FactoryResetUModTest {

    private FactoryResetUMod factoryResetUMod;

    @Mock
    private UModsRepository uModsRepositoryMock;

    @Mock
    private UModMqttServiceContract uModMqttServiceContractMock;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Spy
    private SchedulerProvider schedulerProvider;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(Schedulers.immediate()).when(schedulerProvider).ui();
        this.factoryResetUMod = new FactoryResetUMod(uModsRepositoryMock,schedulerProvider,uModMqttServiceContractMock);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(uModsRepositoryMock);
        verifyNoMoreInteractions(uModMqttServiceContractMock);
        verify(schedulerProvider,times(1)).io();
        verify(schedulerProvider,times(1)).ui();
        verifyNoMoreInteractions(schedulerProvider);
        reset(uModsRepositoryMock,uModMqttServiceContractMock,schedulerProvider);
    }

    @Test
    public void Given_ThereWereNoUsersAndResetSuccess_When_ResetIsRequired_Then_ResultIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        when(uModsRepositoryMock.getUMod("777"))
                .thenReturn(Observable.just(uMod));
        //GetUsersRPC.UserResult userResult = new GetUsersRPC.UserResult("Alexyh",APIUserType.User);
        List<GetUsersRPC.UserResult> users = new ArrayList<>();
        //users.add(userResult);
        GetUsersRPC.Result usersResult = new GetUsersRPC.Result(users);
        when(uModsRepositoryMock.getUModUsers(any(UMod.class),any(GetUsersRPC.Arguments.class)))
                .thenReturn(Observable.just(usersResult));
        FactoryResetRPC.Result result = new FactoryResetRPC.Result("Success");
        when(uModsRepositoryMock.factoryResetUMod(any(UMod.class),any(FactoryResetRPC.Arguments.class)))
                .thenReturn(Observable.just(result));
        FactoryResetUMod.RequestValues requestValues= new FactoryResetUMod.RequestValues(uMod.getUUID());

        //When
        AssertableSubscriber<FactoryResetUMod.ResponseValues>  responseSub= this.factoryResetUMod.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(1)).getUModUsers(any(UMod.class),any(GetUsersRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).factoryResetUMod(any(UMod.class),any(FactoryResetRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).deleteUMod(uMod.getUUID());
    }

    @Test
    public void Given_ThereWereNoUsersAndResetPropagatesAnUnhandledException_When_ResetIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        when(uModsRepositoryMock.getUMod("777"))
                .thenReturn(Observable.just(uMod));
        //GetUsersRPC.UserResult userResult = new GetUsersRPC.UserResult("Alexyh",APIUserType.User);
        List<GetUsersRPC.UserResult> users = new ArrayList<>();
        //users.add(userResult);
        GetUsersRPC.Result usersResult = new GetUsersRPC.Result(users);
        when(uModsRepositoryMock.getUModUsers(any(UMod.class),any(GetUsersRPC.Arguments.class)))
                .thenReturn(Observable.just(usersResult));
        when(uModsRepositoryMock.factoryResetUMod(any(UMod.class),any(FactoryResetRPC.Arguments.class)))
                .thenReturn(Observable.error(new RuntimeException()));
        FactoryResetUMod.RequestValues requestValues= new FactoryResetUMod.RequestValues(uMod.getUUID());

        //When
        AssertableSubscriber<FactoryResetUMod.ResponseValues>  responseSub= this.factoryResetUMod.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(RuntimeException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModsRepositoryMock,times(1)).getUModUsers(any(UMod.class),any(GetUsersRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).factoryResetUMod(any(UMod.class),any(FactoryResetRPC.Arguments.class));
    }

    @Test
    public void Given_ThereWereUsersAndResetFirstFailsBecauseIOExceptionThenSuccess_WhenResetIsRequired_Then_ResultIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        when(uModsRepositoryMock.getUMod("777"))
                .thenReturn(Observable.just(uMod));
        when(uModMqttServiceContractMock.cancelSeveralUModInvitations(any(List.class),any(UMod.class)))
                .thenReturn(Completable.complete());
        GetUsersRPC.UserResult userResult = new GetUsersRPC.UserResult("Alexyh",APIUserType.User);
        List<GetUsersRPC.UserResult> users = new ArrayList<>();
        users.add(userResult);
        GetUsersRPC.Result usersResult = new GetUsersRPC.Result(users);
        when(uModsRepositoryMock.getUModUsers(any(UMod.class),any(GetUsersRPC.Arguments.class)))
                .thenReturn(Observable.just(usersResult));
        FactoryResetRPC.Result result = new FactoryResetRPC.Result("Success");
        when(uModsRepositoryMock.factoryResetUMod(any(UMod.class),any(FactoryResetRPC.Arguments.class)))
                .thenReturn(Observable.error(new IOException("error por IO")))
                .thenReturn(Observable.just(result));
        FactoryResetUMod.RequestValues requestValues= new FactoryResetUMod.RequestValues(uMod.getUUID());

        //When
        AssertableSubscriber<FactoryResetUMod.ResponseValues>  responseSub= this.factoryResetUMod.buildUseCase(requestValues).test();

        //Then
        responseSub.assertCompleted()
                .assertValueCount(1);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModMqttServiceContractMock,times(2)).cancelSeveralUModInvitations(any(List.class),any(UMod.class));
        verify(uModsRepositoryMock,times(2)).getUModUsers(any(UMod.class),any(GetUsersRPC.Arguments.class));
        verify(uModsRepositoryMock,times(2)).factoryResetUMod(any(UMod.class),any(FactoryResetRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).refreshUMods();
        verify(uModsRepositoryMock,times(1)).deleteUMod(uMod.getUUID());
    }

    @Test
    public void Given_ThereWasAUserAndResetFailsBecauseIOExceptionTwice_When_ResetIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        when(uModsRepositoryMock.getUMod("777"))
                .thenReturn(Observable.just(uMod));
        when(uModMqttServiceContractMock.cancelSeveralUModInvitations(any(List.class),any(UMod.class)))
                .thenReturn(Completable.complete());
        GetUsersRPC.UserResult userResult = new GetUsersRPC.UserResult("Alexyh",APIUserType.User);
        List<GetUsersRPC.UserResult> users = new ArrayList<>();
        users.add(userResult);
        GetUsersRPC.Result usersResult = new GetUsersRPC.Result(users);
        when(uModsRepositoryMock.getUModUsers(any(UMod.class),any(GetUsersRPC.Arguments.class)))
                .thenReturn(Observable.just(usersResult));
        when(uModsRepositoryMock.factoryResetUMod(any(UMod.class),any(FactoryResetRPC.Arguments.class)))
                .thenReturn(Observable.error(new IOException("error por IO")));
        FactoryResetUMod.RequestValues requestValues= new FactoryResetUMod.RequestValues(uMod.getUUID());

        //When
        AssertableSubscriber<FactoryResetUMod.ResponseValues>  responseSub= this.factoryResetUMod.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(IOException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModMqttServiceContractMock,times(2)).cancelSeveralUModInvitations(any(List.class),any(UMod.class));
        verify(uModsRepositoryMock,times(2)).getUModUsers(any(UMod.class),any(GetUsersRPC.Arguments.class));
        verify(uModsRepositoryMock,times(2)).factoryResetUMod(any(UMod.class),any(FactoryResetRPC.Arguments.class));

        verify(uModsRepositoryMock,times(1)).refreshUMods();
    }

    @Test
    public void Given_ThereWasAUserAndResetFailsWithHttpException_When_ResetIsRequired_Then_ErrorIsEmitted(){
        //Given
        UMod uMod = new UMod("777");
        when(uModsRepositoryMock.getUMod("777"))
                .thenReturn(Observable.just(uMod));
        when(uModMqttServiceContractMock.cancelSeveralUModInvitations(any(List.class),any(UMod.class)))
                .thenReturn(Completable.complete());
        GetUsersRPC.UserResult userResult = new GetUsersRPC.UserResult("Alexyh",APIUserType.User);
        List<GetUsersRPC.UserResult> users = new ArrayList<>();
        users.add(userResult);
        GetUsersRPC.Result usersResult = new GetUsersRPC.Result(users);
        when(uModsRepositoryMock.getUModUsers(any(UMod.class),any(GetUsersRPC.Arguments.class)))
                .thenReturn(Observable.just(usersResult));
        HttpException httpException = new HttpException(
                Response.error(
                        400,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 400")
                )
        );
        when(uModsRepositoryMock.factoryResetUMod(any(UMod.class),any(FactoryResetRPC.Arguments.class)))
                .thenReturn(Observable.error(httpException));
        FactoryResetUMod.RequestValues requestValues= new FactoryResetUMod.RequestValues(uMod.getUUID());

        //When
        AssertableSubscriber<FactoryResetUMod.ResponseValues>  responseSub= this.factoryResetUMod.buildUseCase(requestValues).test();

        //Then
        responseSub.assertError(HttpException.class)
                .assertValueCount(0);
        verify(uModsRepositoryMock,times(1)).getUMod(uMod.getUUID());
        verify(uModMqttServiceContractMock,times(1)).cancelSeveralUModInvitations(any(List.class),any(UMod.class));
        verify(uModsRepositoryMock,times(1)).getUModUsers(any(UMod.class),any(GetUsersRPC.Arguments.class));
        verify(uModsRepositoryMock,times(1)).factoryResetUMod(any(UMod.class),any(FactoryResetRPC.Arguments.class));
    }
}