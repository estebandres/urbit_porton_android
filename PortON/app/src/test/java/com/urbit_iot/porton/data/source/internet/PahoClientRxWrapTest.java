package com.urbit_iot.porton.data.source.internet;



import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import java.sql.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.observers.AssertableSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PahoClientRxWrapTest {

    @NonNull
    @Mock
    private MqttAsyncClient mqttAsyncClientMock;

    @NonNull
    @Mock
    private BaseSchedulerProvider mSchedulerProviderMock;

    private PahoClientRxWrap pahoClientRxWrap;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Before
    public void setUp() throws Exception {
        pahoClientRxWrap = new PahoClientRxWrap(
                mqttAsyncClientMock,
                mSchedulerProviderMock
        );
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mqttAsyncClientMock,mSchedulerProviderMock);
        reset(
                mqttAsyncClientMock,
                mSchedulerProviderMock
        );
        pahoClientRxWrap=null;
    }

    @Test
    public void Given_TeniendorequestedSubscriptionTopicsYReconeccion_When_connectComplete_Then_LlamamossubscribeToSeveralTopics(){
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy= spy(pahoClientRxWrap);
        pahoClientRxWrapSpy.requestedSubscriptionTopics.add("mock");
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        doReturn(Completable.complete()).when(pahoClientRxWrapSpy).subscribeToSeveralTopics(pahoClientRxWrapSpy.requestedSubscriptionTopics.toArray(new String[0]),1);
        //WHEN
        pahoClientRxWrapSpy.connectComplete(true,"mock");
        //THEN
        verify(pahoClientRxWrapSpy,times(1)).subscribeToSeveralTopics(pahoClientRxWrapSpy.requestedSubscriptionTopics.toArray(new String[0]),1);
        verifyZeroInteractions(mqttAsyncClientMock);
        verifyZeroInteractions(mSchedulerProviderMock);
    }

    @Test
    public void Given_SinTenerExepcionYClienteMqttConectado_When_connectToBroker_Then_DevuelvoObservableConectoClienteDevuelvoSemaforo() throws MqttException, InterruptedException {
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy= spy(pahoClientRxWrap);
        pahoClientRxWrapSpy.connectionMutex = new Semaphore(1);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.connectToBroker().test()
                .assertCompleted();
        //THEN
        assertNotNull(pahoClientRxWrapSpy.subscriptionsKillerSubject);
        assertEquals(1,pahoClientRxWrapSpy.connectionMutex.availablePermits());
        verifyZeroInteractions(mqttAsyncClientMock);
        verifyZeroInteractions(mSchedulerProviderMock);
    }


    @Test
    public void
    Given_SinTenerExepcionYClienteMqttDesconectado_When_connectToBroker_Then_DevuelvoObservableConectoClienteDevuelvoSemaforo() throws MqttException {
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy= spy(pahoClientRxWrap);
        pahoClientRxWrapSpy.connectionMutex = new Semaphore(1);
        when(mqttAsyncClientMock.isConnected()).thenReturn(false);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        IMqttToken connectionToken = mock(IMqttToken.class);
        doNothing().when(connectionToken).waitForCompletion(5200L);
        when(mqttAsyncClientMock.connect(any(MqttConnectOptions.class))).thenReturn(connectionToken);
        //WHEN
        pahoClientRxWrapSpy.connectToBroker().test()
                .onCompleted();
        //THEN
        assertEquals(1,pahoClientRxWrapSpy.connectionMutex.availablePermits());
        verifyZeroInteractions(mqttAsyncClientMock);
        verifyZeroInteractions(mSchedulerProviderMock);
    }


    @Test
    public void Given_ExcepcionMqttPorCLIENT_CONNECTED_When_connectToBroker_Then_LiberaSemaforoYEmiteLaComplecion() throws MqttException {
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        pahoClientRxWrapSpy.connectionMutex = new Semaphore(1);
        MqttException mqttException = new MqttException(32100);
        when(mqttAsyncClientMock.isConnected()).thenReturn(false);
        when(mqttAsyncClientMock.connect(any(MqttConnectOptions.class))).thenThrow(mqttException);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.connectToBroker().test()
                .assertCompleted();
        assertEquals(1,pahoClientRxWrapSpy.connectionMutex.availablePermits());
        verifyZeroInteractions(mqttAsyncClientMock);
        verifyZeroInteractions(mSchedulerProviderMock);
    }


    @Test
    public void Given_ExcepcionMqttPorDistintaCLIENT_CONNECTED_When_connectToBroker_Then_LiberaSemaforoYPropagaLaExcepcion() throws MqttException {
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        pahoClientRxWrapSpy.connectionMutex = new Semaphore(1);
        MqttException mqttException = new MqttException(20);
        when(mqttAsyncClientMock.isConnected()).thenReturn(false);
        when(mqttAsyncClientMock.connect(any(MqttConnectOptions.class))).thenThrow(mqttException);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.connectToBroker().test()
                .assertError(mqttException);
        assertEquals(1,pahoClientRxWrapSpy.connectionMutex.availablePermits());
        verifyZeroInteractions(mqttAsyncClientMock);
        verifyZeroInteractions(mSchedulerProviderMock);
    }


    @Test
    public void Given_InterruptedException_When_connectToBroker_Then_SemaforoNuncaCapturadoYSePropagaLaExcepcion() throws InterruptedException {
        //GIVEN
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Scheduler scheduler = Schedulers.from(executor);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        pahoClientRxWrapSpy.connectionMutex = new Semaphore(0);
        //WHEN
        TestObserver<Void> subscriber = RxJavaInterop.toV2Completable(Completable.complete().subscribeOn(scheduler)
                .andThen(pahoClientRxWrapSpy.connectToBroker())).test();
        executor.shutdownNow();
        subscriber.await();
        //THEN
        subscriber.assertError(InterruptedException.class);
        assertEquals(0,pahoClientRxWrapSpy.connectionMutex.availablePermits());
        verifyZeroInteractions(mqttAsyncClientMock);
        verifyZeroInteractions(mSchedulerProviderMock);
    }


    @Test
    public void Given_SinExcepcionYClienteMqttDesconectado_When_publishToTopic_Then_DevuelvoSemaforoYObservableConExcepcion(){
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        pahoClientRxWrapSpy.publishMutex = new Semaphore(1);
        when(mqttAsyncClientMock.isConnected()).thenReturn(false);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        String mensajeMock= "Mock";
        String topicMock= "Topic";
        int qMock= 1;
        boolean retainedMock=false;
        //WHEN
        pahoClientRxWrapSpy.publishToTopic(mensajeMock.getBytes(),topicMock,qMock,retainedMock).test()
                .assertError(Exception.class);
        //THEN
        assertEquals(1,pahoClientRxWrapSpy.publishMutex.availablePermits());
        verifyZeroInteractions(mqttAsyncClientMock);
        verifyZeroInteractions(mSchedulerProviderMock);
    }


    @Test
    public void
    Given_SinExcepcionYClienteMqttConectado_When_publishToTopic_Then_LiberaSemaforoYEmiteLaComplecion() throws MqttException {
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        pahoClientRxWrapSpy.publishMutex = new Semaphore(1);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        when(mqttAsyncClientMock.getInFlightMessageCount()).thenReturn(3);
        String mensajeMock= "Mock";
        String topicMock= "Topic";
        int qMock= 1;
        boolean retainedMock=false;
        IMqttDeliveryToken publIMqttTokenMock = mock(IMqttDeliveryToken.class);
        doNothing().when(publIMqttTokenMock).waitForCompletion(8200L);
        when(mqttAsyncClientMock.publish(any(String.class),any(byte[].class),any(Integer.class), any(Boolean.class))).thenReturn(publIMqttTokenMock);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.publishToTopic(mensajeMock.getBytes(),topicMock,qMock,retainedMock).test()
                .assertCompleted();
        //THEN
        assertEquals(1,pahoClientRxWrapSpy.publishMutex.availablePermits());
        verifyZeroInteractions(mSchedulerProviderMock);
    }


    @Test
    public void Given_MqttException_When__publishToTopic_Then_DevuelvoSemaforoYDevuelvoObservableConExcepcion() throws MqttException {
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        pahoClientRxWrapSpy.publishMutex = new Semaphore(1);
        MqttException mqttException= new MqttException(1);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        when(mqttAsyncClientMock.getInFlightMessageCount()).thenReturn(3);
        when(mqttAsyncClientMock.publish(any(String.class),any(byte[].class),any(Integer.class), any(Boolean.class))).thenThrow(mqttException);
        String mensajeMock= "Mock";
        String topicMock= "Topic";
        int qMock= 1;
        boolean retainedMock=false;
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.publishToTopic(mensajeMock.getBytes(),topicMock,qMock,retainedMock).test()
                .assertError(mqttException);
        //THEN
        assertEquals(1,pahoClientRxWrapSpy.publishMutex.availablePermits());
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyNoMoreInteractions(mqttAsyncClientMock);

    }

    @Test
    public void When_addRequestedSubscriptionTopic_Then_SemafroEsLiberadoYTopicoAgregadoAlConjunto(){
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        pahoClientRxWrapSpy.subscriptionMutex = new Semaphore(1);
        String topicMock= "Topic";
        //WHEN
        pahoClientRxWrap.addRequestedSubscriptionTopic(topicMock);
        //THEN
        assertEquals(1,pahoClientRxWrapSpy.publishMutex.availablePermits());
        assertTrue(pahoClientRxWrapSpy.requestedSubscriptionTopics.contains(topicMock));
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void When_subscribeToTopicSinBandera_Then_LlamoSubscribeToTopicConBanderaEnTrue(){
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        String topicMock= "Topic";
        int qMock= 1;
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.subscribeToTopic(topicMock,qMock);
        //THEN
        verify(pahoClientRxWrapSpy,times(1)).subscribeToTopic(topicMock,qMock,true);
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_storeTopicForResubscriptionTrueYClienteMqttDesconectado_When_subscribeToTopic_Then_AgregaTopicoAlConjuntoDeSubscripcionesSolicitadasLiberaSemaforoYEmiteComplecion(){
        //GIVEN
        String topicMock= "Topic";
        int qMock= 1;
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        pahoClientRxWrapSpy.subscriptionMutex = new Semaphore(1);
        when(mqttAsyncClientMock.isConnected()).thenReturn(false);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.subscribeToTopic(topicMock,qMock,true).test()
                .assertError(Exception.class);
        //THEN
        assertEquals(1,pahoClientRxWrapSpy.publishMutex.availablePermits());
        assertTrue(pahoClientRxWrapSpy.requestedSubscriptionTopics.contains(topicMock));
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_storeTopicForResubscriptionTrueYClienteMqttConectadoYSuscirpcionExitosa_When_subscribeToTopic_Then_AgregaTopicoAlConjuntoDeSubscripcionesExitosasLiberaSemaforoYEmiteComplecion() throws MqttException {
        //GIVEN
        String topicMock= "Topic";
        int qMock= 1;
        IMqttToken publIMqttTokenMock = mock(IMqttToken.class);
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        pahoClientRxWrapSpy.subscriptionMutex = new Semaphore(1);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        when(mqttAsyncClientMock.subscribe(any(String.class),any(Integer.class),any(),any(IMqttActionListener.class))).
                thenAnswer((Answer<IMqttToken>) invocation -> {
                    IMqttActionListener actionListener = invocation.getArgument(3);
                    actionListener.onSuccess(publIMqttTokenMock);
                    return publIMqttTokenMock;
                });

        //WHEN
        AssertableSubscriber subscriber = pahoClientRxWrapSpy.subscribeToTopic(topicMock,qMock,true).test();
        //THEN
        subscriber.assertCompleted();
        assertEquals(1,pahoClientRxWrapSpy.publishMutex.availablePermits());
        assertTrue(pahoClientRxWrapSpy.requestedSubscriptionTopics.contains(topicMock));
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_storeTopicForResubscriptionTrueYClienteMqttConectadoYFallaSuscripcion_When_subscribeToTopic_Then_EmitoError() throws MqttException {
        //GIVEN
        String topicMock= "Topic";
        int qMock= 1;
        IMqttToken publIMqttTokenMock = mock(IMqttToken.class);
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        pahoClientRxWrapSpy.subscriptionMutex = new Semaphore(1);
        Exception exception= new Exception();
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        when(mqttAsyncClientMock.subscribe(any(String.class),any(Integer.class),any(),any(IMqttActionListener.class))).
                thenAnswer((Answer<IMqttToken>) invocation -> {
                    IMqttActionListener actionListener = invocation.getArgument(3);
                    actionListener.onFailure(publIMqttTokenMock,exception);
                    return publIMqttTokenMock;
                });

        //WHEN
        AssertableSubscriber subscriber = pahoClientRxWrapSpy.subscribeToTopic(topicMock,qMock,true)
                .test();
        //THEN
        subscriber.assertError(exception);
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_SubscripcionFallaPorMqttException_When_subscribeToTopic_Then_EmitoCompletableConExcepcion() throws MqttException {
        //GIVEN
        String topicMock= "Topic";
        int qMock= 1;
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        pahoClientRxWrapSpy.subscriptionMutex = new Semaphore(1);
        MqttException mqttException= new MqttException(3200);
        when(mqttAsyncClientMock.subscribe(any(String.class),any(Integer.class),any(),any(IMqttActionListener.class))).thenThrow(mqttException);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.subscribeToTopic(topicMock,qMock,true).test()
                .assertError(mqttException);
        //THEN
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_ClienteMqttDesconectado_When_unsubscribeFromTopic_Then_EmitoError(){
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        String topicMock= "Topic";
        when(mqttAsyncClientMock.isConnected()).thenReturn(false);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.unsubscribeFromTopic(topicMock).test()
                .assertError(Exception.class);
        //THEN
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_ClienteMqttConectadoDesuscripcionExitosa_When_unsubscribeFromTopic_Then_AgregoTopicoDevuelvoSemaforoEmitoCompletable() throws MqttException {
        //GIVEN
        String topicMock= "Topic";
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        pahoClientRxWrapSpy.subscriptionMutex = new Semaphore(1);
        IMqttToken publIMqttTokenMock = mock(IMqttToken.class);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        when(mqttAsyncClientMock.unsubscribe(any(String.class),any(),any(IMqttActionListener.class)))
                .thenAnswer((Answer<IMqttToken>) invocation -> {
                    IMqttActionListener actionListener = invocation.getArgument(2);
                    actionListener.onSuccess(publIMqttTokenMock);
                    return publIMqttTokenMock;
                });
        //WHEN
        AssertableSubscriber subscriber = pahoClientRxWrapSpy.unsubscribeFromTopic(topicMock).test();
        //THEN
        subscriber.assertCompleted();
        assertEquals(1,pahoClientRxWrapSpy.publishMutex.availablePermits());
        assertFalse(pahoClientRxWrapSpy.successfulSubscriptionTopics.contains(topicMock));
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_ClienteMqttConectadoDesuscripcionFallida_When_unsubscribeFromTopic_Then_EmitoError() throws MqttException {
        //GIVEN
        String topicMock= "Topic";
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        pahoClientRxWrapSpy.subscriptionMutex = new Semaphore(1);
        IMqttToken publIMqttTokenMock = mock(IMqttToken.class);
        Exception exception= new Exception();
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        when(mqttAsyncClientMock.unsubscribe(any(String.class),any(),any(IMqttActionListener.class)))
                .thenAnswer((Answer<IMqttToken>) invocation -> {
                    IMqttActionListener actionListener = invocation.getArgument(2);
                    actionListener.onFailure(publIMqttTokenMock,exception);
                    return publIMqttTokenMock;
                });
        //WHEN
        AssertableSubscriber subscriber = pahoClientRxWrapSpy.unsubscribeFromTopic(topicMock).test();
        //THEN
        subscriber.assertError(exception);
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_MqttException_When_unsubscribeFromTopic_Then_PropagaException() throws MqttException {
        //GIVEN
        String topicMock= "Topic";
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        MqttException mqttException= new MqttException(3200);
        when(mqttAsyncClientMock.unsubscribe(any(String.class), any(), any(IMqttActionListener.class))).thenThrow(mqttException);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.unsubscribeFromTopic(topicMock).test()
                .assertError(mqttException);
        //THEN
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_ClienteMqttDesconectado_When_subscribeToSeveralTopics_Then_PropagaExeption(){
        //GIVEN
        String[] topicMock= {"Topic"};
        int qMock= 1;
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(false);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        pahoClientRxWrapSpy.subscribeToSeveralTopics(topicMock,qMock).test()
                .assertError(Exception.class);
        //THEN
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_ClienteMqttConectadoSuscripcionExitosa_When_subscribeToSeveralTopics_Then_EmiteComplecion() throws MqttException {
        //GIVEN
        String[] topicMock= {"Topic"};
        int qMock= 1;
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        IMqttToken publIMqttTokenMock = mock(IMqttToken.class);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        when(mqttAsyncClientMock.subscribe(any(String[].class), any(int[].class),any(),any(IMqttActionListener.class)))
                .thenAnswer((Answer<IMqttToken>) invocation -> {
                    IMqttActionListener actionListener = invocation.getArgument(3);
                    actionListener.onSuccess(publIMqttTokenMock);
                    return publIMqttTokenMock;
                });
        //WHEN
        AssertableSubscriber subscriber = pahoClientRxWrapSpy.subscribeToSeveralTopics(topicMock,qMock).test();
        //THEN
        subscriber.assertCompleted();
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }


    @Test
    public void Given_ClienteMqttConectadoSuscripcionFallida_When_subscribeToSeveralTopics_Then_PropagaError() throws MqttException {
        //GIVEN
        String[] topicMock= {"Topic"};
        int qMock= 1;
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        IMqttToken publIMqttTokenMock = mock(IMqttToken.class);
        Exception exception= new Exception();
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        when(mqttAsyncClientMock.subscribe(any(String[].class), any(int[].class),any(),any(IMqttActionListener.class)))
                .thenAnswer((Answer<IMqttToken>) invocation -> {
                    IMqttActionListener actionListener = invocation.getArgument(3);
                    actionListener.onFailure(publIMqttTokenMock,exception);
                    return publIMqttTokenMock;
                });
        //WHEN
        AssertableSubscriber subscriber = pahoClientRxWrapSpy.subscribeToSeveralTopics(topicMock,qMock).test();
        //THEN
        subscriber.assertError(exception);
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }

    @Test
    public void Given_MqttExeption_When_subscribeToSeveralTopics_Then_PropagaExcepcion() throws MqttException {
        //GIVEN
        String[] topicMock= {"Topic"};
        int qMock= 1;
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        when(mqttAsyncClientMock.isConnected()).thenReturn(true);
        MqttException mqttException= new MqttException(123);
        when(mqttAsyncClientMock.subscribe(any(String[].class), any(int[].class),any(),any(IMqttActionListener.class))).thenThrow(mqttException);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        AssertableSubscriber subscriber = pahoClientRxWrapSpy.subscribeToSeveralTopics(topicMock,qMock).test();
        //THEN
        subscriber.assertError(mqttException);
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }

    @Test
    public void Given_DesuscripcionExitosa_When_unsubscribeFromAllTopics_Then_EmiteComplecion() throws MqttException {
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        IMqttToken publIMqttTokenMock = mock(IMqttToken.class);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        when(mqttAsyncClientMock.unsubscribe(any(String[].class), any(), any(IMqttActionListener.class)))
                .thenAnswer((Answer<IMqttToken>) invocation -> {
                    IMqttActionListener actionListener = invocation.getArgument(2);
                    actionListener.onSuccess(publIMqttTokenMock);
                    return publIMqttTokenMock;
                });
        //WHEN
        AssertableSubscriber subscriber = pahoClientRxWrapSpy.unsubscribeFromAllTopics().test();
        //THEN
        subscriber.assertCompleted();
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }

    @Test
    public void Given_DesuscripcionFallida_When_unsubscribeFromAllTopics_Then_PropagaError() throws MqttException {
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        IMqttToken publIMqttTokenMock = mock(IMqttToken.class);
        Exception exception= new Exception();
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        when(mqttAsyncClientMock.unsubscribe(any(String[].class), any(), any(IMqttActionListener.class)))
                .thenAnswer((Answer<IMqttToken>) invocation -> {
                    IMqttActionListener actionListener = invocation.getArgument(2);
                    actionListener.onFailure(publIMqttTokenMock,exception);
                    return publIMqttTokenMock;
                });
        //WHEN
        AssertableSubscriber subscriber = pahoClientRxWrapSpy.unsubscribeFromAllTopics().test();
        //THEN
        subscriber.assertError(exception);
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }

    @Test
    public void Given_MqttExeption_When_unsubscribeFromAllTopics_Then_PropagaException() throws MqttException {
        //GIVEN
        PahoClientRxWrap pahoClientRxWrapSpy = spy(pahoClientRxWrap);
        MqttException mqttException= new MqttException(123);
        when(mqttAsyncClientMock.unsubscribe(any(String[].class), any(), any(IMqttActionListener.class)))
                .thenThrow(mqttException);
        when(mSchedulerProviderMock.io()).thenReturn(Schedulers.immediate());
        //WHEN
        AssertableSubscriber subscriber = pahoClientRxWrapSpy.unsubscribeFromAllTopics().test();
        //THEN
        subscriber.assertError(mqttException);
        verifyZeroInteractions(mSchedulerProviderMock);
        verifyZeroInteractions(mqttAsyncClientMock);
    }
}