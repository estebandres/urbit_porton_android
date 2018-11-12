package com.urbit_iot.porton.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.porton.util.GlobalConstants;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class RPC {

    public static class Request {
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__METHOD)
        private String methodName;
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__METHOD_CODE)
        private int methodCode;
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__REQUESTER)
        private String requester;
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__TAG)
        private String requestTag;
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ID)
        private int requestId;

        public Request(String methodName, int methodCode, String tagPrefix, int requestId) {
            this.methodName = methodName;
            this.methodCode = methodCode;
            this.requestTag = tagPrefix + "::" + this.methodCode;
            this.requestId = requestId;
        }

        public Request(String methodName,
                       int methodCode,
                       String requester,
                       String requestTag,
                       int requestId) {
            this.methodName = methodName;
            this.methodCode = methodCode;
            this.requester = requester;
            this.requestTag = requestTag;
            this.requestId = requestId;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getRequestTag() {
            return requestTag;
        }

        public void setRequestTag(String requestTag) {
            this.requestTag = requestTag;
        }

        public int getRequestId() {
            return requestId;
        }

        public void setRequestId(int requestId) {
            this.requestId = requestId;
        }

        public int getMethodCode() {
            return methodCode;
        }

        public void setMethodCode(int methodCode) {
            this.methodCode = methodCode;
        }

        public String getRequester() {
            return requester;
        }

        public void setRequester(String requester) {
            this.requester = requester;
        }
    }

    public static class Result{}

    public static class Response {

        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ID)
        private int responseId;
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__TAG)
        private String requestTag;
        //TODO Note: Although http has a built in error response distinction, to match http with mqtt and whatever other channel for RPC execution is necessary to verbosely communicate an error.
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ERROR)
        private ResponseError responseError;

        public Response(int responseId, String requestTag, ResponseError responseError) {
            this.responseId = responseId;
            this.requestTag = requestTag;
            this.responseError = responseError;
        }

        public String getRequestTag() {
            return requestTag;
        }

        public void setRequestTag(String requestTag) {
            this.requestTag = requestTag;
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
                    "responseId=" + responseId +
                    ", requestTag='" + requestTag + '\'' +
                    ", responseError=" + responseError +
                    '}';
        }

        public int getResponseId() {
            return responseId;
        }

        public void setResponseId(int responseId) {
            this.responseId = responseId;
        }
    }

    public static class ResponseError {
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__CODE)
        private Integer errorCode;
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__MSGE)
        private String errorMessage;

        public ResponseError(Integer errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public Integer getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(Integer errorCode) {
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
