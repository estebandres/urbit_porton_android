package com.urbit_iot.porton.util.networking;

import android.util.Log;

import java.io.IOException;

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

        Request request = chain.request();

        long t1 = System.nanoTime();
        Log.i("interceptor" , String.format("ORIGINAL REQUEST %s on %s%n%s",
                request.url(), chain.connection(), request.headers()));

        final Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        String host = this.host;
        if (host != null) {
            HttpUrl newUrl = request.url().newBuilder()
                    .host(host)
                    .build();
            request = request.newBuilder()
                    .url(newUrl)
                    .build();
        }
        Log.d("interceptor", "MODIFY REQUEST: " + request.toString()
                + "\n" + buffer.readUtf8());

        Response response = chain.proceed(request);

        buffer.clear();

        long t2 = System.nanoTime();
        Log.i("interceptor", String.format("RESPONSE: %s in %.1fms%n%s",
                response.request().url(), (t2 - t1) / 1e6d, response.headers()));


        return response;
    }
}
