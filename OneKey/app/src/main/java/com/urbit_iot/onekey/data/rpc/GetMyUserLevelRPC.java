package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.data.UModUser;
import com.urbit_iot.onekey.util.GlobalConstants;

import java.util.Random;

/**
 * Created by andresteve07 on 12/1/17.
 */

public class GetMyUserLevelRPC {
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
        @SerializedName(GlobalConstants.RPC_REQ_ARGS_ATTR_NAME)
        private GetMyUserLevelRPC.Arguments methodArguments;
        public Request(Arguments args, String uModTag){
            super("Gest.GetUserStatus", uModTag, (new Random().nextInt()));
            this.methodArguments = args;
        }

        public Arguments getMethodArguments() {
            return methodArguments;
        }

        public void setMethodArguments(Arguments methodArguments) {
            this.methodArguments = methodArguments;
        }
    }

    public enum UModUserType{
        Admin {
            @Override
            public UModUser.Level asUModUserLevel() {
                return UModUser.Level.ADMINISTRATOR;
            }
        },
        Guest {
            @Override
            public UModUser.Level asUModUserLevel() {
                return UModUser.Level.PENDING;
            }
        },
        User {
            @Override
            public UModUser.Level asUModUserLevel() {
                return UModUser.Level.AUTHORIZED;
            }
        };

        public abstract UModUser.Level asUModUserLevel();

    }
    public static class Result{

        @SerializedName("user_type")
        private UModUserType userType;

        public Result(UModUserType userType){
            this.userType = userType;
        }

        public UModUserType getUserType() {
            return userType;
        }

        public UModUser.Level getUserLevel(){
            return this.userType.asUModUserLevel();
        }

        public void setUserType(UModUserType userType) {
            this.userType = userType;
        }
    }

    public static class Response extends RPC.Response {
        @SerializedName(GlobalConstants.RPC_SUCC_RESP_RESULT_ATTR_NAME)
        private GetMyUserLevelRPC.Result responseResult;
        public Response(Result result, String callTag, RPC.ResponseError responseError){
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
                    "} ";
        }
    }

}
