package com.urbit_iot.onekey.data.source;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import javax.inject.Inject;

/**
 * Created by andresteve07 on 24/07/18.
 */

public class PhoneConnectivityInfo {
    private Context mContext;
    //private WifiManager wifiManager;
    //private ConnectivityManager connectivityManager;


    public enum ConnectionType{
        UNCONNECTED,
        MOBILE,
        WIFI
    }

    @Inject
    public PhoneConnectivityInfo(Context context){
        this.mContext = context;
        //this. wifiManager = (WifiManager) this.mContext.getSystemService (Context.WIFI_SERVICE);
        //this.connectivityManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public ConnectionType getConnectionTypeA() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    switch (activeNetworkInfo.getType()) {
                        case ConnectivityManager.TYPE_WIFI:
                            return ConnectionType.WIFI;
                        case ConnectivityManager.TYPE_MOBILE:
                            return ConnectionType.MOBILE;
                        default:
                            break;
                    }
                }
            } else {
                NetworkInfo networkInfo;
                networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null) {
                    return ConnectionType.WIFI;
                } else {
                    networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                    if (networkInfo != null) {
                        return ConnectionType.MOBILE;
                    }
                }
            }
        }
        return ConnectionType.UNCONNECTED;
    }

    public ConnectionType getConnectionType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo;
            networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo != null && networkInfo.isConnected()) {
                return ConnectionType.WIFI;
            } else {
                networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if (networkInfo != null && networkInfo.isConnected()) {
                    return ConnectionType.MOBILE;
                }
            }
        }
        return ConnectionType.UNCONNECTED;
    }

    public String getWifiAPSSID(){
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService (Context.WIFI_SERVICE);
        WifiInfo info = null;
        String ssid = null;
        if (wifiManager != null) {
            info = wifiManager.getConnectionInfo();
            ssid  = info.getSSID();
            if (ssid.startsWith("\"") && ssid.endsWith("\"")){
                ssid = ssid.substring(1, ssid.length()-1);
            }
        }

        return ssid;
    }
}
