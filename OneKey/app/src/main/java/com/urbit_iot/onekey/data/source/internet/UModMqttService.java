package com.urbit_iot.onekey.data.source.internet;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.rpc.RPC;
import com.urbit_iot.onekey.util.GlobalConstants;

import net.eusashead.iot.mqtt.MqttMessage;
import net.eusashead.iot.mqtt.ObservableMqttClient;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.HttpException;
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

    private Map<String, Disposable> subscriptionsMap;

    private CompositeDisposable allSubscriptions;

    private Lock clientStateCheckLock;

    private Semaphore clientMutex;


    //private boolean subscribedToUModResponseTopic;

    @Inject
    public UModMqttService(@NonNull ObservableMqttClient mMqttClient,
                           @NonNull Gson gsonInstance,
                           @NonNull String userName) {
        this.mMqttClient = mMqttClient;
        this.gsonInstance = gsonInstance;
        this.userName = userName;
        this.receivedMessagesProcessor = PublishProcessor.create();
        this.subscriptionsMap = new LinkedHashMap<>();
        this.allSubscriptions = new CompositeDisposable();
        this.clientStateCheckLock = new ReentrantLock();
        this.clientMutex = new Semaphore(1);

        connectMqttClient()
                //.observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())//TODO use injected scheduler
                .subscribe(() -> Log.d("umodsMqttService","CONNECTED!"),
                        throwable -> Log.e("umodsMqttService","FAILED TO CONNECT  " + throwable.getClass().getSimpleName() + " MSG: " + throwable.getMessage()));

    }

    private Completable connectMqttClient(){
        return Completable.defer(() ->
                Single.just(1)
                        //.observeOn(Schedulers.single())
                    .flatMapCompletable(integer -> {
                        clientMutex.acquireUninterruptibly();
                        Log.d("MQTT_SERVICE", "LOCKED!!  " + Thread.currentThread().getName());
                        if (mMqttClient.isConnected()){
                            //return Completable.complete();
                            return testConnectionToBroker();//In the case the client isn't aware of disconnection yet.
                        } else {
                            return testConnectionToBroker()
                                    .andThen(mMqttClient.connect())
                                    .andThen(Completable.fromAction(() -> {
                                        Log.d("MQTT_SERVICE", "ACTUAL CONNECTION COMPLETED " + Thread.currentThread().getName());
                                        resubscribeToAllUMods();
                                    }));
                        }
                    })
                    .doOnComplete(() -> {
                        Log.d("MQTT_SERVICE", "CONNECTION COMPLETE ON: " + Thread.currentThread().getName());
                    })
                    .doOnError((throwable) -> {
                        Log.d("MQTT_SERVICE", "CONNECTION ERROR: " + throwable.getClass().getSimpleName() +" ON: "+ Thread.currentThread().getName());
                    })
                    .doFinally(() -> {
                        Log.d("MQTT_SERVICE", "CONNECTION FINALLY + UNLOCKING!!  " + Thread.currentThread().getName());
                        clientMutex.release();
                    })
        );

    }

    public void resubscribeToAllUMods(){
        if (subscriptionsMap.isEmpty()){
            return;
        }
        Set<String> umodsSubscribedTo = new HashSet<>(subscriptionsMap.keySet());
        Log.d("MQTT_SERVICE", "RESUBSCIRPTION: "
                + umodsSubscribedTo.size()
                + " UMODS. ON: "
                + Thread.currentThread().getName());
        this.clearAllSubscriptions();
        for (String uModUUID : umodsSubscribedTo){
            this.subscribeToUModResponseTopic(uModUUID);
        }
    }

    public void subscribeToUModResponseTopic(String uModUUID){
        String responseTopicForUMod = GlobalConstants.URBIT_PREFIX + uModUUID + "/response/" + this.userName;

        Disposable oldSubscription = this.subscriptionsMap.get(uModUUID);

        if (oldSubscription != null){
            if (!oldSubscription.isDisposed()){
                //oldSubscription.dispose();
                return;
            }
            this.allSubscriptions.delete(oldSubscription);
            this.subscriptionsMap.remove(uModUUID);
        }
        //clientMutex.release();
        Flowable<MqttMessage> topicMessagesFlowable = Flowable.defer(() -> mMqttClient.subscribe(responseTopicForUMod,1));
        Disposable topicSubDisposable = connectMqttClient()
                //.observeOn(Schedulers.io())
                .andThen(topicMessagesFlowable)
                .subscribeOn(Schedulers.io())
                .subscribe(mqttMessage -> {
                            Log.d("MQTT_SUBSCRIBER", "MSG_ID: "
                                    + mqttMessage.getId()
                                    +" ON: "
                                    +Thread.currentThread().getName()
                                    + " PAYLOAD: "
                                    + new String(mqttMessage.getPayload()));
                            receivedMessagesProcessor.onNext(mqttMessage);
                        },
                        throwable -> {
                            Log.e("MQTT_SUBSCRIBER", throwable.getMessage() + " ON: " + Thread.currentThread().getName());
                        },
                        () -> {
                            Log.d("MQTT_SUBSCRIBER", "Response Topic Sub Completed.");
                        });
        this.subscriptionsMap.put(uModUUID, topicSubDisposable);
        //Log.d("MQTT_SERVICE", "COMPOSITE DISPOSED: " + allSubscriptions.isDisposed());
        this.allSubscriptions.add(topicSubDisposable);
    }

    synchronized public void subscribeToUModResponseTopic(UMod umod){
       this.subscribeToUModResponseTopic(umod.getUUID());
    }

    public List<String> getListOfSubscribedUModsUUIDs(){
        return new ArrayList<>(subscriptionsMap.keySet());
    }


    public <T,S> Observable<S> publishRPC(String requestTopic, T request, Class<S> responseType){
        String serializedRequest = gsonInstance.toJson(request);
        Log.d("publishRPC", "Serialized Request: " + serializedRequest);
        short mqttMessageId = (short) new Random().nextInt();
        //short mqttMessageId = (short) 123456;
        MqttMessage requestMqttMessage = MqttMessage.create(mqttMessageId,serializedRequest.getBytes(),0,false);
        //TODO Note: this implementation assumes that a SINGLE response will arrive within 2 seconds after the request was published.
        //It should contemplate the arrival of foreign and duplicated messages...Queuing is deemed necessary...???
        Maybe<S> maybeResponse = receivedMessagesProcessor
                //.doOnNext(mqttMessage -> Log.d("publishRPC", "" + mqttMessage.getId() + new String(mqttMessage.getPayload())))
                .map(mqttMessage -> gsonInstance.fromJson(new String(mqttMessage.getPayload()),responseType))
                //.doOnNext(s -> Log.d("publishRPC", s.toString()))
                .filter(response -> ((RPC.Response)response).getResponseId() == ((RPC.Request)request).getRequestId()
                           && ((RPC.Response)response).getRequestTag().equalsIgnoreCase(((RPC.Request)request).getRequestTag()))
                .timeout(6000L, TimeUnit.MILLISECONDS)
                .firstElement()
                .flatMap(response -> {
                    RPC.ResponseError responseError = ((RPC.Response)response).getResponseError();
                    if (responseError!=null){
                        Integer errorCode = responseError.getErrorCode();
                        if (errorCode!= HttpURLConnection.HTTP_UNAUTHORIZED
                                && errorCode!=HttpURLConnection.HTTP_FORBIDDEN){
                            errorCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
                        }
                        HttpException httpException = new HttpException(
                                Response.error(
                                        errorCode,
                                        ResponseBody.create(
                                                MediaType.parse("text/plain"),
                                                gsonInstance.toJson(responseError))
                                )
                        );
                        return Maybe.error(httpException);
                    }
                    return Maybe.just(response);
                });
        Maybe<S> maybeRequestAndResponse = mMqttClient.publish(requestTopic, requestMqttMessage)
                .flatMapMaybe(publishToken -> maybeResponse);

        Maybe<S> maybeConditionalConnection = connectMqttClient().andThen(maybeRequestAndResponse);

        return RxJavaInterop.toV1Single(maybeConditionalConnection).toObservable();
    }

    public void clearAllSubscriptions() {
        //this.allSubscriptions.dispose();
        this.allSubscriptions.clear();
        this.subscriptionsMap.clear();
    }

    public void reconnectToBroker(){
        Completable reconnectionCompletable;
        if (this.mMqttClient.isConnected()){
            reconnectionCompletable = this.mMqttClient.disconnect().andThen(this.mMqttClient.connect());
        } else {
            reconnectionCompletable = this.mMqttClient.connect();
        }
        reconnectionCompletable
                .subscribeOn(Schedulers.io())
                .subscribe(
                () -> Log.d("MQTT_SERVICE", "Reconnection Success"),
                throwable -> Log.e("MQTT_SERVICE", "Reconnection Failure",throwable));
    }

    public Completable testConnectionToBroker(){
        return Completable.fromAction(() -> {
            Socket clientSocket;
            clientSocket = new Socket();
            try{
                clientSocket.connect(
                        new InetSocketAddress(
                                GlobalConstants.MQTT_BROKER__IP_ADDRESS,
                                GlobalConstants.MQTT_BROKER__PORT),
                        1500);
            } finally {
                clientSocket.close();
            }
        });
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