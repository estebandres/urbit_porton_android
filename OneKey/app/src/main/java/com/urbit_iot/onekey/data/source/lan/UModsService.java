package com.urbit_iot.onekey.data.source.lan;

import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;

import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;
import rx.Single;


/**
 * Created by andresteve07 on 12/10/17.
 */

public interface UModsService {

    @POST("/rpc/")
    Observable<SysGetInfoRPC.Response> getSystemInfo(@Body SysGetInfoRPC.Request request);

    @POST("/rpc/")
    Single<TriggerRPC.Response> triggerUMod(@Body TriggerRPC.Request request);

    @POST("/rpc/")
    Single<CreateUserRPC.Response> createUser(@Body CreateUserRPC.Request request);
}
