package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.util.GlobalConstants;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class DeleteUserRPC extends RPC {
    public static final List<Integer> DOC_ERROR_CODES = Arrays.asList(
            HttpURLConnection.HTTP_NOT_FOUND,
            HttpURLConnection.HTTP_INTERNAL_ERROR,
            //Digest Auth + ACL this call is performed with user:pass could fail.
            HttpURLConnection.HTTP_UNAUTHORIZED,
            HttpURLConnection.HTTP_FORBIDDEN);

    public static class Arguments{

        @SerializedName("user_name")
        private String userID;

        public Arguments(String userID) {
            this.userID = userID;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_REQ_ARGS_ATTR_NAME)
        private DeleteUserRPC.Arguments methodArguments;

        public Request(Arguments args, String uModTag, int id) {
            super("Admin.DeleteUser",uModTag,id);
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
        @SerializedName("Response")
        private String response;
        public Result(String response){
            this.response = response;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "response='" + response + '\'' +
                    '}';
        }
    }

    public static class Response extends RPC.Response {

        @SerializedName(GlobalConstants.RPC_SUCC_RESP_RESULT_ATTR_NAME)
        private DeleteUserRPC.Result responseResult;

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
