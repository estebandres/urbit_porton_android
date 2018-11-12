package com.urbit_iot.porton.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.util.GlobalConstants;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class CreateUserRPC extends RPC {
    public static final List<Integer> DOC_ERROR_CODES = Arrays.asList(
                //HttpURLConnection.HTTP_BAD_REQUEST,
                //HttpURLConnection.HTTP_INTERNAL_ERROR,
                //HttpURLConnection.HTTP_BAD_REQUEST,
                //HttpURLConnection.HTTP_PRECON_FAILED,
                //(Mongoose side) Digest Auth + ACL this call is performed with urbit:urbit so never should fail.
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_FORBIDDEN,
                //(Mongoose side) Any Error.
                HttpURLConnection.HTTP_INTERNAL_ERROR);

    public static class Arguments{

        @SerializedName("credentials")
        private String credentials;

        public Arguments(String credentials) {
            this.credentials = credentials;
        }

        public String getCredentials() {
            return credentials;
        }

        public void setCredentials(String credentials) {
            this.credentials = credentials;
        }
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ARGS)
        private CreateUserRPC.Arguments methodArguments;

        public Request(Arguments args, String requester, String callTag, int id) {
            super(
                    GlobalConstants.RPC_METHOD_NAME__CREATE_USER,
                    GlobalConstants.RPC_METHOD_CODE__CREATE_USER,
                    requester,
                    callTag,
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
        @SerializedName("user_type")
        private APIUserType userType;

        public Result(APIUserType userType){
            this.userType = userType;
        }

        public APIUserType getUserType() {
            return userType;
        }

        public UModUser.Level getUserLevel(){
            return this.userType.asUModUserLevel();
        }

        public void setUserType(APIUserType userType) {
            this.userType = userType;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "userType=" + userType +
                    '}';
        }
    }

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__RESULT)
        private CreateUserRPC.Result responseResult;

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