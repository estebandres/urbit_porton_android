package com.urbit_iot.onekey.util.networking;

import android.util.Log;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
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
        Log.d("interceptor", "REQUEST: " + request.toString() + "\n" + buffer.readUtf8());
        return chain.proceed(request);
    }
}
