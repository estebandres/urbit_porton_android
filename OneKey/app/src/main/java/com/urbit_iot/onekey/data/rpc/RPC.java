package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class RPC {

    public static class Request {
        @SerializedName("method")
        private String methodName;
        @SerializedName("tag")
        private String callTag;
        @SerializedName("id")
        private int callID;

        public Request(String methodName, String callTag, int callID) {
            this.methodName = methodName;
            this.callTag = callTag;
            this.callID = callID;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
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

    public static class Result{}

    public static class Response {

        @SerializedName("tag")
        private String callTag;

        //TODO the ESP returns always a successful response even when an error occurs.
        @SerializedName("error")
        private ResponseError responseError;

        public Response(String callTag, ResponseError responseError) {
            this.callTag = callTag;
            this.responseError = responseError;
        }

        public String getCallTag() {
            return callTag;
        }

        public void setCallTag(String callTag) {
            this.callTag = callTag;
        }

        public ResponseError getResponseError() {
            return responseError;
        }

        public void setResponseError(ResponseError responseError) {
            this.responseError = responseError;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "callTag='" + callTag + '\'' +
                    ", responseError=" + responseError +
                    '}';
        }
    }

    public static class ResponseError {
        @SerializedName("code")
        private Integer errorCode;
        @SerializedName("message")
        private String errorMessage;

        public ResponseError(Integer errorCode, String errorMessage) {
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

        @Override
        public String toString() {
            return "ResponseError{" +
                    "errorCode=" + errorCode +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }
}
