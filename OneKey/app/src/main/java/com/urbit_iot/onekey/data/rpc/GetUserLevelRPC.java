package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.util.GlobalConstants;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andresteve07 on 12/1/17.
 */

public class GetUserLevelRPC {
    public static final List<Integer> ALLOWED_ERROR_CODES = Arrays.asList(
            //HttpURLConnection.HTTP_BAD_REQUEST,
            //HttpURLConnection.HTTP_NOT_FOUND,
            //(Mongoose side) Digest Auth + ACL this call is performed with urbit:urbit so never should fail.
            HttpURLConnection.HTTP_UNAUTHORIZED,
            HttpURLConnection.HTTP_FORBIDDEN,
            //(Mongoose side) Any Error.
            HttpURLConnection.HTTP_INTERNAL_ERROR);

    public static class Arguments{
        @SerializedName("user_name")
        private String userName;

        public Arguments(String userName){
            this.userName = userName;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_FIELD_NAME__ARGS)
        private GetUserLevelRPC.Arguments methodArguments;
        public Request(Arguments args, String callTag, int id){
            super(GlobalConstants.RPC_METHOD_NAME__USER_STATUS,
                    GlobalConstants.RPC_METHOD_CODE__USER_STATUS,
                    callTag,id);
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
        @SerializedName("user_name")
        private String userName;

        @SerializedName("user_type")
        private APIUserType APIUserType;

        public Result(String userName, APIUserType APIUserType){
            this.userName = userName;
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

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
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
        private GetUserLevelRPC.Result responseResult;
        public Response(Result result, int responseId, String callTag, RPC.ResponseError responseError){
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
                    "} ";
        }
    }

}
