package com.urbit_iot.porton.util.schedulers;

import androidx.annotation.NonNull;

import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Provides different types of schedulers.
 */
public class SingleSchedulersProvider implements BaseSchedulerProvider {
    private Scheduler computationScheduler;
    private Scheduler ioScheduler;
    private Scheduler mainScheduler;

    public SingleSchedulersProvider() {
        this.computationScheduler = Schedulers.computation();
        this.ioScheduler = Schedulers.io();
        this.mainScheduler = AndroidSchedulers.mainThread();
    }

    @Override
    @NonNull
    public Scheduler computation() {
        return this.computationScheduler;
    }

    @Override
    @NonNull
    public Scheduler io() {
        return this.ioScheduler;
    }

    @Override
    @NonNull
    public Scheduler ui() {
        return this.mainScheduler;
    }
}
