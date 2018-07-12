package com.urbit_iot.onekey.data.source.lan;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.urbit_iot.onekey.data.UMod;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.tcp.client.TcpClient;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by andresteve07 on 05/07/18.
 */

public class UModsTCPScanner {
    private Context mContext;
    private LanAddressesCalculation addressesCalculator;

    @Inject
    public UModsTCPScanner(Context mContext) {
        this.mContext = mContext;
        this.setupCalculator();
        Observable.just(true)
                .delay(3000L,TimeUnit.MILLISECONDS)
                .doOnNext(aBoolean -> {
                    Log.d("TCP_SCAN", "TEST ECHO: ");
                })
                //.flatMap(aBoolean -> TCPScanClient.tcpEchoRequest("172.18.191.68", 7777))
                .subscribeOn(Schedulers.io()).subscribe();
    }
    public void setupCalculator(){
        WifiManager wifi = (WifiManager)
                mContext.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = null;
        if (wifi != null && wifi.isWifiEnabled()) {
            dhcpInfo = wifi.getDhcpInfo();
            Long phoneIPAddress = convertReverseInt2ByteArray(dhcpInfo.ipAddress);
            Long subnetMaskAddress = convertReverseInt2ByteArray(dhcpInfo.netmask);
            if (subnetMaskAddress == 0) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(longToStringIP(phoneIPAddress));
                    NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
                    for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                        subnetMaskAddress = netmaskPrefixToLongAddress(address.getNetworkPrefixLength());
                    }
                } catch (IOException e) {
                    Log.e("TCP_SCAN", e.getMessage());
                }
            }
            this.addressesCalculator = new PhoneIPAddressRadius(phoneIPAddress, subnetMaskAddress);
        } else {
            this.addressesCalculator = ArrayList::new;
        }
    }
    private long netmaskPrefixToLongAddress(short netPrefix){
        long netmaskAddress = 0;
        for (int i=0; i<netPrefix;i++){
            System.out.println((1L<<31-i));
            netmaskAddress = netmaskAddress | (1L<<31-i);
        }
        return netmaskAddress;
    }

    private long convertReverseInt2ByteArray(int ipReversedCode) {
        long ipDirectCode = 0L;
        ipDirectCode += ((ipReversedCode >> 24) & 0xFFL)
                + ((ipReversedCode >> 8) & 0xFF00L)
                + ((ipReversedCode << 8) & 0xFF0000L)
                + (((ipReversedCode & 0xFFL) << 24) & 0xFF000000L);
        return ipDirectCode;
    }

    public Observable<UMod> scanForUModsA(){
        return Observable.empty();
    }

    public Observable<UMod> scanForUMods(){
        List<Long> allAddresses = this.addressesCalculator.calculateAddresses();
        //List<List<Long>> addressesChunks = chopList(allAddresses,50);
        //Log.d("TCP_SCAN", "Chunks of 50" + addressesChunks.size());

        return Observable.from(allAddresses)
                .flatMap(ipAddressAsLong ->
                        Observable.just(longToStringIP(ipAddressAsLong))
                        .subscribeOn(Schedulers.io())
                        .flatMap(this::performTCPEcho), 15)
                .onErrorResumeNext(throwable -> {
                    if (throwable != null){
                        Log.e("TCP_SCAN", ""
                                + throwable.getClass().getSimpleName()
                                + "   " + throwable.getMessage());
                    }
                    return Observable.just(null);
                })//TODO Bad practice using null to ignore errors!!
                .doOnNext(uMod -> Log.d("tcp_scan", uMod.getUUID()))
                .filter(uMod -> uMod != null);
    }

    public Observable<UMod> scanForUModsC(){
        List<Long> allAddresses = this.addressesCalculator.calculateAddresses();
        List<List<Long>> addressesChunks = chopList(allAddresses,50);
        Log.d("TCP_SCAN", "Chunks of 50" + addressesChunks.size());

        return Observable.from(addressesChunks)
                .flatMap(Observable::from)
                .map(UModsTCPScanner::longToStringIP)
                .flatMap(this::performTCPEcho)
                .onErrorResumeNext(throwable -> {
                    if (throwable != null){
                        Log.e("TCP_SCAN", ""
                                + throwable.getClass().getSimpleName()
                                + "   " + throwable.getMessage());
                    }
                    return Observable.just(null);
                })
                .doOnNext(uMod -> Log.d("tcp_scan", uMod.getUUID()))
                .filter(uMod -> uMod != null);
    }

    static <T> List<List<T>> chopList(List<T> list, final int L) {
        List<List<T>> parts = new ArrayList<>();
        final int N = list.size();
        for (int i = 0; i < N; i += L) {
            parts.add(new ArrayList<>(
                    list.subList(i, Math.min(N, i + L)))
            );
        }
        return parts;
    }

    private static String longToStringIP(long ip) {
        return  ((ip >> 24) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >>  8) & 0xFF) + "." +
                ( ip        & 0xFF);
    }

    /*
    private Observable<UMod> performTCPEcho(String ipAddressName){

        SocketAddress serverAddress = null;
        try {
            serverAddress = new InetSocketAddress(InetAddress.getByName(ipAddressName),7777);
        } catch (UnknownHostException e) {
            Log.e("TCP_ECHO", e.getMessage());
            return Observable.error(e);
        }

        return TcpClient.newClient(serverAddress)
                .readTimeOut(2300, TimeUnit.MILLISECONDS)
                .createConnectionRequest()
                .flatMap(connection ->{
                    Log.d("ECHO_REQ", "CONNECTED TO " + ipAddressName);
                    return connection.writeString(Observable.just("HELLO"))
                            .cast(ByteBuf.class)//since the writing don't emmit any object but competes in order to concat it should maintain the emissions type
                            .concatWith(connection.getInput());
                        }
                )
                .take(1)
                .map(byteBuf -> byteBuf.toString(Charset.defaultCharset()))
                .doOnNext(stringResponse -> Log.d("tcp_scan", "ECHO_RESP:  " + stringResponse))
                .filter(possibleUModResp -> possibleUModResp.contains("urbit"))
                .map(uModResp -> {
                    String uModUUID = getUUIDFromUModAdvertisedID(uModResp);
                    return new UMod(uModUUID,
                            ipAddressName,
                            true);
                })
                .doOnCompleted(() -> Log.d("TCP_SCAN", "COMPLETED"));
    }
     */
    private Observable<UMod> performTCPEcho(String ipAddressName){

        return TCPScanClient.tcpEchoRequest(ipAddressName,7777)
                .filter(possibleUModResp -> possibleUModResp.contains("urbit"))
                .map(uModResp -> {
                    String uModUUID = getUUIDFromUModAdvertisedID(uModResp);
                    return new UMod(uModUUID,
                            ipAddressName,
                            true);
                });
        //return Observable.empty();

    }

    private String getUUIDFromUModAdvertisedID(String hostName){
        String uModUUID = "DEFAULTUUID";

        Pattern pattern = Pattern.compile("urbit-(.*?)\\.local\\.");
        Matcher matcher = pattern.matcher(hostName);

        if (matcher.find()){
            uModUUID = matcher.group(1);
        }
        return uModUUID;
    }
}
