package com.urbit_iot.porton.data.source;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by andresteve07 on 24/07/18.
 */

public class PhoneConnectivity {
    private Context mContext;
    //private WifiManager wifiManager;
    //private ConnectivityManager connectivityManager;


    public enum ConnectionType{
        UNCONNECTED,
        MOBILE,
        WIFI
    }

    @Inject
    public PhoneConnectivity(Context context){
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

    public Network getWifiNetwork(){
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            Network[] networks;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                networks = connectivityManager.getAllNetworks();
            } else {
                return null;
            }
            if (networks!=null){
                for(Network network : networks){
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                    if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                            && networkInfo.isConnected()){
                        return network; // Grabbing the Network object for later usage
                    }
                }
            }
        }
        return null;
    }

    //TODO refactor to Completable from emitter
    public boolean connectToWifiAP(String ssid, String password){
        try {
            WifiConfiguration conf = new WifiConfiguration();

            conf.SSID = "\"" + ssid + "\"";   // Please note the quotes. String should contain SSID in quotes
            conf.preSharedKey = "\"" + password + "\"";

            conf.status = WifiConfiguration.Status.ENABLED;
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

            Log.d("CONNECT","TRYING TO CONNECT TO WIFI AP WITH SSID: " + conf.SSID + " AND PASS: " + conf.preSharedKey);

            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                wifiManager.addNetwork(conf);
            } else {
                return false;
            }

            Log.d("CONNECT","AFTER CONNECTION ATTEMPT TO WIFI AP WITH SSID: " + conf.SSID + " AND PASS: " + conf.preSharedKey);

            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    Log.d("CONNECT","RECONNECTION TO WIFI AP WITH SSID: " + conf.SSID + " AND PASS: " + conf.preSharedKey);
                    break;
                }
            }
            //WiFi Connection success, return true
            return true;
        } catch (Exception ex) {
            Log.e("CONNECT","FAILED TO CONNECT TO WIFI AP WITH SSID: " + ssid + " AND PASS: " + password, ex);
            return false;
        }
    }
}
