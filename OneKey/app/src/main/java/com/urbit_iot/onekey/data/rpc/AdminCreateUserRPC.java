package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.util.GlobalConstants;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class AdminCreateUserRPC extends RPC {
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
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ARGS)
        private AdminCreateUserRPC.Arguments methodArguments;

        public Request(Arguments args, String requester, String uModTag, int id) {
            super(
                    GlobalConstants.RPC_METHOD_NAME___ADMIN_CREATE_USER,
                    GlobalConstants.RPC_METHOD_CODE__ADMIN_CREATE_USER,
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
        @SerializedName("user_type")
        private APIUserType APIUserType;

        public Result(APIUserType APIUserType){
            this.APIUserType = APIUserType;
        }

        public APIUserType getAPIUserType() {
            return APIUserType;
        }

        public UModUser.Level getUserLevel(){
            return this.APIUserType.asUModUserLevel();
        }

        public void setAPIUserType(APIUserType APIUserType) {
            this.APIUserType = APIUserType;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "APIUserType=" + APIUserType +
                    '}';
        }
    }

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__RESULT)
        private AdminCreateUserRPC.Result responseResult;

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