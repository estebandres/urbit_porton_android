package com.urbit_iot.porton.umodsnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.urbit_iot.porton.util.GlobalConstants;

import java.util.Date;

/**
 * Created by andresteve07 on 30/05/18.
 */

public class ConnectivityReceiver extends BroadcastReceiver {
    private Integer lastEventNetworkType;
    private Date lastConnectionEventDate;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, UModsNotifService.class);
        /*
        boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager
                .EXTRA_NO_CONNECTIVITY, false);
        int nettype = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE,666);
        NetworkInfo info2 = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager
                .EXTRA_OTHER_NETWORK_INFO);
        String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
        boolean failOver = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
        Log.d("MY_TAG", "onReceive(): mNetworkType=" + nettype + " mOtherNetworkInfo = " +
                (info2 == null ? "[none]" : info2 + " noConn=" + noConnectivity)
                + "  REASON: " + reason + "FAILOVER:  " + failOver);
        Log.d("WiFiConnBR", "WiFi connection event!");
        */
        Date justNowDate = new Date();
        Integer currentEventNetworkType = getConnectedNetworkType(context);
        if (currentEventNetworkType != null){
            if (lastEventNetworkType != null
                    && lastConnectionEventDate != null
                    //TODO would it be better to ask currentEventNetworkType == lastEventNetworkType?
                    && currentEventNetworkType == 0//current network event is MOBILE
                    && lastEventNetworkType == 0//last network event was MOBILE
                    && millisecondsBetweenDates(justNowDate, lastConnectionEventDate) < 4000L){//and occurred less than 4 secs before
                Log.d("CONNECTION_RCVR", "Connection event is ignored.");
                lastConnectionEventDate = justNowDate;
                return;
            }
            lastEventNetworkType = currentEventNetworkType;
            lastConnectionEventDate = justNowDate;
            Log.d("CONNECTION_RCVR", "Connection is active!");
            serviceIntent.setAction(GlobalConstants.ACTION.WIFI_CONNECTED);
        } else {
            Log.d("CONNECTION_RCVR", "There is no connection.");
            serviceIntent.setAction(GlobalConstants.ACTION.WIFI_UNUSABLE);
        }
        context.startService(serviceIntent);
    }

    public boolean isNetworkConnected(Context context) {
        //WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
        //TODO add ping check against reliable servers/services (e.g. google/amazon/...)
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null
                && (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI
                    || activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                && activeNetworkInfo.isConnected();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo;
            networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo != null){
                Log.d("CONN_RECV", networkInfo.toString());
                Log.d("CONN_RECV", networkInfo.getExtraInfo()
                        + "  " + networkInfo.getReason() + " " + networkInfo.getState().name());
            }
            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            } else {
                networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if (networkInfo != null){
                    Log.d("CONN_RECV", networkInfo.toString());
                    Log.d("CONN_RECV", networkInfo.getExtraInfo()
                            + "  " + networkInfo.getReason() + " " + networkInfo.getState().name());
                }
                if (networkInfo != null && networkInfo.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    private Integer getConnectedNetworkType(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo;
            networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo != null && networkInfo.isConnected()) {
                return 1;//WIFI
            } else {
                networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if (networkInfo != null && networkInfo.isConnected()) {
                    return 0;//MOBILE
                }
            }
        }
        return null;
    }

    private long millisecondsBetweenDates(Date newer, Date older){
        return newer.getTime() - older.getTime();
    }
}
