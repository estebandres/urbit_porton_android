package com.urbit_iot.porton.data.source.internet;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.GetGateStatusRPC;
import com.urbit_iot.porton.data.rpc.RPC;
import com.urbit_iot.porton.util.GlobalConstants;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.HttpException;
import rx.Completable;
import rx.Observable;

/**
 * Created by andresteve07 on 28/10/18.
 */

public class SimplifiedUModMqttService implements UModMqttServiceContract {
    @NonNull
    private PahoClientRxWrap pahoClientRxWrap;
    @NonNull
    @VisibleForTesting
    String appUsername;
    @NonNull
    private Gson gsonInstance;
    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    @Inject
    public SimplifiedUModMqttService(@NonNull PahoClientRxWrap pahoClientRxWrap,
                                     @NonNull String username,
                                     @NonNull Gson gsonInstance,
                                     @NonNull BaseSchedulerProvider mSchedulerProvider) {
        this.pahoClientRxWrap = pahoClientRxWrap;
        this.appUsername = username;
        this.gsonInstance = gsonInstance;
        this.mSchedulerProvider = mSchedulerProvider;
        this.pahoClientRxWrap.connectToBroker()
                .subscribeOn(this.mSchedulerProvider.io())//TODO use dagger injected scheduler
                .subscribe(() -> {},
                        throwable -> Log.e("MQTT_SRV", "First Connection Failed  ERROR: " + throwable.getMessage())
                        );
    }

    @Override
    public void subscribeToUModTopics(String uModUUID) {
        String responseTopicForUMod = GlobalConstants.URBIT_PREFIX + uModUUID + "/response/" + this.appUsername;
        String statusTopicForUMod = GlobalConstants.URBIT_PREFIX + uModUUID + "/status";
        String[] topics = {responseTopicForUMod,statusTopicForUMod};
                Log.d("subscribeRespTopic","Attempt subscription  TOPIC: "
                + responseTopicForUMod + " ON: " + Thread.currentThread().getName());
        this.pahoClientRxWrap.connectToBroker()
                .doOnError(throwable -> pahoClientRxWrap.addRequestedSubscriptionTopic(responseTopicForUMod))
                .andThen(this.pahoClientRxWrap.subscribeToSeveralTopics(topics,0))
        //this.pahoClientRxWrap.subscribeToTopic(responseTopicForUMod,1)
                .subscribe(
                        () -> Log.d("subscribeRespTopic","SUB_SUCCESS  TOPICS: "
                                + Arrays.asList(topics) + " ON: " + Thread.currentThread().getName()),
                        throwable -> Log.e("subscribeRespTopic","SUB_FAIL  TOPICS: "
                                + Arrays.asList(topics) + " ON: " + Thread.currentThread().getName(),throwable));
    }

    @Override
    public synchronized void subscribeToUModTopics(UMod umod) {
        if (umod.isInAPMode()){
            pahoClientRxWrap.addRequestedSubscriptionTopic(GlobalConstants.URBIT_PREFIX + umod.getUUID() + "/response/" + this.appUsername);
            pahoClientRxWrap.addRequestedSubscriptionTopic(GlobalConstants.URBIT_PREFIX + umod.getUUID() + "/status");
            return;
        }
        this.subscribeToUModTopics(umod.getUUID());
    }


    @Override
    public <T, S> Observable<S> publishRPC(UMod targetUMod, T request, Class<S> responseType) {
        return this.publishRPC(targetUMod, request, responseType, 1);
    }

    @Override
    public <T, S> Observable<S> publishRPC(UMod targetUMod, T request, Class<S> responseType, int qos) {
        String serializedRequest = gsonInstance.toJson(request);
        String requestTopic = targetUMod.getUModRequestTopic();
        Log.d("publishRPC", "TOPIC: " + requestTopic + " ON: " + Thread.currentThread().getName() + "  Serialized Request: " + serializedRequest );
        return this.pahoClientRxWrap.connectToBroker()
                .doOnCompleted(() -> Log.d("publishRPC","CONECTADO!" + " ON: " + Thread.currentThread().getName()))
                .andThen(this.pahoClientRxWrap.publishToTopic(serializedRequest.getBytes(), requestTopic, qos, false))
                .doOnCompleted(() -> Log.d("publishRPC","PUBLICADO! " + requestTopic + " ON: " + Thread.currentThread().getName()))
                .andThen(this.pahoClientRxWrap.receivedMessagesObservable())
                .filter(messageWrapper -> messageWrapper.getMqttMessage().getPayload().length > 0)
                .filter(messageWrapper -> messageWrapper.getTopic().contains(targetUMod.getUUID()))
                .flatMap(messageWrapper -> {
                    try {
                        S response = gsonInstance.fromJson(new String(messageWrapper.getMqttMessage().getPayload()), responseType);
                        return Observable.just(response);
                    } catch (JsonSyntaxException exc){
                        Log.e("publishRPC","INVALID JSON SYNTAX: " + requestTopic + " ON: " + Thread.currentThread().getName());
                        return Observable.empty();
                    }
                })
                .filter(response -> ((RPC.Response)response).getResponseId() == ((RPC.Request)request).getRequestId()
                        && ((RPC.Response)response).getRequestTag().equalsIgnoreCase(((RPC.Request)request).getRequestTag()))
                .doOnNext(response -> Log.d("publishRPC","PUB RESPONSE: " + response))
                .timeout(8000L, TimeUnit.MILLISECONDS,this.mSchedulerProvider.computation())
                .doOnError(throwable -> Log.e("publishRPC", "FAIL TO PUBLISH: " + serializedRequest + "  TO TOPIC: " + requestTopic + " CAUSE: " + throwable.getClass().getSimpleName() + " ON: " + Thread.currentThread().getName()))
                .first()
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
                        return Observable.error(httpException);
                    }
                    return Observable.just(response);
                });
    }

    @Override
    public Completable testConnectionToBroker() {
        return Completable.defer(() -> Completable.fromEmitter(completableEmitter -> {
            Socket clientSocket;
            clientSocket = new Socket();
            try{
                clientSocket.connect(
                        new InetSocketAddress(
                                GlobalConstants.MQTT_BROKER__IP_ADDRESS,
                                GlobalConstants.MQTT_BROKER__PORT),
                        1500);
                completableEmitter.onCompleted();
            } catch (IOException exc){
                completableEmitter.onError(exc);
            }finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    @Override
    public Observable<UMod> scanUModInvitations() {
        return this.pahoClientRxWrap.receivedMessagesObservable()
                .subscribeOn(this.mSchedulerProvider.io())
                .doOnSubscribe(this::resetInvitationTopic)
                .doOnNext(messageWrapper -> Log.d("MQTT_SERVICE", "INVITATION: "
                        + new String(messageWrapper.getMqttMessage().getPayload())
                        + " ON: " + Thread.currentThread().getName()))
                .takeUntil(Observable.timer(5200L,TimeUnit.MILLISECONDS))
                .flatMap(messageWrapper -> {
                    String msgPayload = new String(messageWrapper.getMqttMessage().getPayload());
                    String uModUUID = getUUIDFromUModAdvertisedID(msgPayload);
                    if (uModUUID != null) {
                        UMod invitedUMod = new UMod(uModUUID);
                        invitedUMod.setAppUserLevel(UModUser.Level.INVITED);
                        invitedUMod.setuModSource(UMod.UModSource.MQTT_SCAN);
                        invitedUMod.setState(UMod.State.STATION_MODE);
                        invitedUMod.setConnectionAddress(null);
                        subscribeToUModTopics(invitedUMod);
                        return Observable.just(invitedUMod);
                    } else {
                        return Observable.empty();
                    }
                });
    }

    void resetInvitationTopic(){
        String invitationsTopic = this.appUsername + "/invitation/+";
        this.pahoClientRxWrap.connectToBroker()
                //.subscribeOn(mSchedulerProvider.io())//TODO use dagger injected scheduler
                .andThen(this.pahoClientRxWrap.unsubscribeFromTopic(invitationsTopic))
                .andThen(this.pahoClientRxWrap.subscribeToTopic(invitationsTopic,0,false))
                .subscribe(() -> Log.d("SERVICE", "INVITATION RESET SUCCESSFUL"),
                        throwable -> Log.e("SERVICE", "INVITATION RESET FAILURE",throwable));
    }
    //TODO is this a method for UMod class? Please avoid repetition
    String getUUIDFromUModAdvertisedID(String hostName){
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

    @Override
    public Completable cancelUModInvitation(String userName, String uModUUID) {
        String invitationsTopic = userName
                + "/invitation/"
                + GlobalConstants.URBIT_PREFIX
                + uModUUID;
        Log.d("MQTT_SERVICE","CANCELING: " + invitationsTopic);
        return this.pahoClientRxWrap.connectToBroker()
                .subscribeOn(mSchedulerProvider.io())//TODO use dagger injected scheduler
                .andThen(pahoClientRxWrap.publishToTopic(new byte[0],invitationsTopic,1,true))
                .doOnCompleted(() -> Log.d("MQTT_SERVICE","SUCCESS ON CANCELING: " + invitationsTopic))
                .doOnError(throwable -> Log.e("MQTT_SERVICE","FAILURE ON CANCELING: " + invitationsTopic,throwable));
    }

    @Override
    public void cancelMyInvitation(UMod uMod) {
        this.cancelUModInvitation(this.appUsername,uMod.getUUID())
                .subscribe(() -> Log.d("MQTT_SERVICE","SUCCESS ON CANCELING MINE : " + uMod.getUUID()),
                        throwable -> Log.d("MQTT_SERVICE","FAILURE ON CANCELING MINE : " + uMod.getUUID()));

    }

    @Override
    public Completable cancelSeveralUModInvitations(List<String> listOfNames, UMod uMod) {
        return Observable.from(listOfNames)
                .flatMapCompletable(userName ->
                                cancelUModInvitation(userName,uMod.getUUID()),
                        true,
                        10).toCompletable();
    }

    @Override
    public void clearAllSubscriptions() {
        this.pahoClientRxWrap.connectToBroker()
                //.subscribeOn(mSchedulerProvider.io())//TODO use dagger injected scheduler
                .andThen(this.pahoClientRxWrap.unsubscribeFromAllTopics())
                .subscribe(() -> {},
                        throwable -> Log.e("MQTT_SRV", "UNSUB_ALL Error: " +
                                throwable.getMessage() + " Type: " +
                                throwable.getClass().getSimpleName()));
    }

    public Observable<GetGateStatusRPC.Response> getUModsGateStatusUpdates(){
        return this.pahoClientRxWrap.receivedMessagesObservable()
                .filter(messageWrapper -> messageWrapper.getTopic().contains("status"))
                .flatMap(messageWrapper -> {
                    try{
                        String umodUUID = getUUIDFromUModStatusTopic(messageWrapper.getTopic());
                        if (Strings.isNullOrEmpty(umodUUID)){
                            return Observable.empty();
                        }
                        GetGateStatusRPC.Result gateStatusResult =
                                gsonInstance.fromJson(new String(messageWrapper.getMqttMessage().getPayload()),
                                        GetGateStatusRPC.Result.class);
                        GetGateStatusRPC.Response gateStatusResponse =
                                new GetGateStatusRPC.Response(gateStatusResult,
                                        123456789,
                                        umodUUID,
                                        null);
                        Log.d("MQTT_SERVICE","STATUS UPDATE: " + gateStatusResponse);
                        return Observable.just(gateStatusResponse);
                    } catch (JsonSyntaxException exc){
                        Log.e("MQTT_SERVICE","INVALID JSON SYNTAX: " +
                                new String(messageWrapper.getMqttMessage().getPayload()) +
                                " ON: " + Thread.currentThread().getName());
                        return Observable.empty();
                    }
                });
    }


    String getUUIDFromUModStatusTopic(String statusTopic){
        String uModUUID = null;

        Pattern pattern = Pattern.compile(
                GlobalConstants.URBIT_PREFIX
                        + GlobalConstants.DEVICE_UUID_REGEX+"/status");
        Matcher matcher = pattern.matcher(statusTopic);

        if (matcher.find()){
            uModUUID = matcher.group(1);
        }
        return uModUUID;
    }
}
