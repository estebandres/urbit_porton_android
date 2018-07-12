package com.urbit_iot.onekey.data.source.lan;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by andresteve07 on 05/07/18.
 */

public class PhoneIPAddressRadius implements LanAddressesCalculation {
    private Long phoneIPAddress;
    private Long networkAddress;
    private Long subnetMaskAddress;

    public PhoneIPAddressRadius(Long phoneIPAddress, Long subnetMaskAddress) {
        this.phoneIPAddress = phoneIPAddress;
        this.subnetMaskAddress = subnetMaskAddress;
        this.networkAddress = phoneIPAddress & subnetMaskAddress;
    }

    @Override
    public List<Long> calculateAddresses() {
        if (this.networkAddress == 0L || this.phoneIPAddress == 0){
            return new ArrayList<>();
        }
        ArrayList<Long> lowerAddresses = new ArrayList<>();
        ArrayList<Long> upperAddresses = new ArrayList<>();
        long maxIPAddress = networkAddress + ((~this.subnetMaskAddress) & 0xFFFFFFFFL) - 1L;
        Log.d("MAX IP", "" + maxIPAddress + "   " + longToStringIP(maxIPAddress));

        //Log.d("UPPER_LIMIT", "" + (maxIPAddress - phoneIPAddress));
        for(int distance = 1; distance <= (maxIPAddress - this.phoneIPAddress) ;distance++ ){
            upperAddresses.add(this.phoneIPAddress + distance);
        }

        Log.d("LOWER_LIMIT", "" + (this.phoneIPAddress - this.networkAddress));
        for (int distance = 1; distance < (this.phoneIPAddress - this.networkAddress) ;distance++){
            lowerAddresses.add(this.phoneIPAddress - distance);
        }

        Log.d("LOWER_SIZE", "" + lowerAddresses.size());
        Log.d("UPPER_SIZE",  "" + upperAddresses.size());

        Iterator<Long> lowerIterator = lowerAddresses.iterator();
        Iterator<Long> upperIterator = upperAddresses.iterator();

        ArrayList<Long> orderShuffledAddresses = new ArrayList<>();

        while (lowerIterator.hasNext() || upperIterator.hasNext()){
            if (lowerIterator.hasNext()){
                orderShuffledAddresses.add(lowerIterator.next());
            }
            if (upperIterator.hasNext()){
                orderShuffledAddresses.add(upperIterator.next());
            }
        }
        return orderShuffledAddresses;
    }

    private static String longToStringIP(long ip) {
        return  ((ip >> 24) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >>  8) & 0xFF) + "." +
                ( ip        & 0xFF);
    }
}
