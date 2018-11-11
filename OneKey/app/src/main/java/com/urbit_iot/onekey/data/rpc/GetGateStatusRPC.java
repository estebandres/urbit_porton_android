package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.util.GlobalConstants;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class GetGateStatusRPC extends RPC {
    public static class Arguments{
        public Arguments(){}
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ARGS)
        private GetGateStatusRPC.Arguments methodArguments;
        public Request(String requester, boolean isAdmin, Arguments args, String tagPrefix, int id) {
            super(
                    GlobalConstants.RPC_METHOD_NAME__ADMIN_GET_GATE_STATUS,
                    GlobalConstants.RPC_METHOD_CODE__ADMIN_GET_GATE_STATUS,
                    requester,
                    tagPrefix,
                    id);
            this.methodArguments = args;
            this.changeMethod(isAdmin);
        }

        public void changeMethod(boolean isAdmin){
            if (isAdmin){
                super.setMethodName(GlobalConstants.RPC_METHOD_NAME__ADMIN_GET_GATE_STATUS);
                super.setMethodCode(GlobalConstants.RPC_METHOD_CODE__ADMIN_GET_GATE_STATUS);
            } else {
                super.setMethodName(GlobalConstants.RPC_METHOD_NAME__USER_GET_GATE_STATUS);
                super.setMethodCode(GlobalConstants.RPC_METHOD_CODE__USER_GET_GATE_STATUS);
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
        @SerializedName("status_code")
        private Integer statusCode;
        @SerializedName("opening_percentage")
        private Float openingPorcentage;


        public Result(Integer statusCode, Float openingPorcentage) {
            this.statusCode = statusCode;
            this.openingPorcentage = openingPorcentage;
        }

        public Integer getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }

        public Float getOpeningPorcentage() {
            return openingPorcentage;
        }

        public void setOpeningPorcentage(Float openingPorcentage) {
            this.openingPorcentage = openingPorcentage;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "statusCode=" + statusCode +
                    ", openingPorcentage=" + openingPorcentage +
                    '}';
        }
    }

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__RESULT)
        private GetGateStatusRPC.Result responseResult;

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