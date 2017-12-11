package com.urbit_iot.onekey.data.source.lan;

import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;

import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

import retrofit2.http.GET;


/**
 * Created by andresteve07 on 12/10/17.
 */

public interface UModsService {

    @POST("/rpc/")
    Observable<SysGetInfoRPC.SuccessResponse> getSystemInfo(@Body SysGetInfoRPC.Request request);
}
