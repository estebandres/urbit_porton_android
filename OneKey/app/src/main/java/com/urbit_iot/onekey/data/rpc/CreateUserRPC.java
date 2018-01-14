package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.util.GlobalConstants;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class CreateUserRPC extends RPC {
    public static class Arguments{

        @SerializedName("credential")
        private String credentials;

        public Arguments(String credentials) {
            this.credentials = credentials;
        }

        public String getCredentials() {
            return credentials;
        }

        public void setCredentials(String credentials) {
            this.credentials = credentials;
        }
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_REQ_ARGS_ATTR_NAME)
        private CreateUserRPC.Arguments methodArguments;

        public Request(Arguments args, String uModTag, int id) {
            super("Guest.CreateUser",uModTag,id);
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
        private GetMyUserLevelRPC.UModUserType userType;

        public Result(GetMyUserLevelRPC.UModUserType userType){
            this.userType = userType;
        }

        public GetMyUserLevelRPC.UModUserType getUserType() {
            return userType;
        }

        public UModUser.Level getUserLevel(){
            return this.userType.asUModUserLevel();
        }

        public void setUserType(GetMyUserLevelRPC.UModUserType userType) {
            this.userType = userType;
        }
    }

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_SUCC_RESP_RESULT_ATTR_NAME)
        private CreateUserRPC.Result responseResult;

        public Response(Result result, String callTag, ResponseError responseError) {
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
                    '}';
        }
    }
}