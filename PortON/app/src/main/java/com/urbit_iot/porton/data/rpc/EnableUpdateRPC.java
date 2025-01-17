package com.urbit_iot.porton.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.porton.util.GlobalConstants;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class EnableUpdateRPC extends RPC {
    public static class Arguments{
        public Arguments(){}
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ARGS)
        private EnableUpdateRPC.Arguments methodArguments;
        public Request(Arguments args, String requester, String callTag, int id) {
            super(
                    GlobalConstants.RPC_METHOD_NAME__ENABLE_UPDATE,
                    GlobalConstants.RPC_METHOD_CODE__ENABLE_UPDATE,
                    requester,
                    callTag,
                    id);

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
        @SerializedName("message")
        private String message;
        public Result(String message){
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "message='" + message + '\'' +
                    '}';
        }
    }

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__RESULT)
        private EnableUpdateRPC.Result responseResult;

        public Response(Result result, int responseId, String callTag, ResponseError responseError) {
            super(responseId, callTag, responseError);
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
                    super.toString() + ", " +
                    "responseResult=" + responseResult +
                    '}';
        }
    }
}
