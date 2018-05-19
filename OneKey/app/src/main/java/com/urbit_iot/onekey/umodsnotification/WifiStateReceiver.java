package com.urbit_iot.onekey.umodsnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.urbit_iot.onekey.util.GlobalConstants;

/**
 * Created by andresteve07 on 18/05/18.
 */

public class WifiStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())){
            Intent serviceIntent = new Intent(context, UModsNotifService.class);
            Log.d("WiFiBR", "WiFi event!");
            if (WifiManager.WIFI_STATE_ENABLED == wifiState){
                Log.d("WiFiBR", "WiFi is now enabled!");
                serviceIntent.setAction(GlobalConstants.ACTION.WIFI_CONNECTED);
            } else {
                Log.d("WiFiBR", "WiFi is unusable!");
                serviceIntent.setAction(GlobalConstants.ACTION.WIFI_UNUSABLE);
            }
            context.startService(serviceIntent);
        }
    }
}
