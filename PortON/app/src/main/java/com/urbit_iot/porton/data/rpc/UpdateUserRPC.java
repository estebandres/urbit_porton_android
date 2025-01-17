package com.urbit_iot.porton.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.porton.util.GlobalConstants;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class UpdateUserRPC extends RPC {
    public static final List<Integer> DOC_ERROR_CODES = Arrays.asList(
            //HttpURLConnection.HTTP_NOT_FOUND,
            //HttpURLConnection.HTTP_BAD_REQUEST,
            //Digest Auth + ACL this call is performed with user:pass COULD fail.
            HttpURLConnection.HTTP_UNAUTHORIZED,
            HttpURLConnection.HTTP_FORBIDDEN,
            //(Mongoose side) Any Error.
            HttpURLConnection.HTTP_INTERNAL_ERROR);

    public static class Arguments{

        @SerializedName("user_name")
        private String userID;
        @SerializedName("user_type")
        private APIUserType userType;

        public Arguments(String userID, APIUserType userType) {
            this.userID = userID;
            this.userType = userType;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public APIUserType getUserType() {
            return userType;
        }

        public void setUserType(APIUserType userType) {
            this.userType = userType;
        }
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ARGS)
        private UpdateUserRPC.Arguments methodArguments;

        public Request(Arguments args, String requester, String uModTag, int id) {
            super(
                    GlobalConstants.RPC_METHOD_NAME___UPDATE_USER,
                    GlobalConstants.RPC_METHOD_CODE__UPDATE_USER,
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
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__MSGE)
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
        private UpdateUserRPC.Result responseResult;

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