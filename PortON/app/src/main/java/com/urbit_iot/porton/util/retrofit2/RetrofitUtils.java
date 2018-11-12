package com.urbit_iot.porton.util.retrofit2;

import com.google.gson.Gson;
import com.urbit_iot.porton.data.rpc.RPC;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Response;

/**
 * Created by andresteve07 on 2/16/18.
 */

public class RetrofitUtils {

    private Gson gsonInstance;

    @Inject
    public RetrofitUtils(Gson gsonInstance) {
        this.gsonInstance = gsonInstance;
    }

    public RPC.ResponseError parseError(Response<?> response) {

        RPC.ResponseError responseError;
        try {
            responseError = this.gsonInstance.fromJson(response.errorBody().string(), RPC.ResponseError.class);
        } catch (IOException e) {
            return new RPC.ResponseError(0,"");
        }

        return responseError;
    }
}
