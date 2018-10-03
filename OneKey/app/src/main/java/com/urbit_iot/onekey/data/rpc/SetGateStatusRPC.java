package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.util.GlobalConstants;

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

    public static class Arguments{
        @SerializedName("status_code")
        private UMod.GateStatus gateStatus;

        public Arguments(UMod.GateStatus gateStatus) {
            this.gateStatus = gateStatus;
        }

        public UMod.GateStatus getGateStatus() {
            return gateStatus;
        }

        public void setGateStatus(UMod.GateStatus gateStatus) {
            this.gateStatus = gateStatus;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "gateStatus=" + gateStatus +
                    '}';
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

    public static class Result{
        @SerializedName("status_code")
        private UMod.GateStatus gateStatus;

        public Result(UMod.GateStatus gateStatus) {
            this.gateStatus = gateStatus;
        }

        public UMod.GateStatus getGateStatus() {
            return gateStatus;
        }

        public void setGateStatus(UMod.GateStatus gateStatus) {
            this.gateStatus = gateStatus;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "gateStatus=" + gateStatus +
                    '}';
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