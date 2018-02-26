package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.util.GlobalConstants;

import java.util.List;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class GetUsersRPC extends RPC {
    public static class Arguments{
        public Arguments(){}
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_REQ_ARGS_ATTR_NAME)
        private GetUsersRPC.Arguments methodArguments;
        public Request(Arguments args, String callTag, int id) {
            super("Admin.GetUsers",callTag,id);
            this.methodArguments = args;
        }

        public Arguments getMethodArguments() {
            return methodArguments;
        }

        public void setMethodArguments(Arguments methodArguments) {
            this.methodArguments = methodArguments;
        }
    }

    public static class UserResult{
        @SerializedName("user_name")
        private String userName;
        @SerializedName("user_type")
        private APIUserType userType;

        public UserResult(String userName, APIUserType userType) {
            this.userName = userName;
            this.userType = userType;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public APIUserType getUserType() {
            return userType;
        }

        public void setUserType(APIUserType userType) {
            this.userType = userType;
        }

        @Override
        public String toString() {
            return "UserResult{" +
                    "userName='" + userName + '\'' +
                    ", userType=" + userType +
                    '}';
        }
    }

    public static class Result{
        @SerializedName("Response")
        private List<UserResult> users;

        public Result(List<UserResult> users) {
            this.users = users;
        }

        public List<UserResult> getUsers() {
            return users;
        }

        public void setUsers(List<UserResult> users) {
            this.users = users;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "users=" + users +
                    '}';
        }
    }

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_SUCC_RESP_RESULT_ATTR_NAME)
        private GetUsersRPC.Result responseResult;

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
