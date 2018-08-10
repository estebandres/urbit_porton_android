package com.urbit_iot.onekey.data.source.gps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.LocationRequest;


import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Maybe;
import rx.Observable;
import io.reactivex.schedulers.Schedulers;
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider;

/**
 * Created by andresteve07 on 17/07/18.
 */

public class LocationService {
    private ReactiveLocationProvider locationProvider;
    private Context mContext;

    @Inject
    public LocationService(Context context) {
        this.mContext = context;
        this.locationProvider = new ReactiveLocationProvider(context);
        /*
        getCurrentLocation()
                .subscribeOn(Schedulers.io())
                .subscribe(location -> {
            Log.d("GPS_SCAN", "Result: " + location.toString());
        },throwable -> {
            Log.e("GPS_SCAN", "Failure: " + throwable.getMessage(), throwable);
        },()->{
            Log.d("GPS_SCAN", "Scan Fnished");
        });
        */
        getCurrentLocation().subscribe(location -> {
            Log.d("GPS_SCAN", "Result: " + location.toString() + " TIME: " + new Date(location.getTime()).toString());
        }, throwable -> {
            Log.e("GPS_SCAN", "Failure: " + throwable.getMessage(), throwable);
        }, () -> {
            Log.d("GPS_SCAN", "Scan Fnished");
        });
    }

    public Observable<Location> getCurrentLocation() {
        LocationRequest request = LocationRequest.create() //standard GMS LocationRequest
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setInterval(100);

        if (ActivityCompat.checkSelfPermission(
                this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this.mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return Observable.empty();
        }
        //return RxJavaInterop.toV1Observable(s -> locationProvider.getUpdatedLocation(request).takeUntil(io.reactivex.Observable.timer(3L,TimeUnit.SECONDS)).firstElement());
        return RxJavaInterop.toV1Observable(locationProvider.getUpdatedLocation(request), BackpressureStrategy.BUFFER)
                .observeOn(rx.schedulers.Schedulers.io())
                .doOnNext(location -> Log.d("LOCATION_SERVICE","" + location.toString() + " TIME: " + new Date(location.getTime()).toString() ))
                .doOnError(throwable -> Log.e("LOCATION_SERVICE", "" + throwable.getMessage(),throwable))
                .takeUntil(Observable.timer(2000L, TimeUnit.MILLISECONDS));
    }

    public Observable<Location> getCurrentLocationA() {
        if (ActivityCompat.checkSelfPermission(
                this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this.mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return Observable.empty();
        }
        return RxJavaInterop.toV1Observable(locationProvider.getLastKnownLocation(), BackpressureStrategy.BUFFER)
                .doOnNext(location -> Log.d("LOCATION_SERVICE","" + location.toString() + " TIME: " + new Date(location.getTime()).toString() ))
                .takeUntil(Observable.timer(2000L, TimeUnit.MILLISECONDS));
    }

    public Observable<Address> getAddressFromLocation(Location location) {
        if (ActivityCompat.checkSelfPermission(
                this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                this.mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return Observable.empty();
        }

        return RxJavaInterop.toV1Observable(locationProvider
                .getReverseGeocodeObservable(
                        location.getLatitude(),
                        location.getLongitude(),
                        10),
                BackpressureStrategy.BUFFER)
                .subscribeOn(rx.schedulers.Schedulers.io())
                .flatMap(Observable::from)
                .first()
                .doOnNext(address -> Log.d("LOCATION_SERVICE", " ADDRESS: " + address.toString()))
                .takeUntil(Observable.timer(2000L, TimeUnit.MILLISECONDS));
    }
}
