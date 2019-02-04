package com.urbit_iot.porton.data.source.internet;

import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import rx.Completable;
import rx.CompletableEmitter;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by andresteve07 on 26/10/18.
 */

public class PahoClientRxWrap implements MqttCallbackExtended{
    @NonNull
    private MqttAsyncClient mqttAsyncClient;

    private Set<String> requestedSubscriptionTopics;

    private Set<String> successfulSubscriptionTopics;

    private MqttConnectOptions mqttConnectOptions;

    @NonNull
    private PublishSubject<MqttMessageWrapper> messagesSubject;

    private Semaphore connectionMutex;

    private Semaphore publishMutex;

    private Semaphore subscriptionMutex;

    private PublishSubject<Boolean> subscriptionsKillerSubject;

    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    public PahoClientRxWrap(@NonNull MqttAsyncClient mqttAsyncClient,
                            @NonNull BaseSchedulerProvider mSchedulerProvider) {
        this.mqttAsyncClient = mqttAsyncClient;
        this.mSchedulerProvider = mSchedulerProvider;
        this.requestedSubscriptionTopics = new HashSet<>();
        this.successfulSubscriptionTopics = new HashSet<>();
        this.mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setMaxInflight(200);
        mqttConnectOptions.setConnectionTimeout(15);
        //Keep alive defaults to 60 secs
        this.messagesSubject = PublishSubject.create();
        this.connectionMutex = new Semaphore(1);
        this.publishMutex = new Semaphore(1);
        this.subscriptionMutex = new Semaphore(1);
        this.subscriptionsKillerSubject = PublishSubject.create();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        Log.d("PAHO_CALLBK","CONNECT__ URI: " + serverURI + "  RECONNECT: " + reconnect);
        if (!requestedSubscriptionTopics.isEmpty()){
            subscribeToSeveralTopics(requestedSubscriptionTopics.toArray(new String[0]),1)
                    .subscribeOn(mSchedulerProvider.io())//TODO use dagger injected scheduler
                    .retry(3L)
                    .subscribe(() -> Log.d("MQTT_WRAP", "Resubscription Success: "),
                            throwable -> Log.e("MQTT_WRAP", "Resubscription Error: " +
                                    throwable.getMessage() + " Type: " +
                                    throwable.getClass().getSimpleName()));
        }

    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.e("PAHO_CALLBK","DISCONNECT__ CAUSE: " + cause.getMessage() + " TYPE: " + cause.getClass().getSimpleName(),cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.d("PAHO_CALLBK","MESSAGE__ TOPIC: " + topic + "  MSG: " + message.toString());
        this.messagesSubject.onNext(new MqttMessageWrapper(topic, message));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d("PAHO_CALLBK","DELIVERY__ TOKEN: " + Arrays.toString(token.getTopics()));
    }
    public Observable<MqttMessageWrapper> receivedMessagesObservable(){
        return this.messagesSubject.asObservable();
    }

    public Completable connectToBroker() {
        return Completable.fromEmitter(completableEmitter -> {
            try {
                connectionMutex.acquire();
                Log.d("MQTT_SRV", "CONN___ LOCKED!! PERMITS: " + connectionMutex.availablePermits() + " ON: " + Thread.currentThread().getName());
                this.subscriptionsKillerSubject = PublishSubject.create();
                if (mqttAsyncClient.isConnected()){
                    Log.d("MQTT_SRV", "CONN___ SUCCESS!  ON: " + Thread.currentThread().getName());
                    connectionMutex.release();
                    completableEmitter.onCompleted();
                    return;
                }
                IMqttToken connectToken = mqttAsyncClient.connect(mqttConnectOptions);
                connectToken.waitForCompletion(5200L);
                Log.d("MQTT_SRV", "CONN___ SUCCESS!  ON: " + Thread.currentThread().getName());
                connectionMutex.release();
                completableEmitter.onCompleted();
            } catch (MqttException exception) {
                Log.e("MQTT_SRV", "CONN___ FAILURE: " + exception.getMessage() +
                        "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),exception);
                connectionMutex.release();
                switch (exception.getReasonCode()){
                    case MqttException.REASON_CODE_CLIENT_CONNECTED:
                        completableEmitter.onCompleted();
                        break;
                    default:
                        Log.e("MQTT_SRV", "CONN___ FAILURE: " + exception.getMessage() +
                                "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                                Thread.currentThread().getName(),exception);
                        completableEmitter.onError(exception);
                        break;
                }
            } catch (InterruptedException interrExc) {
                Log.e("MQTT_SRV", "CONN___ FAILURE: " + interrExc.getMessage() +
                        "TYPE: " + interrExc.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),interrExc);
                completableEmitter.onError(interrExc);
            }
        })
        .observeOn(mSchedulerProvider.io());
    }

    public Completable publishToTopic(byte[] messagePayload, String topic, int qos, boolean retained){
        return Completable.fromEmitter(completableEmitter -> {
            try {
                publishMutex.acquire();//Absolutely necessary to avoid calling .publish(...) concurrently
                Log.d("MQTT_SRV", "PUB___ LOCKED!! PERMITS: " + publishMutex.availablePermits() + " ON: " + Thread.currentThread().getName());
                if (!mqttAsyncClient.isConnected()){
                    Log.e("MQTT_SRV", "PUB___ FAILURE: " + "Client is Disconnected " +
                            " TYPE: " + "Custom type" + " ON: " + Thread.currentThread().getName());
                    publishMutex.release();
                    completableEmitter.onError(new Exception());
                    return;
                }
                Log.d("MQTT_SRV","PUB___ IN-FLIGHT MSGS COUNT: " + mqttAsyncClient.getInFlightMessageCount());//Will always fail on nullpointer why??
                IMqttToken publIMqttToken = mqttAsyncClient.publish(topic, messagePayload, qos, retained);
                if (qos == 1){//Workaround because paho isn't thread safe for qos 0
                    publishMutex.release();
                }
                publIMqttToken.waitForCompletion(8200L);
                if (qos == 0){//Workaround because paho isn't thread safe for qos 0
                    publishMutex.release();
                }
                Log.d("MQTT_SRV", "PUB___ SUCCESS!  ON: " + Thread.currentThread().getName());

                completableEmitter.onCompleted();
            } catch (MqttException exception) {
                Log.e("MQTT_SRV", "PUB___ FAILURE: " + exception.getMessage() +
                        "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),exception);
                publishMutex.release();
                completableEmitter.onError(exception);
            } catch (InterruptedException interrExc) {
                Log.e("MQTT_SRV", "PUB___ FAILURE: " + interrExc.getMessage() +
                        "TYPE: " + interrExc.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),interrExc);
            }
        })
        .observeOn(mSchedulerProvider.io());
    }

    public void addRequestedSubscriptionTopic(String topic){
        subscriptionMutex.acquireUninterruptibly();
        requestedSubscriptionTopics.add(topic);
        subscriptionMutex.release();
    }
    public Completable subscribeToTopic(String topic, int qos){
        return subscribeToTopic(topic,qos,true);
    }

    public Completable subscribeToTopic(String topic, int qos, boolean storeTopicForResubscription){
        return Completable.fromEmitter(completableEmitter -> {
            try {
                subscriptionMutex.acquireUninterruptibly();
                if (storeTopicForResubscription){
                    requestedSubscriptionTopics.add(topic);
                }
                subscriptionMutex.release();
                if (!mqttAsyncClient.isConnected()){
                    Log.e("MQTT_SRV", "SUB___ FAILURE TOPIC: " + topic +  "Client is Disconnected" +
                            "TYPE: " + "Custom type" + "ON: " + Thread.currentThread().getName());
                    completableEmitter.onError(new Exception());
                    return;
                }
                /*
                if (requestedSubscriptionTopics.contains(topic)){
                    completableEmitter.onCompleted();
                    return;
                }
                */
                mqttAsyncClient.subscribe(topic, qos, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d("MQTT_SRV", "SUB___ SUCCESS! TOPIC: " + topic +  " ON: " + Thread.currentThread().getName());
                        subscriptionMutex.acquireUninterruptibly();
                        successfulSubscriptionTopics.add(topic);
                        subscriptionMutex.release();
                        completableEmitter.onCompleted();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("MQTT_SRV", "SUB___ FAILURE TOPIC: " + topic + " CAUSE: " + exception.getMessage() +
                                "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                                Thread.currentThread().getName(),exception);
                        completableEmitter.onError(exception);
                    }
                });
            } catch (MqttException exception) {
                Log.e("MQTT_SRV", "SUB___ FAILURE TOPIC: " + topic +" CAUSE: " + exception.getMessage() +
                        "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),exception);
                completableEmitter.onError(exception);
            }
        })
        .observeOn(mSchedulerProvider.io());
    }

    public Completable unsubscribeFromTopic(String topic){
        return Completable.fromEmitter((CompletableEmitter completableEmitter) -> {
            try {
                if (!mqttAsyncClient.isConnected()){
                    Log.e("MQTT_SRV", "UNSUB___ FAILURE TOPIC: " + topic +  "Client is Disconnected" +
                            "TYPE: " + "Custom type" + "ON: " + Thread.currentThread().getName());
                    completableEmitter.onError(new Exception("Client is Disconnected"));
                    return;
                }
                mqttAsyncClient.unsubscribe(topic, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d("MQTT_SRV", "UNSUB___ SUCCESS! TOPIC: " + topic +  " ON: " + Thread.currentThread().getName());
                        subscriptionMutex.acquireUninterruptibly();
                        successfulSubscriptionTopics.add(topic);
                        subscriptionMutex.release();
                        completableEmitter.onCompleted();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("MQTT_SRV", "UNSUB___ FAILURE TOPIC: " + topic + " CAUSE: " + exception.getMessage() +
                                "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                                Thread.currentThread().getName(),exception);
                        completableEmitter.onError(exception);
                    }
                });
            } catch (MqttException exception) {
                Log.e("MQTT_SRV", "UNSUB___ FAILURE TOPIC: " + topic +" CAUSE: " + exception.getMessage() +
                        "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),exception);
                completableEmitter.onError(exception);
            }
        })
                .observeOn(mSchedulerProvider.io());
    }

    public Completable subscribeToSeveralTopics(String[] topics, int qos){
        return subscribeToSeveralTopics(topics,qos,true);
    }
    public Completable subscribeToSeveralTopics(String[] topics, int qos, boolean storeTopicsForResubscription){
        return Completable.fromEmitter(completableEmitter -> {
            try {
                subscriptionMutex.acquireUninterruptibly();
                if (storeTopicsForResubscription){
                    requestedSubscriptionTopics.addAll(Arrays.asList(topics));
                }
                subscriptionMutex.release();
                if (!mqttAsyncClient.isConnected()){
                    Log.e("MQTT_SRV", "SUB-MANY___ FAILURE!  ON: " + Thread.currentThread().getName());
                    completableEmitter.onError(new Exception());
                    return;
                }
                /*
                if (requestedSubscriptionTopics.contains(topic)){
                    completableEmitter.onCompleted();
                    return;
                }
                */
                int[] qosArray = new int[topics.length];
                Arrays.fill(qosArray, qos);
                mqttAsyncClient.subscribe(topics, qosArray, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d("MQTT_SRV", "SUB-MANY___ SUCCESS!  ON: " + Thread.currentThread().getName());
                        subscriptionMutex.acquireUninterruptibly();
                        successfulSubscriptionTopics.addAll(Arrays.asList(topics));
                        subscriptionMutex.release();
                        completableEmitter.onCompleted();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("MQTT_SRV", "SUB-MANY___ FAILURE: " + exception.getMessage() +
                                "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                                Thread.currentThread().getName(),exception);
                        completableEmitter.onError(exception);
                    }
                });
            } catch (MqttException exception) {
                Log.e("MQTT_SRV", "SUB-MANY___ FAILURE: " + exception.getMessage() +
                        "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),exception);
                completableEmitter.onError(exception);
            }
        })
                .observeOn(mSchedulerProvider.io());
    }

    public Completable unsubscribeFromAllTopics(){
        return Completable.fromEmitter(completableEmitter -> {
            try {
                mqttAsyncClient.unsubscribe(this.requestedSubscriptionTopics.toArray(new String[0]),
                        null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d("MQTT_SRV", "SUB-ALL___ SUCCESS!  ON: " + Thread.currentThread().getName());
                        completableEmitter.onCompleted();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("MQTT_SRV", "SUB-ALL___ FAILURE: " + exception.getMessage() +
                                "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                                Thread.currentThread().getName(),exception);
                        completableEmitter.onError(exception);
                    }
                });
            } catch (MqttException exception) {
                Log.e("MQTT_SRV", "SUB-ALL___ FAILURE: " + exception.getMessage() +
                        "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),exception);
                completableEmitter.onError(exception);
            }
        })
        .observeOn(mSchedulerProvider.io());
    }
}
