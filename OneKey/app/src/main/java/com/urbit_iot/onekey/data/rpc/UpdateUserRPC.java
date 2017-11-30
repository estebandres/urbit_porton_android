package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.data.UModUser;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class UpdateUserRPC extends RPC {
    public static class Arguments extends RPC.Arguments{

        @SerializedName("user_name")
        private String userID;
        @SerializedName("user_type")
        private UModUser.UModUserStatus userLevel;

        public Arguments(String userID, UModUser.UModUserStatus userLevel) {
            this.userID = userID;
            this.userLevel = userLevel;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public UModUser.UModUserStatus getUserLevel() {
            return userLevel;
        }

        public void setUserLevel(UModUser.UModUserStatus userLevel) {
            this.userLevel = userLevel;
        }
    }

    public static class Request extends RPC.Request{

        public Request(Arguments args, String uModTag, int id) {
            super("UpdateUser",args,uModTag,id);
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