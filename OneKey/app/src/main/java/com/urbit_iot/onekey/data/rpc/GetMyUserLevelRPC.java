package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.util.GlobalConstants;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by andresteve07 on 12/1/17.
 */

public class GetMyUserLevelRPC {
    public static final List<Integer> ALLOWED_ERROR_CODES = Arrays.asList(
            HttpURLConnection.HTTP_BAD_REQUEST,
            HttpURLConnection.HTTP_NOT_FOUND,
            //Digest Auth + ACL this call is performed with urbit:urbit so never should fail.
            HttpURLConnection.HTTP_UNAUTHORIZED,
            HttpURLConnection.HTTP_FORBIDDEN);

    public static class Arguments{
        @SerializedName("user_name")
        private String userName;

        public Arguments(String userName){
            this.userName = userName;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_REQ_ARGS_ATTR_NAME)
        private GetMyUserLevelRPC.Arguments methodArguments;
        public Request(Arguments args, String uModTag){
            super("Guest.UserStatus", uModTag, (new Random().nextInt()));
            this.methodArguments = args;
        }

        public Arguments getMethodArguments() {
            return methodArguments;
        }

        public void setMethodArguments(Arguments methodArguments) {
            this.methodArguments = methodArguments;
        }
    }

    public static class Result{

        @SerializedName("user_type")
        private APIUserType APIUserType;

        public Result(APIUserType APIUserType){
            this.APIUserType = APIUserType;
        }

        public APIUserType getAPIUserType() {
            return APIUserType;
        }

        public UModUser.Level getUserLevel(){
            return this.APIUserType.asUModUserLevel();
        }

        public void setAPIUserType(APIUserType APIUserType) {
            this.APIUserType = APIUserType;
        }
    }

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_SUCC_RESP_RESULT_ATTR_NAME)
        private GetMyUserLevelRPC.Result responseResult;
        public Response(Result result, String callTag, RPC.ResponseError responseError){
            super(callTag, responseError);
            this.responseResult = result;
        }

        public Result getResponseResult() {
            return responseResult;
        }

        public void setResponseResult(Result responseResult) {
            this.responseResult = responseResult;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "callTag=" + this.getCallTag() + ", " +
                    "responseResult=" + responseResult +
                    "} ";
        }
    }

}
