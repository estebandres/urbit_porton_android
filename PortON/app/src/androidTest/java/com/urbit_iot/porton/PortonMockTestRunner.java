package com.urbit_iot.porton;

import android.app.Application;
import android.content.Context;

import com.urbit_iot.porton.umods.TestingPortonApplication;

import androidx.test.runner.AndroidJUnitRunner;

public class PortonMockTestRunner extends AndroidJUnitRunner {

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newApplication(cl, TestingPortonApplication.class.getName(), context);
    }

}
