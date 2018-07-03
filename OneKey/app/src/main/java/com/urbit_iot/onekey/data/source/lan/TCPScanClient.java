package com.urbit_iot.onekey.data.source.lan;

import android.util.Log;

import com.urbit_iot.onekey.data.UMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import rx.Emitter;
import rx.Observable;

/**
 * Created by andresteve07 on 05/07/18.
 */

public class TCPScanClient {
    static public Observable<String> tcpEchoRequest(String ipAddressString, int port){
        Observable<String> requestObservable = Observable.create(stringEmitter -> {
            Socket clientSocket;
            PrintWriter out;
            BufferedReader in;
            try {
                clientSocket = new Socket(ipAddressString, port);
                clientSocket.setSoTimeout(2500);
                Log.d("TCP_ECHO_CLIENT", "connected");
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out.println("HELLO");
                Log.d("TCP_ECHO_CLIENT", "hello sent");
                String response = in.readLine();
                Log.d("TCP_ECHO_CLIENT", "read finished");
                stringEmitter.onNext(response);
                out.close();
                in.close();
                clientSocket.close();
                stringEmitter.onCompleted();
            } catch (UnknownHostException e) {
                //e.printStackTrace();
                //stringEmitter.onError(e);
                Log.e("TCP_ECHO_CLIENT", ""
                        + e.getClass().getSimpleName()
                        + "   " + e.getMessage());
                stringEmitter.onCompleted();
            } catch (IOException e) {
                //stringEmitter.onError(e);
                //e.printStackTrace();
                Log.e("TCP_ECHO_CLIENT", ""
                        + e.getClass().getSimpleName()
                        + "   " + e.getMessage());
                stringEmitter.onCompleted();
            }
        }, Emitter.BackpressureMode.BUFFER);
        return requestObservable;
    }

    static public void tcpEchoRequestA(String ipAddressString, int port){
        Socket clientSocket;
        PrintWriter out;
        BufferedReader in;
        try {
            clientSocket = new Socket(ipAddressString, port);
            clientSocket.setSoTimeout(2500);
            Log.d("TCP_ECHO_CLIENT", "connected");
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out.println("HELLO");
            Log.d("TCP_ECHO_CLIENT", "hello sent");
            String response = in.readLine();
            Log.d("TCP_ECHO_CLIENT", "read finished");
            out.close();
            in.close();
            clientSocket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
