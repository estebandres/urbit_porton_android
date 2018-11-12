package com.urbit_iot.porton.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.porton.util.GlobalConstants;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class TriggerRPC extends RPC {
    public static class Arguments{
        public Arguments(){}
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ARGS)
        private TriggerRPC.Arguments methodArguments;
        public Request(String requester, boolean isAdmin, Arguments args, String tagPrefix, int id) {
            super(
                    GlobalConstants.RPC_METHOD_NAME__ADMIN_TRIGGER,
                    GlobalConstants.RPC_METHOD_CODE__ADMIN_TRIGGER,
                    requester,
                    tagPrefix,
                    id);
            this.methodArguments = args;
            this.changeMethod(isAdmin);
        }

        public void changeMethod(boolean isAdmin){
            if (isAdmin){
                super.setMethodName(GlobalConstants.RPC_METHOD_NAME__ADMIN_TRIGGER);
                super.setMethodCode(GlobalConstants.RPC_METHOD_CODE__ADMIN_TRIGGER);
            } else {
                super.setMethodName(GlobalConstants.RPC_METHOD_NAME__USER_TRIGGER);
                super.setMethodCode(GlobalConstants.RPC_METHOD_CODE__USER_TRIGGER);
            }
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
        @SerializedName("status_code")
        private Integer statusCode;
        @SerializedName("opening_percentage")
        private Integer openingPercentage;

        public Result(String message){
            this.message = message;
        }

        public Result(String message, Integer statusCode, Integer openingPercentage) {
            this.message = message;
            this.statusCode = statusCode;
            this.openingPercentage = openingPercentage;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }

        public Integer getOpeningPercentage() {
            return openingPercentage;
        }

        public void setOpeningPercentage(Integer openingPercentage) {
            this.openingPercentage = openingPercentage;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "message='" + message + '\'' +
                    ", statusCode=" + statusCode +
                    ", openingPercentage=" + openingPercentage +
                    '}';
        }
    }

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__RESULT)
        private TriggerRPC.Result responseResult;

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