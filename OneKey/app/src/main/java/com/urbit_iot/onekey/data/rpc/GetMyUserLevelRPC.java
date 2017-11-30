package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.data.UModUser;

import java.util.Random;

/**
 * Created by andresteve07 on 12/1/17.
 */

public class GetMyUserLevelRPC {
    public static class Arguments extends RPC.Arguments{
        public Arguments(){}
    }

    public static class Request extends RPC.Request{
        public Request(Arguments args, String uModTag){
            super("Gest.GetUserStatus",args, uModTag, (new Random().nextInt()));
        }
    }

    public static class Result extends RPC.Result{

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
        public SuccessResponse(Result result, String callTag){
            super(result,callTag);
        }
        public Result getResponseResult(){
            return (Result) super.getResponseResult();
        }
    }

    /*
    public static class FailureResponse extends RPC.FailureResponse{
        public FailureResponse(int errorCode, String errorMessage){
            super(errorCode,errorMessage);
        }
    }
    */

}
