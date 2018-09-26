package com.urbit_iot.onekey.util.loggly;

import android.util.Log;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.github.tony19.loggly.LogglyClient;
import com.google.gson.Gson;
import com.urbit_iot.onekey.util.GlobalConstants;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by andresteve07 on 5/27/18.
 */

public class SteveLogglyTimberTree extends Timber.HollowTree implements Timber.TaggedTree{

    private static final int MAX_CACHE_SIZE = 200;
    private final LogglyClient logglyClient;
    private ArrayList<String> unsentLogs;
    private Lock unsentLogsMutex;
    private RxSharedPreferences rxSharedPreferences;
    private Preference<String> serializedUnsentLogs;
    private Gson gsonInstance;

    /** Log severity level */
    private enum SteveLogglyLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }


    public SteveLogglyTimberTree(String logglyToken, RxSharedPreferences rxSharedPreferences, Gson gsonInstance){
        logglyClient = new LogglyClient(logglyToken);
        this.unsentLogsMutex = new ReentrantLock();
        this.rxSharedPreferences = rxSharedPreferences;
        this.gsonInstance = gsonInstance;
        this.serializedUnsentLogs =
                this.rxSharedPreferences.getString(GlobalConstants.SP_KEY__UNSENT_LOGS);
        String serializedString = serializedUnsentLogs.get();
        Log.d("STEVE_LOGGLY", "STORED LOGS: " + serializedString);
        if (serializedUnsentLogs.isSet() && !serializedString.isEmpty()){
            String[] unsentLogsArray = this.gsonInstance.fromJson(serializedString,String[].class);
            this.unsentLogs = new ArrayList<>(Arrays.asList(unsentLogsArray));
        } else {
            this.unsentLogs = new ArrayList<>();
        }
        Observable.interval(30, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .doOnNext(n -> {
                    unsentLogsMutex.lock();
                    if (!unsentLogs.isEmpty()){
                        if (logglyClient.logBulk(new ArrayList<>(unsentLogs))){
                            unsentLogs.clear();
                            updateUnsentLogsPreference(unsentLogs);
                        }
                    }
                    unsentLogsMutex.unlock();
                })
                .subscribe();
    }

    private void updateUnsentLogsPreference(List<String> unsentLogs){
        if (!this.serializedUnsentLogs.isSet()){
            this.serializedUnsentLogs =
                    this.rxSharedPreferences.getString(GlobalConstants.SP_KEY__UNSENT_LOGS);
        }
        String serializedUnsentLogsString = this.gsonInstance.toJson(unsentLogs);
        Log.d("STEVE_LOGGLY", "STORED LOGS: " + serializedUnsentLogsString);
        this.serializedUnsentLogs.set(serializedUnsentLogsString);
    }

    /**
     * Logs a message with {@code DEBUG} severity
     * @param message message to be logged
     * @param args message formatting arguments
     */
    @Override
    public void d(String message, Object... args) {
        log(SteveLogglyLevel.DEBUG, message, args);
    }

    /**
     * Logs a message and an associated throwable with {@code DEBUG} severity
     * @param t throwable to be logged
     * @param message message to be logged
     * @param args message formatting arguments
     */
    @Override
    public void d(Throwable t, String message, Object... args) {
        log(SteveLogglyLevel.DEBUG, message, t, args);
    }

    /**
     * Logs a message with {@code INFO} severity
     * @param message message to be logged
     * @param args message formatting arguments
     */
    @Override
    public void i(String message, Object... args) {
        log(SteveLogglyLevel.INFO, message, args);
    }

    /**
     * Logs a message and an associated throwable with {@code INFO} severity
     * @param t throwable to be logged
     * @param message message to be logged
     * @param args message formatting arguments
     */
    @Override
    public void i(Throwable t, String message, Object... args) {
        log(SteveLogglyLevel.INFO, message, t, args);
    }

    /**
     * Logs a message with {@code ERROR} severity
     * @param message message to be logged
     * @param args message formatting arguments
     */
    @Override
    public void e(String message, Object... args) {
        log(SteveLogglyLevel.ERROR, message, args);
    }

    /**
     * Logs a message and an associated throwable with {@code ERROR} severity
     * @param t throwable to be logged
     * @param message message to be logged
     * @param args message formatting arguments
     */
    @Override
    public void e(Throwable t, String message, Object... args) {
        log(SteveLogglyLevel.ERROR, message, t, args);
    }

    /**
     * Logs a message with {@code WARN} severity
     * @param message message to be logged
     * @param args message formatting arguments
     */
    @Override
    public void w(String message, Object... args) {
        log(SteveLogglyLevel.WARN, message, args);
    }

    /**
     * Logs a message and an associated throwable with {@code WARN} severity
     * @param t throwable to be logged
     * @param message message to be logged
     * @param args message formatting arguments
     */
    @Override
    public void w(Throwable t, String message, Object... args) {
        log(SteveLogglyLevel.WARN, message, t, args);
    }

    public class LogStructure {
        private SteveLogglyLevel level;
        private String message;
        private String deviceTimestamp;
        private LoggerTrace loggerTrace;


    }
    /**
     * Gets the JSON representation of a log event
     * @param steveLogglyLevel log severity steveLogglyLevel
     * @param message message to be logged
     * @param args message formatting arguments
     * @return JSON string
     */
    private String toJson(SteveLogglyLevel steveLogglyLevel, String message, Object... args) {
        return String.format("{\"level\": \"%1$s\", \"message\": \"%2$s\", \"device_timestamp\": \"%3$s\", \"logger_trace\": %4$s}",
                steveLogglyLevel,
                String.format(message, args).replace("\"", "\\\""),
                Calendar.getInstance().getTime().toString(),
                this.getLoggerTraceJson());
    }

    /**
     * Converts a {@code Throwable} into a string
     * http://stackoverflow.com/a/4812589/600838
     * @param t throwable to convert
     * @return string representation of the throwable
     */
    private String formatThrowable(Throwable t) {
        StringWriter errors = new StringWriter();
        t.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    /**
     * Gets the JSON representation of a log event
     * @param steveLogglyLevel log severity steveLogglyLevel
     * @param message message to be logged
     * @param args message formatting arguments
     * @return JSON string
     */
    private String toJson(SteveLogglyLevel steveLogglyLevel, String message, Throwable t, Object... args) {
        //TODO gson library could be handy.
        //TODO Write a LogModel for easier update and serialize
        return String.format("{\"level\": \"%1$s\", \"message\": \"%2$s\", \"exception\": \"%3$s\", \"device_timestamp\": \"%4$s\", \"logger_trace\": %5$s}",
                steveLogglyLevel,
                String.format(message, args).replace("\"", "\\\""),
                formatThrowable(t),
                Calendar.getInstance().getTime().toString(),
                this.getLoggerTraceJson());
    }
    private LoggerTrace getLoggerTrace(){
        String className = "";
        LoggerTrace loggerTrace = null;
        //StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
        //ArrayList<StackTraceElement> stackTraceElements = new ArrayList<>(Arrays.asList(new Throwable().getStackTrace()));
        //Log.d("SteveTimber", Arrays.toString(stackTraceElements.toArray()));
        for (StackTraceElement element: stackTraceElements){
            if(element.getClassName().contains("urbit") && !element.getClassName().contains("Timber")){
                className = element.getClassName();
                loggerTrace = new LoggerTrace(className.substring(className.lastIndexOf('.') + 1),
                        element.getMethodName(),
                        element.getFileName(),
                        element.getLineNumber());
                break;
            }
        }
        return loggerTrace;
    }

    private String getLoggerTraceJson(){
        LoggerTrace loggerTrace = getLoggerTrace();
        if (loggerTrace != null){
            return String.format("{\"class_name\": \"%1$s\", \"method_name\": \"%2$s\", \"file_name\": \"%3$s\", \"line_number\": %4$d}",
                    loggerTrace.getClassName(),
                    loggerTrace.getMethodName(),
                    loggerTrace.getFileName(),
                    loggerTrace.getLineNumber());
        } else {
            return "";
        }
    }

    private static class LoggerTrace{
        private String className;
        private String methodName;
        private String fileName;
        private int lineNumber;

        public LoggerTrace(String className, String methodName, String fileName, int lineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }
    }

    /**
     * Asynchronously sends a log event to Loggly
     * @param steveLogglyLevel log severity steveLogglyLevel
     * @param message message to be logged
     * @param t throwable
     * @param args message formatting arguments
     */
    private void log(SteveLogglyLevel steveLogglyLevel, String message, Throwable t, Object... args) {
        //logglyClient.log(toJson(steveLogglyLevel, message, t, args), handler);
        this.sendLogToServer(toJson(steveLogglyLevel, message, t, args));
    }

    /**
     * Asynchronously sends a log event to Loggly
     * @param steveLogglyLevel log severity steveLogglyLevel
     * @param message message to be logged
     * @param args message formatting arguments
     */
    private void log(SteveLogglyLevel steveLogglyLevel, String message, Object... args) {
        //logglyClient.log(toJson(steveLogglyLevel, message, args), handler);
        this.sendLogToServer(toJson(steveLogglyLevel, message, args));
    }

    private void sendLogToServer(String log){
        Observable.just(true)
                .doOnNext(aBoolean -> {
                    if(logglyClient.log(log)){
                        unsentLogsMutex.lock();
                        if (!unsentLogs.isEmpty()){
                            if(logglyClient.logBulk(new ArrayList<>(unsentLogs))){
                                unsentLogs.clear();
                                updateUnsentLogsPreference(unsentLogs);
                            }
                        }
                        unsentLogsMutex.unlock();
                    } else {
                        unsentLogsMutex.lock();
                        if(unsentLogs.size() < MAX_CACHE_SIZE){
                            unsentLogs.add(log);
                        } else {
                            unsentLogs.remove(0);
                            unsentLogs.add(log);
                        }
                        updateUnsentLogsPreference(unsentLogs);
                        unsentLogsMutex.unlock();
                        //TODO serialize unsents list to file or shared preferences!!
                    }
                })
                .subscribeOn(Schedulers.io())//TODO replace with dagger scheduler instance.
                .subscribe();
    }

    /**
     * Sets the Loggly tag for all logs going forward. This differs from
     * the API of {@code Timber.TaggedTree} in that it's not a one-shot
     * tag.
     * @param tag desired tag or CSV of multiple tags; use empty string
     *            to clear tags
     */
    @Override
    public void tag(String tag) {
        logglyClient.setTags(tag);
    }
}
