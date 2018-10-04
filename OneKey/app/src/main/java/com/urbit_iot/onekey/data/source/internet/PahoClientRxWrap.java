package com.urbit_iot.onekey.data.source.internet;

import android.support.annotation.NonNull;
import android.util.Log;

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
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by andresteve07 on 26/10/18.
 */

public class PahoClientRxWrap implements MqttCallbackExtended{
    @NonNull
    private MqttAsyncClient mqttAsyncClient;

    private Set<String> subscribedTopics;

    private MqttConnectOptions mqttConnectOptions;

    @NonNull
    private PublishSubject<MqttMessage> messagesSubject;

    private Semaphore clientMutex;

    private Semaphore publishMutex;

    public PahoClientRxWrap(@NonNull MqttAsyncClient mqttAsyncClient) {
        this.mqttAsyncClient = mqttAsyncClient;
        this.subscribedTopics = new HashSet<>();
        this.mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setMaxInflight(200);
        this.messagesSubject = PublishSubject.create();
        this.clientMutex = new Semaphore(1);
        this.publishMutex = new Semaphore(1);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        Log.d("PAHO_CALLBK","CONNECT__ URI: " + serverURI + "  RECONNECT: " + reconnect);
        if (!subscribedTopics.isEmpty()){
            subscribeToSeveralTopics(subscribedTopics.toArray(new String[0]),1)
                    .subscribeOn(Schedulers.io())//TODO use dagger injected scheduler
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
        this.messagesSubject.onNext(message);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d("PAHO_CALLBK","DELIVERY__ TOKEN: " + Arrays.toString(token.getTopics()));
    }
    public Observable<MqttMessage> receivedMessagesObservable(){
        return this.messagesSubject.asObservable();
    }

    public Completable connectToBroker() {
        return Completable.fromEmitter(completableEmitter -> {
            try {
                clientMutex.acquireUninterruptibly();
                Log.d("MQTT_SRV", "CONN___ LOCKED!!  ON: " + Thread.currentThread().getName());
                if (mqttAsyncClient.isConnected()){
                    Log.d("MQTT_SRV", "CONN___ SUCCESS!  ON: " + Thread.currentThread().getName());
                    completableEmitter.onCompleted();
                    return;
                }
                mqttAsyncClient.connect(mqttConnectOptions, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d("MQTT_SRV", "CONN___ SUCCESS!  ON: " + Thread.currentThread().getName());
                        completableEmitter.onCompleted();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("MQTT_SRV", "CONN___ FAILURE: " + exception.getMessage() +
                                "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                                Thread.currentThread().getName(),exception);
                        completableEmitter.onError(exception);
                    }
                });
            } catch (MqttException exception) {
                Log.e("MQTT_SRV", "PUB___ FAILURE: " + exception.getMessage() +
                        "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),exception);
                switch (exception.getReasonCode()){
                    case MqttException.REASON_CODE_CLIENT_CONNECTED:
                        completableEmitter.onCompleted();
                        break;
                    default:
                        Log.e("MQTT_SRV", "PUB___ FAILURE: " + exception.getMessage() +
                                "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                                Thread.currentThread().getName(),exception);
                        completableEmitter.onError(exception);
                        break;
                }
            }
        })
        .observeOn(Schedulers.io())
        .doOnTerminate(() -> {
            Log.d("MQTT_SRV", "CONN___ FINALLY + UNLOCKING!!  ON: " + Thread.currentThread().getName());
            //Log.d("MQTT_SRV","CONN___ IN-FLIGHT BUFFER SIZE: " + mqttAsyncClient.getBufferedMessageCount());
            clientMutex.release();
        });
        /*
        .retryWhen(observable -> observable.flatMap(throwable -> {
            if (((MqttException) throwable).getReasonCode() == MqttException.REASON_CODE_CONNECT_IN_PROGRESS) {
                return Observable.timer(1200L, TimeUnit.MILLISECONDS);
            }
            return Observable.error(throwable);
        }));*/
    }

    public Completable publishToTopic(byte[] messagePayload, String topic, int qos, boolean retained){
        return Completable.fromEmitter(completableEmitter -> {
            try {
                publishMutex.acquireUninterruptibly();//Absolutely necessary to avoid calling .publish(...) concurrently
                Log.d("MQTT_SRV","PUB___ IN-FLIGHT MSGS COUNT: " + mqttAsyncClient.getInFlightMessageCount());//Will always fail on nullpointer why??
                mqttAsyncClient.publish(topic, messagePayload, qos, retained, null,
                        new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d("MQTT_SRV", "PUB___ SUCCESS!  ON: " + Thread.currentThread().getName());
                        completableEmitter.onCompleted();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("MQTT_SRV", "PUB___ FAILURE: " + exception.getMessage() +
                                "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                                Thread.currentThread().getName(),exception);
                        completableEmitter.onError(exception);
                    }
                });
            } catch (MqttException exception) {
                Log.e("MQTT_SRV", "PUB___ FAILURE: " + exception.getMessage() +
                        "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),exception);
                completableEmitter.onError(exception);
            }
        })
        .observeOn(Schedulers.io())
        .doOnTerminate(() -> publishMutex.release());
    }

    public Completable subscribeToTopic(String topic, int qos){
        return Completable.fromEmitter((CompletableEmitter completableEmitter) -> {
            try {
                //clientMutex.acquireUninterruptibly();
                //Log.d("MQTT_SERVICE", "SUB___ LOCKED!!  " + Thread.currentThread().getName());
                if (!mqttAsyncClient.isConnected()){
                    Log.e("MQTT_SRV", "SUB___ FAILURE: " + "Client is Disconnected" +
                            "TYPE: " + "Custom type" + "ON: " + Thread.currentThread().getName());
                    completableEmitter.onError(new Exception());
                    return;
                }
                /*
                if (subscribedTopics.contains(topic)){
                    completableEmitter.onCompleted();
                    return;
                }
                */
                mqttAsyncClient.subscribe(topic, qos, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d("MQTT_SRV", "SUB___ SUCCESS!  ON: " + Thread.currentThread().getName());
                        completableEmitter.onCompleted();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e("MQTT_SRV", "SUB___ FAILURE: " + exception.getMessage() +
                                "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                                Thread.currentThread().getName(),exception);
                        completableEmitter.onError(exception);
                    }
                });
            } catch (MqttException exception) {
                Log.e("MQTT_SRV", "SUB___ FAILURE: " + exception.getMessage() +
                        "TYPE: " + exception.getClass().getSimpleName() + "ON: " +
                        Thread.currentThread().getName(),exception);
                completableEmitter.onError(exception);
            }
        })
                .observeOn(Schedulers.io())
                .doOnTerminate(() -> subscribedTopics.add(topic))
                /*
        .doOnTerminate(() -> {
            Log.d("MQTT_SERVICE", "SUB___  FINALLY + UNLOCKING!!  " + Thread.currentThread().getName());
            clientMutex.release();
        })
        */
        ;
    }

    public Completable subscribeToSeveralTopics(String[] topics, int qos){
        return Completable.fromEmitter(completableEmitter -> {
            try {
                //clientMutex.acquireUninterruptibly();
                //Log.d("MQTT_SERVICE", "SUB___ LOCKED!!  " + Thread.currentThread().getName());
                if (!mqttAsyncClient.isConnected()){
                    Log.d("MQTT_SRV", "SUB-MANY___ SUCCESS!  ON: " + Thread.currentThread().getName());
                    completableEmitter.onError(new Exception());
                    return;
                }
                /*
                if (subscribedTopics.contains(topic)){
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
                .observeOn(Schedulers.io())
                .doOnCompleted(() -> subscribedTopics = new HashSet<>(Arrays.asList(topics)))
                /*
        .doOnTerminate(() -> {
            Log.d("MQTT_SERVICE", "SUB___  FINALLY + UNLOCKING!!  " + Thread.currentThread().getName());
            clientMutex.release();
        })
        */
                ;
    }

    public Completable unsubscribeFromAllTopics(){
        return Completable.fromEmitter(completableEmitter -> {
            try {
                mqttAsyncClient.unsubscribe(this.subscribedTopics.toArray(new String[0]),
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
        .observeOn(Schedulers.io());
    }
}
