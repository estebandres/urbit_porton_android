package com.urbit_iot.porton.umods;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.urbit_iot.porton.ApplicationModule;
import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.data.source.TestingUModsRepositoryComponent;
import com.urbit_iot.porton.data.source.TestingUModsRepositoryModule;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.data.source.UModsRepositoryComponent;
import com.urbit_iot.porton.util.schedulers.SchedulerProviderComponent;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Component;
import rx.Observable;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UModsActivityTest {
    @Inject
    UModsRepository uModsRepositoryMock;

    @Rule
    public ActivityTestRule<UModsActivity> mTasksActivityTestRule =
            new ActivityTestRule<UModsActivity>(UModsActivity.class,true,false){
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

    @Before
    public void setUp() throws Exception {
        if (uModsRepositoryMock == null){
            Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
            PortONApplication app = (PortONApplication) instrumentation.getTargetContext().getApplicationContext();
            TestingUModsRepositoryComponent testingComponent =
                    (TestingUModsRepositoryComponent) app.createUModsRepositoryComponentSingleton(null,null);
            testingComponent.inject(this);
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void dummyTest(){
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.empty());
        mTasksActivityTestRule.launchActivity(null);

    }
}