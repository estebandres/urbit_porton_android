package com.urbit_iot.onekey.util.loggly;

import com.github.tony19.loggly.LogglyClient;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

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

    /** Log severity level */
    private enum SteveLogglyLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    public SteveLogglyTimberTree(String logglyToken){
        logglyClient = new LogglyClient(logglyToken);
        this.unsentLogs = new ArrayList<>();

        Observable.interval(2, TimeUnit.HOURS)
                .subscribeOn(Schedulers.io())
                .doOnNext(n -> {
                    if (!unsentLogs.isEmpty()){
                        if (logglyClient.logBulk(new ArrayList<>(unsentLogs))){
                            unsentLogs.clear();
                        }
                    }
                })
                .subscribe();
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

    /**
     * Gets the JSON representation of a log event
     * @param steveLogglyLevel log severity steveLogglyLevel
     * @param message message to be logged
     * @param args message formatting arguments
     * @return JSON string
     */
    private String toJson(SteveLogglyLevel steveLogglyLevel, String message, Object... args) {
        return String.format("{\"level\": \"%1$s\", \"message\": \"%2$s\", \"device_timestamp\": \"%3$s\"}",
                steveLogglyLevel,
                String.format(message, args).replace("\"", "\\\""),
                Calendar.getInstance().getTime().toString());
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
        return String.format("{\"level\": \"%1$s\", \"message\": \"%2$s\", \"exception\": \"%3$s\", \"device_timestamp\": \"%4$s\"}",
                steveLogglyLevel,
                String.format(message, args).replace("\"", "\\\""),
                formatThrowable(t),
                Calendar.getInstance().getTime().toString());
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
                        if (!unsentLogs.isEmpty()){
                            if(logglyClient.logBulk(new ArrayList<>(unsentLogs))){
                                unsentLogs.clear();
                            }
                        }
                    } else {
                        if(unsentLogs.size() < MAX_CACHE_SIZE){
                            unsentLogs.add(log);
                        } else {
                            unsentLogs.remove(0);
                            unsentLogs.add(log);
                        }
                        //TODO serialize unsents list to file or shared preferences!!
                    }
                })
                .subscribeOn(Schedulers.io())//TODO replace for dagger scheduler instance.
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
