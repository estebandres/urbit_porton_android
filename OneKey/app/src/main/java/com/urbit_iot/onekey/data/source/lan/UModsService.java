package com.urbit_iot.onekey.data.source.lan;

import com.urbit_iot.onekey.data.rpc.CreateUserRPC;
import com.urbit_iot.onekey.data.rpc.DeleteUserRPC;
import com.urbit_iot.onekey.data.rpc.FactoryResetRPC;
import com.urbit_iot.onekey.data.rpc.GetMyUserLevelRPC;
import com.urbit_iot.onekey.data.rpc.GetUsersRPC;
import com.urbit_iot.onekey.data.rpc.OTACommitRPC;
import com.urbit_iot.onekey.data.rpc.SysGetInfoRPC;
import com.urbit_iot.onekey.data.rpc.TriggerRPC;
import com.urbit_iot.onekey.data.rpc.UpdateUserRPC;

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

    @POST("/rpc/Sys.GetInfo")
    Observable<SysGetInfoRPC.Result> getSystemInfo(@Body SysGetInfoRPC.Arguments request);

    @POST("/rpc/User.Trigger")
    Observable<TriggerRPC.Result> userTriggerUMod(@Body TriggerRPC.Arguments request);

    @POST("/rpc/Admin.Trigger")
    Observable<TriggerRPC.Result> adminTriggerUMod(@Body TriggerRPC.Arguments request);

    @POST("/rpc/Guest.CreateUser")
    Observable<CreateUserRPC.Result> createUser(@Body CreateUserRPC.Arguments request);

    @POST("/rpc/Admin.DeleteUser")
    Observable<DeleteUserRPC.Result> deleteUser(@Body DeleteUserRPC.Arguments requestArgs);

    @POST("/rpc/Guest.UserStatus")
    Observable<GetMyUserLevelRPC.Result> getAppUserLevel(@Body GetMyUserLevelRPC.Arguments request);

    @POST("/rpc/Admin.GetUsers")
    Observable<GetUsersRPC.Result> getUsers(@Body GetUsersRPC.Arguments arguments);

    @POST("/rpc/Admin.FactoryReset")
    Observable<FactoryResetRPC.Result> postFactoryReset(@Body FactoryResetRPC.Arguments request);

    @POST("/rpc/Admin.UpdateUser")
    Observable<UpdateUserRPC.Result> postUpdateUser(@Body UpdateUserRPC.Arguments requestArguments);

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

    @POST("/rpc/OTA.Commit")
    Observable<Response<ResponseBody>> otaCommit(@Body OTACommitRPC.Arguments arguments);
}
