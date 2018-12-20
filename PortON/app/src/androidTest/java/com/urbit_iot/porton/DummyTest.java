package com.urbit_iot.porton;


import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.AndroidJUnitRunner;
import android.test.suitebuilder.annotation.LargeTest;

import com.urbit_iot.porton.umods.UModsActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DummyTest {

    @Rule
    public ActivityTestRule<UModsActivity> mTasksActivityTestRule =
            new ActivityTestRule<UModsActivity>(UModsActivity.class){
                @Override
                protected Intent getActivityIntent() {
                    Context targetContext = InstrumentationRegistry
                            .getInstrumentation()
                            .getTargetContext();
                    Intent result = new Intent(targetContext, UModsActivity.class);
                    result.putExtra("APP_USER_NAME", "5490387154623893");
                    result.putExtra("APP_UUID", "2f6830a0-55d1-461f-b21a-92863089de80");
                    return result;
                }
            };

    @Test
    public void dummyTest(){

    }

}