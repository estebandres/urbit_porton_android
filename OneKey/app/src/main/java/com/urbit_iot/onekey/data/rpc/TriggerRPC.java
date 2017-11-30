package com.urbit_iot.onekey.data.rpc;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class TriggerRPC extends RPC {
    public static class Arguments extends RPC.Arguments{
        public Arguments(){}
    }

    public static class Request extends RPC.Request{

        public Request(Arguments args, String callTag, int id) {
            super("Trigger",args,callTag,id);
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
    /*
    public static class FailureResponse extends RPC.FailureResponse{
        public FailureResponse(int errorCode, String errorMessage){
            super(errorCode,errorMessage);
        }
    }
    */
}
