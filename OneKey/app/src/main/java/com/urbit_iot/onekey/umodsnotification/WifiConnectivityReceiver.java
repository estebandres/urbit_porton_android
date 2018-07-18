package com.urbit_iot.onekey.umodsnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.urbit_iot.onekey.util.GlobalConstants;

/**
 * Created by andresteve07 on 18/05/18.
 */

public class WifiConnectivityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, UModsNotifService.class);
        Log.d("WiFiConnBR", "WiFi connection event!");
        if (this.isWiFiConnected(context)){
            Log.d("WiFiConnBR", "WiFi is connected!");
            serviceIntent.setAction(GlobalConstants.ACTION.WIFI_CONNECTED);
        } else {
            Log.d("WiFiConnBR", "WiFi is unusable!");
            serviceIntent.setAction(GlobalConstants.ACTION.WIFI_UNUSABLE);
        }
        context.startService(serviceIntent);
    }

    public boolean isWiFiConnected(Context context){
        //WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager!=null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null &&
                        (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI
                                ||activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE)){
                    return activeNetworkInfo.isConnected();
                } else {
                    return false;
                }
            } else {
                NetworkInfo networkInfo;
                networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null){
                    return networkInfo.isConnected();
                } else {
                    networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                    return networkInfo != null && networkInfo.isConnected();
                }
            }
        }
        return false;
    }
}
