package com.urbit_iot.porton.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.porton.util.GlobalConstants;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class SetGateStatusRPC extends RPC {
    public static final List<Integer> DOC_ERROR_CODES = Arrays.asList(
            //HttpURLConnection.HTTP_NOT_FOUND,
            //HttpURLConnection.HTTP_BAD_REQUEST,
            //Digest Auth + ACL this call is performed with user:pass COULD fail.
            HttpURLConnection.HTTP_UNAUTHORIZED,
            HttpURLConnection.HTTP_FORBIDDEN,
            //(Mongoose side) Any Error.
            HttpURLConnection.HTTP_INTERNAL_ERROR);

    public static class GateStatusRPCModel{
        @SerializedName("status_code")
        private Integer gateStatusCode;

        public GateStatusRPCModel(Integer gateStatus) {
            this.gateStatusCode = gateStatus;
        }

        public Integer getGateStatusCode() {
            return gateStatusCode;
        }

        public void setGateStatusCode(Integer gateStatusCode) {
            this.gateStatusCode = gateStatusCode;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "gateStatusCode=" + gateStatusCode +
                    '}';
        }
    }

    public static class Arguments extends GateStatusRPCModel{

        public Arguments(Integer gateStatus) {
            super(gateStatus);
        }
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ARGS)
        private SetGateStatusRPC.Arguments methodArguments;

        public Request(Arguments args, String requester, String uModTag, int id) {
            super(
                    GlobalConstants.RPC_METHOD_NAME___SET_GATE_STATUS,
                    GlobalConstants.RPC_METHOD_CODE__SET_GATE_STATUS,
                    requester,
                    uModTag,
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

    public static class Result extends GateStatusRPCModel{

        public Result(Integer gateStatus) {
            super(gateStatus);
        }
    }

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__RESULT)
        private SetGateStatusRPC.Result responseResult;

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