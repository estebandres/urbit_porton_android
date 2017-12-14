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
        public Result(){}
    }

    public static class SuccessResponse extends RPC.SuccessResponse{
        @SerializedName(GlobalConstants.RPC_SUCC_RESP_RESULT_ATTR_NAME)
        private CreateUserRPC.Result responseResult;

        public SuccessResponse(Result result, String callTag, ResponseError responseError) {
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
            return "SuccessResponse{" +
                    "callTag=" + this.getCallTag() + ", " +
                    "responseResult=" + responseResult +
                    '}';
        }
    }
}