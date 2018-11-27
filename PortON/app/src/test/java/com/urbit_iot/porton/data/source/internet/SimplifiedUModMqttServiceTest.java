package com.urbit_iot.porton.data.source.internet;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.RPC;
import com.urbit_iot.porton.data.rpc.TriggerRPC;
import com.urbit_iot.porton.util.GlobalConstants;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.observers.AssertableSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SimplifiedUModMqttServiceTest {

    @Mock
    private PahoClientRxWrap pahoClientRxWrapMock;

    private String appUsernameMock;

    private Gson gsonInstance;

    @Mock
    private BaseSchedulerProvider mSchedulerProviderMock;

    private SimplifiedUModMqttService simplifiedUModMqttService;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Before
    public void setUp() throws Exception {
        gsonInstance = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.io());
        when(mSchedulerProviderMock.computation()).thenReturn(Schedulers.computation());
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        simplifiedUModMqttService= new SimplifiedUModMqttService(
                pahoClientRxWrapMock,
                appUsernameMock,
                gsonInstance,
                mSchedulerProviderMock);
        reset(pahoClientRxWrapMock);
    }

    @After
    public void tearDown() throws Exception {
        reset(  pahoClientRxWrapMock,
                mSchedulerProviderMock );
        simplifiedUModMqttService = null;
    }

    @Test
    public void Given_BrokerConnectionAndTopicSubcriptionSucceed_When_subscribeToUModResponseTopic_Then_creaResponseTopicForUModYSeConectaBrokerYSeSuscribeAlTopico(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        //WHEN
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.subscribeToTopic(any(String.class),any(Integer.class))).thenReturn(Completable.complete());
        simplifiedUModMqttService.subscribeToUModResponseTopic(moduloMock.getUUID());
        //THEN
        verify(pahoClientRxWrapMock,times(1)).connectToBroker();
        verify(pahoClientRxWrapMock,times(1)).subscribeToTopic(any(String.class),any(Integer.class));
        verifyNoMoreInteractions(pahoClientRxWrapMock);

    }

    @Test
    public void When_subscribeToUModResponseTopic_Then_llamamosMetodoSubscribeToUModResponseTopic(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        SimplifiedUModMqttService simplifiedUModMqttServiceSpy= spy(simplifiedUModMqttService);
        //WHEN
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.subscribeToTopic(any(String.class),any(Integer.class))).thenReturn(Completable.complete());
        simplifiedUModMqttServiceSpy.subscribeToUModResponseTopic(moduloMock);
        //THEN
        verify(simplifiedUModMqttServiceSpy,times(1)).subscribeToUModResponseTopic(moduloMock.getUUID());
    }

    //TODO COPIAR CAMBIOS DE STEVE
    @Test
    public void Given_TeniendoSintaxisValidaJSonYNoErroesRespuesta_When_publishRPC_Then_DevuelvoObservableConLaRespuesta(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        RPC.Request request = new CreateUserRPC.Request(null,"2123", "a",123);
        RPC.Response response = new CreateUserRPC.Response(null,123,"a",null);
        String jsonRequest = gsonInstance.toJson(request);
        String jsonResponse = gsonInstance.toJson(response);
        MqttMessage mqttMessage= new MqttMessage();
        mqttMessage.setPayload(jsonResponse.getBytes());
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.publishToTopic(jsonRequest.getBytes(),moduloMock.getUModRequestTopic(),1,false)).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.receivedMessagesObservable()).thenReturn(Observable.just(mqttMessage));
        TestScheduler testScheduler = new TestScheduler();
        when(mSchedulerProviderMock.computation()).thenReturn(testScheduler);
        //WHEN
        AssertableSubscriber testSubscriber = simplifiedUModMqttService.publishRPC(moduloMock,request,response.getClass()).test();
        testScheduler.advanceTimeBy(8,TimeUnit.SECONDS);
        testSubscriber.assertCompleted();
        //THEN
        assertEquals(123, response.getResponseId());
    }

    @Test
    public void Given_TeniendoSintaxisValidaJSonYNoErroesRespuestaPeroDistintosID_When_publishRPC_Then_DevuelvoObservableConLaRespuesta(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        RPC.Request request = new CreateUserRPC.Request(null,"2123", "a",123);
        RPC.Response response= new TriggerRPC.Response(null,333,"a",null);
        String jsonRequest = gsonInstance.toJson(request);
        String jsonResponse = gsonInstance.toJson(response);
        MqttMessage mqttMessage= new MqttMessage();
        mqttMessage.setPayload(jsonResponse.getBytes());
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.publishToTopic(jsonRequest.getBytes(),moduloMock.getUModRequestTopic(),1,false)).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.receivedMessagesObservable()).thenReturn(Observable.just(mqttMessage));
        TestScheduler testScheduler = new TestScheduler();
        when(mSchedulerProviderMock.computation()).thenReturn(testScheduler);
        //WHEN
        when(pahoClientRxWrapMock.publishToTopic(jsonRequest.getBytes(),moduloMock.getUModRequestTopic(),1,false)).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.receivedMessagesObservable()).thenReturn(Observable.just(mqttMessage));
        AssertableSubscriber testSubscriber = simplifiedUModMqttService.publishRPC(moduloMock,request,response.getClass()).test();
        testScheduler.advanceTimeBy(8,TimeUnit.SECONDS);
        testSubscriber.assertNotCompleted();
    }

    @Test
    public void Given_TeniendoSintaxisNoValidaJSon_When_publishRPC_Then_DevuelvoObservableVacio(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        RPC.Request request = new CreateUserRPC.Request(null,"2123", "a",123);
        RPC.Response response = new CreateUserRPC.Response(null,123,"a",null);
        MqttMessage mqttMessage= new MqttMessage();
        String jsonRequest = gsonInstance.toJson(request);
        byte[] a = {2};
        mqttMessage.setPayload(a);
        TestScheduler testScheduler = new TestScheduler();
        when(mSchedulerProviderMock.computation()).thenReturn(testScheduler);
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.publishToTopic(jsonRequest.getBytes(),moduloMock.getUModRequestTopic(),1,false)).thenReturn(Completable.complete());
        PublishSubject<MqttMessage> emisorDeRespuestas = PublishSubject.create();
        when(pahoClientRxWrapMock.receivedMessagesObservable()).thenReturn(emisorDeRespuestas.asObservable());
        //WHEN
        AssertableSubscriber testSubscriber = simplifiedUModMqttService.publishRPC(moduloMock,request,response.getClass()).test();
        emisorDeRespuestas.onNext(mqttMessage);
        testScheduler.advanceTimeBy(8, TimeUnit.SECONDS);
        testSubscriber.assertError(TimeoutException.class)
                .assertValueCount(0);
    }

    @Test
    public void Given_TeniendoSintaxisValidaJSonYErroesRespuestaHTTPDesautorizado_When_publishRPC_Then_DevuelvoObservableConExpecion(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        RPC.Request request = new CreateUserRPC.Request(null,"2123", "a",123);
        RPC.ResponseError error= new RPC.ResponseError (401,"errorHTTP");
        RPC.Response response = new CreateUserRPC.Response(null,123,"a",error);
        String jsonResponse = gsonInstance.toJson(response);
        MqttMessage mqttMessage= new MqttMessage();
        mqttMessage.setPayload(jsonResponse.getBytes());
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.publishToTopic(any(byte[].class),any(String.class),any(Integer.class),any(Boolean.class))).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.receivedMessagesObservable()).thenReturn(Observable.just(mqttMessage));
        TestScheduler testScheduler = new TestScheduler();
        when(mSchedulerProviderMock.computation()).thenReturn(testScheduler);
        //WHEN
        Observable<? extends RPC.Response> testObservable = simplifiedUModMqttService.publishRPC(moduloMock,request,response.getClass());
        Flowable<? extends RPC.Response> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        TestSubscriber testSubscriber = testFlowable.test();
        testScheduler.advanceTimeBy(8,TimeUnit.SECONDS);
        testSubscriber
                .assertError(retrofit2.adapter.rxjava.HttpException.class)
                .assertErrorMessage("HTTP 401 Response.error()");
    }

    @Test
    public void Given_TeniendoSintaxisValidaJSonYErroesRespuestaHTTP_FORBIDDEN_When_publishRPC_Then_DevuelvoObservableConExpecion(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        RPC.Request request = new CreateUserRPC.Request(null,"2123", "a",123);
        RPC.ResponseError error= new RPC.ResponseError (403,"errorHTTP");
        RPC.Response response = new CreateUserRPC.Response(null,123,"a",error);
        String jsonResponse = gsonInstance.toJson(response);
        MqttMessage mqttMessage= new MqttMessage();
        mqttMessage.setPayload(jsonResponse.getBytes());
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.publishToTopic(any(byte[].class),any(String.class),any(Integer.class),any(Boolean.class))).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.receivedMessagesObservable()).thenReturn(Observable.just(mqttMessage));
        TestScheduler testScheduler = new TestScheduler();
        when(mSchedulerProviderMock.computation()).thenReturn(testScheduler);
        //WHEN
        Observable<? extends RPC.Response> testObservable = simplifiedUModMqttService.publishRPC(moduloMock,request,response.getClass());
        Flowable<? extends RPC.Response> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        TestSubscriber testSubscriber = testFlowable.test();
        testScheduler.advanceTimeBy(8,TimeUnit.SECONDS);
        testSubscriber
                .assertError(retrofit2.adapter.rxjava.HttpException.class)
                .assertErrorMessage("HTTP 403 Response.error()");
    }

    @Test
    public void Given_TeniendoSintaxisValidaJSonYErroesRespuestaDistintoHTTP_FORBIDDEN_When_publishRPC_Then_DevuelvoObservableConExpecion(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        RPC.Request request = new CreateUserRPC.Request(null,"2123", "a",123);
        RPC.ResponseError error= new RPC.ResponseError (203,"errorHTTP");
        RPC.Response response = new CreateUserRPC.Response(null,123,"a",error);
        String jsonResponse = gsonInstance.toJson(response);
        MqttMessage mqttMessage= new MqttMessage();
        mqttMessage.setPayload(jsonResponse.getBytes());
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.publishToTopic(any(byte[].class),any(String.class),any(Integer.class),any(Boolean.class))).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.receivedMessagesObservable()).thenReturn(Observable.just(mqttMessage));
        TestScheduler testScheduler = new TestScheduler();
        when(mSchedulerProviderMock.computation()).thenReturn(testScheduler);
        //WHEN
        Observable<? extends RPC.Response> testObservable = simplifiedUModMqttService.publishRPC(moduloMock,request,response.getClass());
        Flowable<? extends RPC.Response> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        TestSubscriber testSubscriber = testFlowable.test();
        testScheduler.advanceTimeBy(8,TimeUnit.SECONDS);
        testSubscriber
                .assertError(retrofit2.adapter.rxjava.HttpException.class)
                .assertErrorMessage("HTTP 500 Response.error()");

    }

    @Test
    public void Given_RecibiendoMensajeInvitacionConUUIDDNulo_When_scanUModInvitations_Then_DevuelvoObservableVacio(){
        //GIVEN
        SimplifiedUModMqttService simplifiedUModMqttServiceSpy= spy(simplifiedUModMqttService);
        MqttMessage mqttMessage= new MqttMessage();
        when(pahoClientRxWrapMock.receivedMessagesObservable()).thenReturn(Observable.just(mqttMessage));
        doNothing().when(simplifiedUModMqttServiceSpy).resetInvitationTopic();
        //WHEN
        simplifiedUModMqttServiceSpy.scanUModInvitations().test()
                .assertCompleted()
                .assertValueCount(0);
    }

    @Test
    public void Given_RecibiendoMensajeInvitacionConUUIDDNoNulo_When_scanUModInvitations_Then_DevuelvoObservableConModulo(){
        //GIVEN
        SimplifiedUModMqttService simplifiedUModMqttServiceSpy= spy(simplifiedUModMqttService);
        when(pahoClientRxWrapMock.subscribeToTopic(any(String.class),any(Integer.class))).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        MqttMessage mqttMessage= new MqttMessage();
        UMod modulo= new UMod("123");
        mqttMessage.setPayload(modulo.getAlias().getBytes());
        when(pahoClientRxWrapMock.receivedMessagesObservable()).thenReturn(Observable.just(mqttMessage));
        doNothing().when(simplifiedUModMqttServiceSpy).resetInvitationTopic();
        doReturn(modulo.getUUID()).when(simplifiedUModMqttServiceSpy).getUUIDFromUModAdvertisedID(mqttMessage.toString());
        //WHEN
        Observable<UMod> testObservable = simplifiedUModMqttServiceSpy.scanUModInvitations();
        Flowable<UMod> testFlowable = RxJavaInterop.toV2Flowable(testObservable);
        testFlowable.test()
                .assertValues(modulo)
                .assertValueCount(1)
                .assertValue(uMod -> uMod.getAppUserLevel() == UModUser.Level.INVITED)
                .assertValue(uMod -> uMod.getuModSource() == UMod.UModSource.MQTT_SCAN)
                .assertValue(uMod -> uMod.getState() == UMod.State.STATION_MODE)
                .assertValue(uMod -> uMod.getConnectionAddress() == null);

        verify(simplifiedUModMqttServiceSpy,times(1)).subscribeToUModResponseTopic(modulo);
    }

    @Test
    public void When_cancelUModInvitation_Then_SePublicaUnaSolicitudDeCancelarParaEseNombreYUUID(){
        //GIVEN
        String userName="Incubadora";
        String UUID="123";
        String invitationsTopic = userName
                + "/invitation/"
                + GlobalConstants.URBIT_PREFIX
                + UUID;
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.io());
        when(pahoClientRxWrapMock.publishToTopic(new byte[0],invitationsTopic,1,true)).thenReturn(Completable.complete());
        //WHEN
        simplifiedUModMqttService.cancelUModInvitation(userName,UUID);
        //THEN
        verify(pahoClientRxWrapMock,times(1)).connectToBroker();
        verify(mSchedulerProviderMock,times(2)).io();
        verify(pahoClientRxWrapMock,times(1)).publishToTopic(new byte[0],invitationsTopic,1,true);
    }

    @Test
    public void When_cancelMyInvitation_Then_LlamoAFuncionCancelUModInvitationConHostNameYUUIDDelModulo(){
        //GIVEN
        UMod moduloMock= new UMod("123");
        SimplifiedUModMqttService simplifiedUModMqttServiceSpy= spy(simplifiedUModMqttService);
        doReturn(Completable.complete()).when(simplifiedUModMqttServiceSpy).cancelUModInvitation(simplifiedUModMqttServiceSpy.appUsername,"123");
        //WHEN
        simplifiedUModMqttServiceSpy.cancelMyInvitation(moduloMock);
        //THEN
        verify(simplifiedUModMqttServiceSpy,times(1)).cancelUModInvitation(simplifiedUModMqttServiceSpy.appUsername,moduloMock.getUUID());
    }

    @Test
    public void When_cancelSeveralUModInvitations_Then_CanceloLasPeticionesDelModulo(){
        //GIVEN
        SimplifiedUModMqttService simplifiedUModMqttServiceSpy= spy(simplifiedUModMqttService);
        String[] topicsArr = new String[14];
        for(int i=0;i<14;i++){
            topicsArr[i]= "peticiones";
        }
        List<String> topicos = Arrays.asList(topicsArr);
        UMod moduloMock= new UMod("123");
        TestScheduler testScheduler = new TestScheduler();
        Completable simplifiedUModMqttServiceObservable= simplifiedUModMqttServiceSpy.cancelSeveralUModInvitations(topicos,moduloMock);
        Flowable <SimplifiedUModMqttService> simplifiedUModMqttServiceFlowable= RxJavaInterop.toV2Flowable(simplifiedUModMqttServiceObservable.toObservable());
        doReturn(Completable.complete().delay(300L, TimeUnit.MILLISECONDS,testScheduler)).when(simplifiedUModMqttServiceSpy).cancelUModInvitation(any(String.class),any(String.class));
        TestSubscriber<SimplifiedUModMqttService> testSubscriber = simplifiedUModMqttServiceFlowable.test();
        //WHEN1
        testScheduler.advanceTimeBy(300L,TimeUnit.MILLISECONDS);
        //THEN1
        testSubscriber.assertNotComplete();
        //WHEN2
        testScheduler.advanceTimeBy(300L,TimeUnit.MILLISECONDS);
        //THEN2
        testSubscriber.assertComplete();
    }

    @Test
    public void When_clearAllSubscriptions_Then_SeLlamaAUnsubscribeFromAllTopics(){
        //GIVEN
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.unsubscribeFromAllTopics()).thenReturn(Completable.complete());
        //WHEN
        simplifiedUModMqttService.clearAllSubscriptions();
        //THEN
        verify(pahoClientRxWrapMock,times(1)).unsubscribeFromAllTopics();
    }

    @Test
    public void When_resetInvitationTopic_Then_ConectoBrokerYBorroSolicitudYPidoSolicitud(){
        //GIVEN
        when(pahoClientRxWrapMock.connectToBroker()).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.unsubscribeFromTopic(any(String.class))).thenReturn(Completable.complete());
        when(pahoClientRxWrapMock.subscribeToTopic(any(String.class),any(Integer.class),any(Boolean.class))).thenReturn(Completable.complete());
        //WHEN
        simplifiedUModMqttService.resetInvitationTopic();
        //THEN
        verify(pahoClientRxWrapMock,times(1)).connectToBroker();
        verify(pahoClientRxWrapMock,times(1)).unsubscribeFromTopic(any(String.class));
        verify(pahoClientRxWrapMock,times(1)).subscribeToTopic(any(String.class),any(Integer.class),any(Boolean.class));
    }
}