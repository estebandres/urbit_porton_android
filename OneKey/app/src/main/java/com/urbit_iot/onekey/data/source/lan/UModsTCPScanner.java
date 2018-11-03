package com.urbit_iot.onekey.data.source.lan;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

//import io.netty.buffer.ByteBuf;
//import io.reactivex.netty.protocol.tcp.client.TcpClient;
import rx.Observable;

/**
 * Created by andresteve07 on 05/07/18.
 */

public class UModsTCPScanner {
    private Context mContext;
    private LanAddressesCalculation addressesCalculator;
    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    @Inject
    public UModsTCPScanner(Context mContext,
                           @NonNull BaseSchedulerProvider mSchedulerProvider) {
        this.mContext = mContext;
        this.mSchedulerProvider = mSchedulerProvider;
        this.setupCalculator();
        Observable.just(true)
                .delay(3000L,TimeUnit.MILLISECONDS)
                .doOnNext(aBoolean -> {
                    Log.d("TCP_SCAN", "TEST ECHO: ");
                })
                //.flatMap(aBoolean -> TCPScanClient.tcpEchoRequest("172.18.191.68", 7777))
                .subscribeOn(this.mSchedulerProvider.io()).subscribe();
    }
    public void setupCalculator(){
        WifiManager wifi = (WifiManager)
                mContext.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = null;
        boolean wifiConnectionIsActive = false;
        //TODO Code duplication!
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager!=null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    wifiConnectionIsActive = activeNetworkInfo.isConnected();
                } else {
                    wifiConnectionIsActive = false;
                }
            } else {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null) {
                    wifiConnectionIsActive = networkInfo.isConnected();
                } else {
                    wifiConnectionIsActive = false;
                }
            }
        }
        if (wifi != null && wifi.isWifiEnabled() && wifiConnectionIsActive) {
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
                    Log.e("TCP_SCAN", "" + e.getMessage(),e);
                }
            }
            this.addressesCalculator = new PhoneIPAddressRadius(phoneIPAddress, subnetMaskAddress);
        } else {
            this.addressesCalculator = ArrayList::new;
        }
    }

    private boolean phoneConnectedToAPModeUMod(){
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService (Context.WIFI_SERVICE);
        WifiInfo info = null;
        String ssid = null;
        if (wifiManager != null) {
            info = wifiManager.getConnectionInfo();
            ssid  = info.getSSID();
            if (ssid.startsWith("\"") && ssid.endsWith("\"")){
                ssid = ssid.substring(1, ssid.length()-1);
            }
            return ssid.matches(GlobalConstants.URBIT_PREFIX + GlobalConstants.DEVICE_UUID_REGEX);
        }
        return true;//This will cancel the TCP scan for the cases when WiFi SSID couldn't be gotten.
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

    /*
    public Observable<UMod> scanForUMods(){
        return Observable.empty();
    }
    */

    public Observable<UMod> scanForUMods(){
        if (phoneConnectedToAPModeUMod()){
            return Observable.empty();
        }
        List<Long> allAddresses = this.addressesCalculator.calculateAddresses();

        return Observable.from(allAddresses)
                .flatMap(ipAddressAsLong ->
                        Observable.just(longToStringIP(ipAddressAsLong))
                        .subscribeOn(mSchedulerProvider.io())
                        .flatMap(this::performTCPEcho), 15)
                .onErrorResumeNext(throwable -> {
                    if (throwable != null){
                        Log.e("TCP_SCAN", ""
                                + throwable.getClass().getSimpleName()
                                + "   " + throwable.getMessage());
                    }
                    return Observable.just(null);
                })//TODO Bad practice using null to ignore errors!!
                .filter(uMod -> uMod != null)
                .doOnNext(uMod -> Log.d("tcp_scan", uMod.getUUID()));
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

    private Observable<UMod> performTCPEcho(String ipAddressName){

        return TCPScanClient.tcpEchoRequest(ipAddressName,GlobalConstants.UMOD__TCP_ECHO_PORT)
                .filter(possibleUModResp -> possibleUModResp.matches(
                        GlobalConstants.URBIT_PREFIX
                                + GlobalConstants.DEVICE_UUID_REGEX))
                .map(uModResp -> {
                    String uModUUID = getUUIDFromUModAdvertisedID(uModResp);
                    return new UMod(uModUUID,
                            ipAddressName,
                            true);
                });

    }

    private String getUUIDFromUModAdvertisedID(String hostName){
        String uModUUID = "DEFAULTUUID";

        Pattern pattern = Pattern.compile(
                GlobalConstants.URBIT_PREFIX
                + GlobalConstants.DEVICE_UUID_REGEX);
        Matcher matcher = pattern.matcher(hostName);

        if (matcher.find()){
            uModUUID = matcher.group(1);
        }
        return uModUUID;
    }
}
