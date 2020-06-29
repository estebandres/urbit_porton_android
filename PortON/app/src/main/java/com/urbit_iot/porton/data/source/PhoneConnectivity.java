package com.urbit_iot.porton.data.source;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.PatternMatcher;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
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


    public enum ConnectionType {
        UNCONNECTED,
        MOBILE,
        WIFI
    }

    @Inject
    public PhoneConnectivity(Context context) {
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

    public String getWifiAPSSID() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = null;
        String ssid = null;
        if (wifiManager != null) {
            info = wifiManager.getConnectionInfo();
            ssid = info.getSSID();
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
        }

        return ssid;
    }

    public Network getWifiNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            Network[] networks;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                networks = connectivityManager.getAllNetworks();
            } else {
                return null;
            }
            if (networks != null) {
                for (Network network : networks) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                            && networkInfo.isConnected()) {
                        return network; // Grabbing the Network object for later usage
                    }
                }
            }
        }
        return null;
    }

    //TODO refactor to Completable from emitter
    public boolean connectToWifiAP(String ssid, String password) {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            /*
            //WITH NETWORK SUGGESTIONS
            WifiNetworkSuggestion networkSuggestion1 =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid)
                            .setWpa2Passphrase(password)
                            .build();
            List<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
            suggestionsList.add(networkSuggestion1);
            final int status = wifiManager.addNetworkSuggestions(suggestionsList);
            if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                return false;
            }
            final IntentFilter intentFilter =
                    new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);
            final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction()!=null && !intent.getAction().equals(
                            WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                        Log.d("CONNECT","SUGGESTED CONNECTION HAPPENED");
                    }

                }
            };
            mContext.registerReceiver(broadcastReceiver, intentFilter);
            return true;
             */
            //WITH PEER-TO-PEER
            final NetworkSpecifier specifier =
                    new WifiNetworkSpecifier.Builder()
                            .setSsidPattern(new PatternMatcher("urbit", PatternMatcher.PATTERN_PREFIX))
                            //.setBssidPattern(MacAddress.fromString("10:03:23:00:00:00"), MacAddress.fromString("ff:ff:ff:00:00:00"))
                            .build();
            final NetworkRequest request =
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .setNetworkSpecifier(specifier)
                            .build();
            final ConnectivityManager connectivityManager = (ConnectivityManager)
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    Log.d("CONNECT","Network available: " + network.toString());
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    Log.d("CONNECT","Network UNAvailable: ");
                }
            };
            connectivityManager.requestNetwork(request, networkCallback);
            connectivityManager.unregisterNetworkCallback(networkCallback);

            return true;

        } else {
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

                Log.d("CONNECT", "TRYING TO CONNECT TO WIFI AP WITH SSID: " + conf.SSID + " AND PASS: " + conf.preSharedKey);

                wifiManager.addNetwork(conf);

                Log.d("CONNECT", "AFTER CONNECTION ATTEMPT TO WIFI AP WITH SSID: " + conf.SSID + " AND PASS: " + conf.preSharedKey);

                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    Log.e("CONNECT", "FINE LOCATION NOT GRANTED!!");
                    return false;
                }
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
}
