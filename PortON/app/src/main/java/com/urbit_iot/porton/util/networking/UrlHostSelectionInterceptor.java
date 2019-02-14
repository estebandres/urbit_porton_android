package com.urbit_iot.porton.util.networking;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

/**
 * Created by andresteve07 on 12/10/17.
 */

public class UrlHostSelectionInterceptor implements Interceptor {
    private volatile String host;

    public void setHost(String host) {
        this.host = host;
    }

    @Override public okhttp3.Response intercept(Chain chain) throws IOException {
        Chain originalChain = chain;
        Request request = originalChain.request();

        long t1 = System.nanoTime();
        Log.i("interceptor" , String.format("ORIGINAL REQUEST %s on %s%n%s",
                request.url(), originalChain.connection(), request.headers()));

        final Buffer buffer = new Buffer();
        if (request.body() != null) {
            request.body().writeTo(buffer);
        }

        String host = this.host;
        if (host != null) {
            HttpUrl newUrl = request.url().newBuilder()
                    .host(host)
                    .build();

            if (request.url().pathSegments().contains("update")){
                originalChain = chain
                        .withConnectTimeout(20, TimeUnit.SECONDS)
                        .withReadTimeout(35, TimeUnit.SECONDS)
                        .withWriteTimeout(35,TimeUnit.SECONDS);
            }

            request = request.newBuilder()
                    .url(newUrl)
                    .build();
        }

        Log.d("interceptor", "CON_TO: " + originalChain.connectTimeoutMillis()
                + " READ_TO: " + originalChain.readTimeoutMillis()
                + " WRITE_TO: " + originalChain.writeTimeoutMillis());

        if (buffer.size()>0) {
            if (buffer.size()<500){
                Log.d("interceptor", "MODIFY REQUEST: " + request.toString()
                        + "\n" + buffer.readUtf8());
            } else {
                Log.d("interceptor", "MODIFY REQUEST: " + request.toString()
                        + "\n" + buffer.readUtf8(500));
            }
        } else {
            Log.d("interceptor", "MODIFY REQUEST: " + request.toString());
        }

        Response response = originalChain.proceed(request);

        buffer.clear();

        long t2 = System.nanoTime();
        Log.i("interceptor", String.format("RESPONSE: %s in %.1fms%n%s",
                response.request().url(), (t2 - t1) / 1e6d, response.headers()));


        return response;
    }
}
