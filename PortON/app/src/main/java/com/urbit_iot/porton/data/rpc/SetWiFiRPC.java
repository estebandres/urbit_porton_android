package com.urbit_iot.porton.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.porton.util.GlobalConstants;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class SetWiFiRPC extends RPC {
    public static class Arguments{

        @SerializedName("ssid")
        private String ssid;
        @SerializedName("password")
        private String password;

        public Arguments(String ssid, String password) {
            this.ssid = ssid;
            this.password = password;
        }

        public String getSsid() {
            return ssid;
        }

        public void setSsid(String ssid) {
            this.ssid = ssid;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ARGS)
        private SetWiFiRPC.Arguments methodArguments;

        public Request(Arguments args, String callTag, int id) {
            super(GlobalConstants.RPC_METHOD_NAME__SET_WIFI,
                    GlobalConstants.RPC_METHOD_CODE__SET_WIFI,
                    callTag,id);
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
        private SetWiFiRPC.Result responseResult;

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