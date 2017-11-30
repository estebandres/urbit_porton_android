package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class DeleteUserRPC extends RPC {

    public static class Arguments extends RPC.Arguments{

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

        public Request(Arguments args, String uModTag, int id) {
            super("Admin.DeleteUser",args,uModTag,id);
        }

        public Arguments getMethodArguments(){
            return (Arguments) super.getMethodArguments();
        }
    }

    public static class Result extends RPC.Result{
        public Result(){}
    }

    public static class SuccessResponse extends RPC.SuccessResponse{

        public SuccessResponse(Result result, String callTag) {
            super(result, callTag);
        }

        public Result getResponseResult(){
            return (Result) super.getResponseResult();
        }
    }
}
