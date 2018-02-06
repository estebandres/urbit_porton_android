package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.util.GlobalConstants;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class SetWiFiAPRPC extends RPC {
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
        @SerializedName(GlobalConstants.RPC_REQ_ARGS_ATTR_NAME)
        private SetWiFiAPRPC.Arguments methodArguments;

        public Request(Arguments args, String uModTag, int id) {
            super("Admin.SetWiFiAP",uModTag,id);
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

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_SUCC_RESP_RESULT_ATTR_NAME)
        private SetWiFiAPRPC.Result responseResult;

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