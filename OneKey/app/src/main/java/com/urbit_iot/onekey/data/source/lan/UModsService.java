package com.urbit_iot.onekey.data.source.lan;

import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.OTACommitRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;

import javax.inject.Inject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Streaming;
import retrofit2.http.Url;
import rx.Observable;
import rx.Single;


/**
 * Created by andresteve07 on 12/10/17.
 */

public interface UModsService {

    @POST("/rpc/")
    Observable<SysGetInfoRPC.Response> getSystemInfo(@Body SysGetInfoRPC.Request request);

    @POST("/rpc/")
    Observable<TriggerRPC.Response> triggerUMod(@Body TriggerRPC.Request request);

    @POST("/rpc/")
    Observable<CreateUserRPC.Response> createUser(@Body CreateUserRPC.Request request);

    @POST("/rpc/")
    Observable<GetMyUserLevelRPC.Response> getAppUserLevel(@Body GetMyUserLevelRPC.Request request);

    @POST("/rpc/")
    Observable<FactoryResetRPC.Response> postFactoryReset(@Body FactoryResetRPC.Request request);

    /*
    @GET
    @Streaming
    Observable<Response<ResponseBody>> downloadFirmwareImage(@Url String downloadUrl);
    */

    @Multipart
    @POST("/update")
    Observable<Response<ResponseBody>>
    startFirmwareUpdate(@Part("commit_timeout") RequestBody timeoutBody,
                        @Part MultipartBody.Part firmwareImageFile);

    @POST("/rpc/")
    Observable<Response<ResponseBody>> otaCommit(@Body OTACommitRPC.Request request);
}
