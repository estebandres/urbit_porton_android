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
        public Arguments(){}
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

    public static class Result{

        @SerializedName("level")
        private UModUser.UModUserStatus level;

        public Result(UModUser.UModUserStatus level){
            this.level = level;
        }

        public UModUser.UModUserStatus getLevel() {
            return level;
        }

        public void setLevel(UModUser.UModUserStatus level) {
            this.level = level;
        }
    }

    public static class SuccessResponse extends RPC.SuccessResponse{
        @SerializedName(GlobalConstants.RPC_SUCC_RESP_RESULT_ATTR_NAME)
        private GetMyUserLevelRPC.Result responseResult;
        public SuccessResponse(Result result, String callTag, RPC.ResponseError responseError){
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
            return "SuccessResponse{" +
                    "callTag=" + this.getCallTag() + ", " +
                    "responseResult=" + responseResult +
                    "} ";
        }
    }

}
