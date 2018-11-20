package com.urbit_iot.porton.data.source.internet;

import com.urbit_iot.porton.data.UMod;

import java.util.List;

import rx.Completable;
import rx.Observable;

/**
 * Created by andresteve07 on 28/10/18.
 */

public interface UModMqttServiceContract {

    void subscribeToUModResponseTopic(String uModUUID);

    void subscribeToUModResponseTopic(UMod umod);

    <T,S> Observable<S> publishRPC(UMod targetUMod, T request, Class<S> responseType);

    <T,S> Observable<S> publishRPC(UMod targetUMod, T request, Class<S> responseType, int qos);

    Completable testConnectionToBroker();

    Observable<UMod> scanUModInvitations();

    Completable cancelUModInvitation(String userName, String uModUUID);

    void cancelMyInvitation(UMod uMod);

    Completable cancelSeveralUModInvitations(List<String> listOfNames, UMod uMod);

    void clearAllSubscriptions();
}