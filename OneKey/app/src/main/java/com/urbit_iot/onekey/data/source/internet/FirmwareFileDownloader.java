package com.urbit_iot.onekey.data.source.internet;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import rx.Observable;

/**
 * Created by steve-urbit on 07/02/18.
 */

public class FirmwareFileDownloader {

    private Context appContext;

    public FirmwareFileDownloader(Context context){
        this.appContext = context;
    }

    public Observable<File> downloadFirmwareFile(){
        String downloadUrl = "http://172.18.188.81:3000/firmware_update";

        File firmwareFile = new File(this.appContext.getFilesDir(), "firmware_update.zip");
        try{
            FileUtils.copyURLToFile(new URL(downloadUrl),firmwareFile,2000,2000);
        } catch(MalformedURLException mExc){
            mExc.printStackTrace();
        } catch (IOException ioExc){
            ioExc.printStackTrace();
        }
        Log.d("file_downloader","Downloaded File: " + firmwareFile.length());
        if (firmwareFile.length() > 0){
            return Observable.just(firmwareFile);
        } else {
            return Observable.error(new Exception("file_downloader Empty file was retrieve."));
        }
    }
}