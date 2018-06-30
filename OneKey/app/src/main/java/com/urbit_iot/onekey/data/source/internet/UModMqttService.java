package com.urbit_iot.onekey.data.source.internet;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.rpc.RPC;

import net.eusashead.iot.mqtt.MqttMessage;
import net.eusashead.iot.mqtt.ObservableMqttClient;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Maybe;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import rx.Observable;


/**
 * Created by andresteve07 on 06/04/18.
 */

public class UModMqttService {

    @NonNull
    private ObservableMqttClient mMqttClient;
    @NonNull
    private Gson gsonInstance;
    @NonNull
    private String userName;

    private PublishProcessor<MqttMessage> receivedMessagesProcessor;

    //private boolean subscribedToUModResponseTopic;

    @Inject
    public UModMqttService(@NonNull ObservableMqttClient mMqttClient,
                           @NonNull Gson gsonInstance,
                           @NonNull String userName) {
        this.mMqttClient = mMqttClient;
        this.gsonInstance = gsonInstance;
        this.userName = userName;
        this.receivedMessagesProcessor = PublishProcessor.create();

        mMqttClient.connect()
                .doOnComplete(this::subscribeToResponseTopic)
                .subscribe(() -> Log.d("umodsMqttService","CONNECTED!"),
                        throwable -> Log.e("umodsMqttService","FAILED TO CONNECT" + throwable.getMessage()));

    }

    public void subscribeToUModRequestsTopic(String uModTopic){
        mMqttClient.subscribe(uModTopic,2)
                .subscribeOn(Schedulers.io())
                .subscribe(mqttMessage -> receivedMessagesProcessor.onNext(mqttMessage));
    }

    private void subscribeToResponseTopic(){
        mMqttClient.subscribe(this.userName + "/response",2)
                .subscribeOn(Schedulers.io())
                .subscribe(mqttMessage -> {
                    Log.d("subscribeTopic", "" + mqttMessage.getId() + new String(mqttMessage.getPayload()));
                    receivedMessagesProcessor.onNext(mqttMessage);
                });
    }

    public void subscribeToUModResponseTopic(UMod umod, String userName){
        mMqttClient.connect()
                .andThen(mMqttClient.subscribe("urbit_" + umod.getUUID() + "/response/" + userName,2))
                .subscribeOn(Schedulers.io())
                .subscribe(mqttMessage -> {
                    Log.d("subscribeTopic", "" + mqttMessage.getId() + new String(mqttMessage.getPayload()));
                    receivedMessagesProcessor.onNext(mqttMessage);
                });
    }


    public <T,S> Observable<S> publishRPC(String requestTopic, T request, Class<S> responseType){
        String serializedRequest = gsonInstance.toJson(request);
        Log.d("publishRPC", "Serialized Request: " + serializedRequest);
        short mqttMessageId = (short) new Random().nextInt();
        //short mqttMessageId = (short) 123456;
        MqttMessage requestMqttMessage = MqttMessage.create(mqttMessageId,serializedRequest.getBytes(),2,false);
        //TODO Note: this implementation assumes that a SINGLE response will arrive within 2 seconds after the request was published.
        //It should contemplate the arrival of foreign and duplicated messages...Queuing is deemed necessary...???
        Maybe<S> maybeResponse = receivedMessagesProcessor
                //.doOnNext(mqttMessage -> Log.d("publishRPC", "" + mqttMessage.getId() + new String(mqttMessage.getPayload())))
                .map(mqttMessage -> gsonInstance.fromJson(new String(mqttMessage.getPayload()),responseType))
                //.doOnNext(s -> Log.d("publishRPC", s.toString()))
                .filter(response -> ((RPC.Response)response).getResponseId() == ((RPC.Request)request).getRequestId()
                        && ((RPC.Response)response).getRequestTag().equalsIgnoreCase(((RPC.Request)request).getRequestTag()))
                .timeout(2000L, TimeUnit.MILLISECONDS)
                .firstElement();
        Maybe<S> maybeRequestAndResponse = mMqttClient.publish(requestTopic, requestMqttMessage)
                .flatMapMaybe(publishToken -> maybeResponse);

        Maybe<S> maybeConditionalConnection = Maybe.just(this.mMqttClient.isConnected())
                .flatMap(connectionWithBrokerIsAlive -> {
                    if (connectionWithBrokerIsAlive){
                        return maybeRequestAndResponse;
                    } else {
                        return mMqttClient.connect()
                                .doOnComplete(this::subscribeToResponseTopic)//TODO Q: is resubscribing necessary??
                                .andThen(maybeRequestAndResponse);
                    }
                });
        return RxJavaInterop.toV1Single(maybeConditionalConnection).toObservable();
    }
}
/*

public Maybe<RPC.Response> mqttRPCExecution(UMod uMod, RPC.Request request){
        String serializedRequest = gsonInstance.toJson(request);
        MqttMessage message = MqttMessage.create(543874623,serializedRequest.getBytes(),2,false);
        //TODO Note: this implementation assumes that a SINGLE response will arrive within 2 seconds after the request was published.
        //It should contemplate the arrival of foreign and duplicated messages...Queuing is deemed necessary...???
        Maybe<RPC.Response> maybeResponse = receivedMessagesProcessor
                .map(mqttMessage -> gsonInstance.fromJson(message.getPayload().toString(),RPC.Response.class))
                .filter(response -> response.getResponseId() == request.getRequestId())
                .timeout(2000L, TimeUnit.MILLISECONDS)
                .firstElement();
        //maybeResponse.toObservable();
        Maybe<RPC.Response> maybeRequestAndResponse = mMqttClient.publish("dsf", message)
                .flatMapMaybe(publishToken -> maybeResponse);

        return Maybe.just(this.mMqttClient.isConnected())
                .flatMap(connectionWithBrokerIsAlive -> {
                    if (connectionWithBrokerIsAlive){
                        return maybeRequestAndResponse;
                    } else {
                        return mMqttClient.connect()
                                .andThen(maybeRequestAndResponse);
                    }
                });
    }

    public Observable<TriggerRPC.Response> publishTrigger(String uModUUID, TriggerRPC.Request request){
        String serializedRequest = gsonInstance.toJson(request);
        MqttMessage message = MqttMessage.create(543874623,serializedRequest.getBytes(),2,false);
        //TODO Note: this implementation assumes that a SINGLE response will arrive within 2 seconds after the request was published.
        //It should contemplate the arrival of foreign and duplicated messages...Queuing is deemed necessary...???
        Maybe<TriggerRPC.Response> maybeResponse = receivedMessagesProcessor
                .map(mqttMessage -> gsonInstance.fromJson(message.getPayload().toString(),TriggerRPC.Response.class))
                .filter(response -> response.getResponseId() == request.getRequestId())
                .timeout(2000L, TimeUnit.MILLISECONDS)
                .firstElement();
        //maybeResponse.toObservable();
        Maybe<TriggerRPC.Response> maybeRequestAndResponse = mMqttClient.publish("urbit-243451/request", message)
                .flatMapMaybe(publishToken -> maybeResponse);

        return Maybe.just(this.mMqttClient.isConnected())
                .flatMap(connectionWithBrokerIsAlive -> {
                    if (connectionWithBrokerIsAlive){
                        return maybeRequestAndResponse;
                    } else {
                        return mMqttClient.connect()
                                .andThen(maybeRequestAndResponse);
                    }
                })
                .toObservable();
    }

 */