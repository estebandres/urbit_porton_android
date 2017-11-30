package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class RPC {

    public static abstract class Arguments{}

    public static class Request {
        @SerializedName("method")
        private String methodName;
        @SerializedName("args")
        private Arguments methodArguments;
        @SerializedName("tag")
        private String callTag;
        @SerializedName("id")
        private int callID;

        public Request(String methodName, Arguments methodArguments, String callTag, int callID) {
            this.methodName = methodName;
            this.methodArguments = methodArguments;
            this.callTag = callTag;
            this.callID = callID;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public Arguments getMethodArguments() {
            return methodArguments;
        }

        public void setMethodArguments(Arguments methodArguments) {
            this.methodArguments = methodArguments;
        }

        public String getCallTag() {
            return callTag;
        }

        public void setCallTag(String callTag) {
            this.callTag = callTag;
        }

        public int getCallID() {
            return callID;
        }

        public void setCallID(int callID) {
            this.callID = callID;
        }
    }

    public static abstract class Result{}

    public static class SuccessResponse {
        @SerializedName("result")
        private Result responseResult;
        @SerializedName("tag")
        private String callTag;

        public SuccessResponse(Result responseResult, String callTag) {
            this.responseResult = responseResult;
            this.callTag = callTag;
        }

        public Result getResponseResult() {
            return responseResult;
        }

        public void setResponseResult(Result responseResult) {
            this.responseResult = responseResult;
        }

        public String getCallTag() {
            return callTag;
        }

        public void setCallTag(String callTag) {
            this.callTag = callTag;
        }
    }

    public static class FailureResponse {
        @SerializedName("code")
        private int errorCode;
        @SerializedName("tag")
        private String errorMessage;

        public FailureResponse(int errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
