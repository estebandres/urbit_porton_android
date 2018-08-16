package com.urbit_iot.onekey.data.source.internet;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.RPC;
import com.urbit_iot.onekey.util.GlobalConstants;

import net.eusashead.iot.mqtt.MqttMessage;
import net.eusashead.iot.mqtt.ObservableMqttClient;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
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

    private Lock lastReconnectMutex;

    private Semaphore clientMutex;

    private Date lastReconnectionAttempt;

    private PublishProcessor<Boolean> reconnectionAttemptProcessor;

    private CompletableEmitter reconnectionEmmiter;

    private Disposable reconnectDisposable;


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
        this.lastReconnectMutex = new ReentrantLock();
        this.clientMutex = new Semaphore(1);
        this.reconnectionAttemptProcessor = PublishProcessor.create();

        connectMqttClient()
                //.observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())//TODO use injected scheduler
                .subscribe(() -> Log.d("umodsMqttService","CONNECTED!"),
                        throwable -> Log.e("umodsMqttService","FAILED TO CONNECT  " + throwable.getClass().getSimpleName() + " MSG: " + throwable.getMessage()));
        /*
        this.cancelUModInvitation("543874623893", "666666")
                .subscribeOn(rx.schedulers.Schedulers.io())
                .subscribe(() -> Log.d("UMOD_SERVICE_TEST","CANCELLED"),
                        throwable -> Log.e("UMOD_SERVICE_TEST","CANCELLED",throwable));
        */

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
                                    .ambWith(Completable.timer(2000L,TimeUnit.MILLISECONDS))
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

    synchronized public void reconnectionAttempt(){
        Log.d("MQTT_SERVICE",
                "RECONNECTION ATTEMPT ON: "
                        + Thread.currentThread().getName());
        this.reconnectToBroker();
        this.reconnectionAttemptProcessor.onNext(true);
    }

    private void reconnectToBroker(){
        if (this.reconnectDisposable != null
                && !this.reconnectDisposable.isDisposed()){
            return;
        }

        Completable reconnectionCompletable = Single.just(1).flatMapCompletable(integer -> {
            Log.d("MQTT_SERVICE",
                    "RECONNECTION TRYING LOCK!!  "
                            + Thread.currentThread().getName());
            clientMutex.acquireUninterruptibly();
            Log.d("MQTT_SERVICE",
                    "RECONNECTION LOCKED!!  "
                            + Thread.currentThread().getName());
            if (mMqttClient.isConnected()){
                //In the case the client isn't aware of the disconnection yet.
                return testConnectionToBroker()
                        .andThen(Completable.fromAction(this::resubscribeToAllUMods));
            } else {
                return testConnectionToBroker()
                        .andThen(mMqttClient.connect())
                        .ambWith(Completable.timer(2000L,TimeUnit.MILLISECONDS))
                        .andThen(Completable.fromAction(() -> {
                            Log.d("MQTT_SERVICE",
                                    "ACTUAL RECONNECTION COMPLETED "
                                            + Thread.currentThread().getName());
                            resubscribeToAllUMods();
                        }));
            }
        })
        .doOnError((throwable) ->
                Log.d("MQTT_SERVICE", "RECONNECTION ERROR: "
                + throwable.getClass().getSimpleName()
                +" ON: "
                + Thread.currentThread().getName()))
        .doFinally(() -> {
            Log.d("MQTT_SERVICE",
                    "RECONNECTION FINALLY + UNLOCKING!!  "
                            + Thread.currentThread().getName());
            clientMutex.release();
        });


        this.reconnectDisposable = this.reconnectionAttemptProcessor
                //.debounce(1500L,TimeUnit.MILLISECONDS)
                .throttleFirst(4000L,TimeUnit.MILLISECONDS)
                .flatMap(aBoolean -> reconnectionCompletable.toFlowable())
                .subscribeOn(Schedulers.io())
                .subscribe(o -> {},throwable -> {
                    Log.d("MQTT_SERVICE", "RECONNECTION ERROR ON: "
                            + Thread.currentThread().getName()
                            +"\n EXCEPTION: "
                            + throwable.getClass().getSimpleName(), throwable);
                },() -> {
                    Log.d("MQTT_SERVICE", "RECONNECTION COMPLETED ON: "
                            + Thread.currentThread().getName() );
                });
                //.take(1)
                //.firstElement()
                //.flatMapCompletable(aBoolean -> Completable.complete())
                /*
                .subscribe(boolean1 -> Log.d("MQTT_SERVICE",
                "RECONNECTION TRYING LOCK!!  "
                        + Thread.currentThread().getName()),
                        throwable -> Log.d("MQTT_SERVICE",
                                "RECONNECTION ERROR: "
                                        + throwable.getClass().getSimpleName()
                                        +" ON: "
                                        + Thread.currentThread().getName()), () -> Log.d("MQTT_SERVICE",
                                "RECONNECTION COMPLEEEEETEEEEE!!! ON: "
                                        + Thread.currentThread().getName()));
                                        /*
        /*
        this.reconnectDisposable = Completable.defer(() -> this.reconnectionAttemptProcessor
                .debounce(1500L,TimeUnit.MILLISECONDS)
                .firstElement()
                .flatMapCompletable(aBoolean -> {
                    Log.d("MQTT_SERVICE",
                            "RECONNECTION TRYING LOCK!!  "
                                    + Thread.currentThread().getName());
                    clientMutex.acquireUninterruptibly();
                    Log.d("MQTT_SERVICE",
                            "RECONNECTION LOCKED!!  "
                                    + Thread.currentThread().getName());
                    if (mMqttClient.isConnected()){
                        //In the case the client isn't aware of disconnection yet.
                        return testConnectionToBroker()
                                .andThen(Completable.fromAction(this::resubscribeToAllUMods));
                    } else {
                        return testConnectionToBroker()
                                .andThen(mMqttClient.connect())
                                .andThen(Completable.fromAction(() -> {
                                    Log.d("MQTT_SERVICE",
                                            "ACTUAL RECONNECTION COMPLETED "
                                                    + Thread.currentThread().getName());
                                    resubscribeToAllUMods();
                                }));
                    }
                })
                .doFinally(() -> {
                    Log.d("MQTT_SERVICE",
                            "RECONNECTION FINALLY + UNLOCKING!!  "
                                    + Thread.currentThread().getName());
                    clientMutex.release();
                }))
                .subscribeOn(Schedulers.io())
                .subscribe(() -> Log.d("MQTT_SERVICE",
                                "RECONNECTION COMPLETE ON: "
                                + Thread.currentThread().getName()),
                        throwable -> Log.d("MQTT_SERVICE",
                                "RECONNECTION ERROR: "
                                + throwable.getClass().getSimpleName()
                                +" ON: "
                                + Thread.currentThread().getName()));
    */
    }

    private long millisecondsBetweenDates(Date newer, Date older){
        return newer.getTime() - older.getTime();
    }

    public Completable testConnectionToBroker(){
        return Completable.defer(() ->
                Completable.fromAction(() -> {
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
                })
        );
    }

    public Observable<UMod> scanUModInvitations(){
        String invitationsTopic = this.userName + "/invitation/+";
        Flowable<UMod> invitationsFlowable = this.connectMqttClient()
                .andThen(mMqttClient.unsubscribe(invitationsTopic))
                .andThen(mMqttClient.subscribe(invitationsTopic,1))
                .doOnNext(mqttMessage -> Log.d("MQTT_SERVICE", "INVITATION: " + new String(mqttMessage.getPayload())))
                .flatMap(mqttMessage -> {
                    String msgPayload = new String(mqttMessage.getPayload());
                    String uModUUID = getUUIDFromUModAdvertisedID(msgPayload);
                    if (uModUUID!=null){
                        UMod invitedUMod = new UMod(uModUUID);
                        invitedUMod.setAppUserLevel(UModUser.Level.INVITED);
                        invitedUMod.setuModSource(UMod.UModSource.MQTT_SCAN);
                        invitedUMod.setState(UMod.State.STATION_MODE);
                        invitedUMod.setConnectionAddress(null);
                        subscribeToUModResponseTopic(invitedUMod);
                        return Flowable.just(invitedUMod);
                    } else {
                        return Flowable.empty();
                    }
                });
        return RxJavaInterop.toV1Observable(invitationsFlowable)
                .takeUntil(Observable.timer(5000L,TimeUnit.MILLISECONDS));
    }

    public rx.Completable cancelUModInvitation(String userName, String uModUUID){
        String invitationsTopic = userName
                + "/invitation/"
                + GlobalConstants.URBIT_PREFIX
                + uModUUID;

        Log.d("MQTT_SERVICE","CANCELING: " + invitationsTopic);
        short mqttMessageId = (short) new Random().nextInt();
        MqttMessage mqttMessage =
                MqttMessage.create(mqttMessageId,new byte[0],1,true);
        Completable publishCompletable = Completable.defer(() -> this.connectMqttClient()
                .andThen(mMqttClient.publish(invitationsTopic,mqttMessage))
                .toCompletable())
                .doOnComplete(() -> Log.d("MQTT_SERVICE","SUCCESS ON CANCELING: " + invitationsTopic))
                .doOnError(throwable -> Log.e("MQTT_SERVICE","FAILURE ON CANCELING: " + invitationsTopic,throwable));
        return RxJavaInterop.toV1Completable(publishCompletable);
    }

    public void cancelMyInvitation(UMod uMod){
        this.cancelUModInvitation(this.userName, uMod.getUUID())
                .subscribeOn(rx.schedulers.Schedulers.io())
                .subscribe(() -> Log.d("MQTT_SERVICE","SUCCESS ON CANCELING MINE : " + uMod.getUUID()),
                        throwable -> Log.d("MQTT_SERVICE","FAILURE ON CANCELING MINE : " + uMod.getUUID()));
    }

    public rx.Completable cancelSeveralUModInvitations(List<String> listOfNames, UMod uMod){
        return Observable.from(listOfNames)
                .flatMapCompletable(userName ->
                                cancelUModInvitation(userName,uMod.getUUID()),
                        true,
                        10).toCompletable();
    }

    private String getUUIDFromUModAdvertisedID(String hostName){
        String uModUUID = null;

        Pattern pattern = Pattern.compile(
                GlobalConstants.URBIT_PREFIX
                        + GlobalConstants.DEVICE_UUID_REGEX);
        Matcher matcher = pattern.matcher(hostName);

        if (matcher.find()){
            uModUUID = matcher.group(1);
        }
        return uModUUID;
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




        try {
            lastReconnectMutex.lockInterruptibly();
            if (lastReconnectionAttempt != null){
                long diffInMillis = millisecondsBetweenDates(new Date(), lastReconnectionAttempt);
                lastReconnectMutex.unlock();
                if (diffInMillis < 1450L){
                    return;
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    //clientMutex.acquireUninterruptibly();
    Completable reconnectionCompletable;
        reconnectionCompletable = Flowable.timer(2000L,TimeUnit.MILLISECONDS)
                .flatMapCompletable(aLong -> {
                clientMutex.acquireUninterruptibly();
                Log.d("MQTT_SERVICE", "Reconnection attempt. Lock acquired.");

                    if (lastReconnectionAttempt != null){
                        long diffInMillis = millisecondsBetweenDates(new Date(), lastReconnectionAttempt);
                        lastReconnectMutex.unlock();
                        if (diffInMillis < 4500L){
                            return Completable.complete();
                        }
                    }

                //lastReconnectMutex.lockInterruptibly();
                lastReconnectionAttempt = new Date();
                //lastReconnectMutex.unlock();

                if (this.mMqttClient.isConnected()){
                return this.testConnectionToBroker()
                .andThen(this.mMqttClient.disconnect())
                .delay(2000L, TimeUnit.MILLISECONDS)
                .andThen(this.mMqttClient.connect())
                .delay(2000L, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .andThen(Completable.fromAction(this::resubscribeToAllUMods));
                } else {
                return this.testConnectionToBroker()
                .andThen(this.mMqttClient.connect())
                .delay(2000L, TimeUnit.MILLISECONDS)
                .andThen(Completable.fromAction(this::resubscribeToAllUMods));
                }
                });
                reconnectionCompletable
                .subscribeOn(Schedulers.io())
                .doFinally(() -> {
                Log.d("MQTT_SERVICE","Reconnection Finalized. Lock released.");
                clientMutex.release();
                })
                .subscribe(
                () -> Log.d("MQTT_SERVICE", "Reconnection Success"),
                throwable -> Log.e("MQTT_SERVICE", "Reconnection Failure",throwable));

                //this.connectMqttClient().subscribeOn(Schedulers.io()).subscribe();

 */