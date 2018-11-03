package com.urbit_iot.onekey.data.source.internet;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.data.rpc.RPC;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private String appUsername;
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
    public void subscribeToUModResponseTopic(String uModUUID) {
        String responseTopicForUMod = GlobalConstants.URBIT_PREFIX + uModUUID + "/response/" + this.appUsername;
        Log.d("subscribeRespTopic","Attempt subscription  TOPIC: "
                + responseTopicForUMod + " ON: " + Thread.currentThread().getName());
        this.pahoClientRxWrap.connectToBroker()
                .andThen(this.pahoClientRxWrap.subscribeToTopic(responseTopicForUMod,0))
        //this.pahoClientRxWrap.subscribeToTopic(responseTopicForUMod,1)
                .subscribe(
                        () -> Log.d("subscribeRespTopic","SUB_SUCCESS  TOPIC: "
                                + responseTopicForUMod + " ON: " + Thread.currentThread().getName()),
                        throwable -> Log.e("subscribeRespTopic","SUB_FAIL  TOPIC: "
                                + responseTopicForUMod + " ON: " + Thread.currentThread().getName(),throwable));
    }

    @Override
    public synchronized void subscribeToUModResponseTopic(UMod umod) {
        this.subscribeToUModResponseTopic(umod.getUUID());
    }

    @Override
    public <T, S> Observable<S> publishRPC(UMod targetUMod, T request, Class<S> responseType) {
        String serializedRequest = gsonInstance.toJson(request);
        String requestTopic = targetUMod.getUModRequestTopic();
        Log.d("publishRPC", "TOPIC: " + requestTopic + " ON: " + Thread.currentThread().getName() + "  Serialized Request: " + serializedRequest );
        return this.pahoClientRxWrap.connectToBroker()
                .doOnCompleted(() -> Log.d("publishRPC","CONECTADO!" + " ON: " + Thread.currentThread().getName()))
                .andThen(this.pahoClientRxWrap.publishToTopic(serializedRequest.getBytes(), requestTopic, 0, false))
                .doOnCompleted(() -> Log.d("publishRPC","PUBLICADO! " + requestTopic + " ON: " + Thread.currentThread().getName()))
                .andThen(this.pahoClientRxWrap.receivedMessagesObservable())
                //.doOnNext(message -> Log.d("publishRPC","RECIBIDO: " + requestTopic + " MSGE_IS_NULL: " + (message == null)))
                //.filter(mqttMessage -> mqttMessage.)//TODO create mqtt message class wrapper to include source topic to filter by response topic
                .filter(message -> message.getPayload().length > 0)
                //.doOnNext(message -> Log.d("publishRPC","RECIBIDO: " + requestTopic + " MSGE: " + message.getPayload().length))
                .flatMap(mqttMessage -> {
                    try {
                        S response = gsonInstance.fromJson(new String(mqttMessage.getPayload()), responseType);
                        return Observable.just(response);
                    } catch (JsonSyntaxException exc){
                        Log.e("publishRPC","INVALID JSON SYNTAX: " + requestTopic + " ON: " + Thread.currentThread().getName());
                        return Observable.empty();
                    }
                })
                //.map(mqttMessage -> gsonInstance.fromJson(new String(mqttMessage.getPayload()),responseType))
                //.doOnNext(s -> Log.d("publishRPC", s.toString()))
                .filter(response -> ((RPC.Response)response).getResponseId() == ((RPC.Request)request).getRequestId()
                        && ((RPC.Response)response).getRequestTag().equalsIgnoreCase(((RPC.Request)request).getRequestTag()))
                .doOnNext(response -> Log.d("publishRPC","PUB RESPONSE: " + response))
                .timeout(6000L, TimeUnit.MILLISECONDS)
                .doOnError(throwable -> Log.e("publishRPC", "FAIL TO PUBLISH: " + requestTopic + " CAUSE: " + throwable.getClass().getSimpleName() + " ON: " + Thread.currentThread().getName()))
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
        String invitationsTopic = this.appUsername + "/invitation/+";
        return this.pahoClientRxWrap.connectToBroker()
                //.subscribeOn(mSchedulerProvider.io())//TODO use dagger injected scheduler
                .andThen(this.pahoClientRxWrap.subscribeToTopic(invitationsTopic,0))
                .andThen(this.pahoClientRxWrap.receivedMessagesObservable())
                .doOnNext(mqttMessage -> Log.d("MQTT_SERVICE", "INVITATION: " + new String(mqttMessage.getPayload())))
                .takeUntil(Observable.timer(5200L,TimeUnit.MILLISECONDS))
                .flatMap(mqttMessage -> {
                    String msgPayload = new String(mqttMessage.getPayload());
                    String uModUUID = getUUIDFromUModAdvertisedID(msgPayload);
                    if (uModUUID != null) {
                        UMod invitedUMod = new UMod(uModUUID);
                        invitedUMod.setAppUserLevel(UModUser.Level.INVITED);
                        invitedUMod.setuModSource(UMod.UModSource.MQTT_SCAN);
                        invitedUMod.setState(UMod.State.STATION_MODE);
                        invitedUMod.setConnectionAddress(null);
                        subscribeToUModResponseTopic(invitedUMod);
                        return Observable.just(invitedUMod);
                    } else {
                        return Observable.empty();
                    }
                });
    }

    //TODO is this a method for UMod class? Please avoid repetition
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

    @Override
    public Completable cancelUModInvitation(String userName, String uModUUID) {
        String invitationsTopic = userName
                + "/invitation/"
                + GlobalConstants.URBIT_PREFIX
                + uModUUID;
        Log.d("MQTT_SERVICE","CANCELING: " + invitationsTopic);
        return this.pahoClientRxWrap.connectToBroker()
                .subscribeOn(mSchedulerProvider.io())//TODO use dagger injected scheduler
                .andThen(pahoClientRxWrap.publishToTopic(new byte[0],invitationsTopic,0,false))
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
}
