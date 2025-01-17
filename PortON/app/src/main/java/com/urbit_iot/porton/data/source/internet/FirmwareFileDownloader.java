package com.urbit_iot.porton.data.source.internet;

import android.content.Context;
import android.util.Log;

import com.urbit_iot.porton.util.GlobalConstants;

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
        return Observable.defer(() -> {
            /*
            String downloadUrl = "http://"
                    + GlobalConstants.FIRMWARE_SERVER__IP_ADDRESS
                    + ":"
                    + GlobalConstants.FIRMWARE_SERVER__PORT
                    + "/firmware_update";
             */
            String downloadUrl = GlobalConstants.FIRMWARE_SERVER__URL;
            File firmwareFile = new File(this.appContext.getFilesDir(), "firmware_update.zip");
            try{
                FileUtils.copyURLToFile(new URL(downloadUrl),firmwareFile,8000,20000);
            } catch(MalformedURLException mExc){
                mExc.printStackTrace();
            } catch (IOException ioExc){
                ioExc.printStackTrace();
            }
            //TODO add real size validation and Hash validation
            Log.d("file_downloader","Downloaded File: " + firmwareFile.length());
            if (firmwareFile.length() > 0){
                return Observable.just(firmwareFile);
            } else {
                return Observable.error(new Exception("file_downloader Empty file was retrieve."));
            }
        });
    }
}