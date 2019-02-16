package com.urbit_iot.porton.data.source.lan;

import android.util.Log;

import com.urbit_iot.porton.util.GlobalConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import rx.Completable;
import rx.Emitter;
import rx.Observable;

/**
 * Created by andresteve07 on 05/07/18.
 */

public class TCPScanClient {
    static public Observable<String> tcpEchoRequest(String ipAddressString, int port){
        Observable<String> requestObservable = Observable.create(stringEmitter -> {
            Socket clientSocket = null;
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                //clientSocket = new Socket(ipAddressString, port);
                clientSocket = new Socket();
                clientSocket.setSoTimeout(1500);
                clientSocket.connect(new InetSocketAddress(ipAddressString,port),800);

                Log.d("TCP_ECHO_CLIENT", "SOCKET connected ON " + Thread.currentThread().getName());
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out.println("HELLO");
                Log.d("TCP_ECHO_CLIENT", "HELLO sent ON " + Thread.currentThread().getName());
                String response = in.readLine();
                Log.d("TCP_ECHO_CLIENT", "ECHO read: **" +response+ "** finished ON " + Thread.currentThread().getName());
                stringEmitter.onNext(response);
            } catch (UnknownHostException e) {
                //errorLogging(e,ipAddressString);
                stringEmitter.onCompleted();
            } catch (SocketTimeoutException e) {
                //errorLogging(e,ipAddressString);
                stringEmitter.onCompleted();
            } catch (IOException e) {
                //errorLogging(e,ipAddressString);
                stringEmitter.onCompleted();
            }
            finally {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Log.e("TCP_ECHO_CLIENT","IN fail to close.");
                    }
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        Log.e("TCP_ECHO_CLIENT","SOCKET fail to close.");
                    }
                }
                stringEmitter.onCompleted();
            }
        }, Emitter.BackpressureMode.BUFFER);
        return requestObservable;
    }

    static public Completable tcpEchoCompletable(String ipAddressString, int port){
        return Completable.fromEmitter(completableEmitter -> {
            Socket clientSocket = null;
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                //clientSocket = new Socket(ipAddressString, port);
                clientSocket = new Socket();
                clientSocket.setSoTimeout(1500);
                clientSocket.connect(new InetSocketAddress(ipAddressString,port),800);

                Log.d("TCP_ECHO_CLIENT", "SOCKET connected ON " + Thread.currentThread().getName());
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out.println("HELLO");
                Log.d("TCP_ECHO_CLIENT", "HELLO sent ON " + Thread.currentThread().getName());
                String response = in.readLine();
                Log.d("TCP_ECHO_CLIENT", "ECHO read: **" +response+ "** finished ON " + Thread.currentThread().getName());
                if (response.matches(GlobalConstants.URBIT_PREFIX + GlobalConstants.DEVICE_UUID_REGEX)){
                    completableEmitter.onCompleted();
                } else {
                    completableEmitter.onError(new Exception("Response was: " + response));
                }
            } catch (UnknownHostException e) {
                //errorLogging(e,ipAddressString);
                completableEmitter.onError(e);
            } catch (SocketTimeoutException e) {
                //errorLogging(e,ipAddressString);
                completableEmitter.onError(e);
            } catch (IOException e) {
                //errorLogging(e,ipAddressString);
                completableEmitter.onError(e);
            }
            finally {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Log.e("TCP_ECHO_CLIENT","IN fail to close.");
                    }
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        Log.e("TCP_ECHO_CLIENT","SOCKET fail to close.");
                    }
                }
            }
        });
    }


    static public void errorLogging(Throwable throwable, String ipAddress){
        Log.e("TCP_ECHO_CLIENT", "FAILED FOR: "
                + ipAddress + " CAUSE: "
                + throwable.getClass().getSimpleName()
                + " -> " + throwable.getMessage() + "  THREAD: " + Thread.currentThread().getName());
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
