package com.urbit_iot.porton.umods.domain.usecase;

import android.support.annotation.NonNull;

import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.SchedulerProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.concurrent.Executor;

import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.internal.schedulers.ExecutorScheduler;
import rx.plugins.RxJavaPlugins;
import rx.schedulers.Schedulers;

import static org.junit.Assert.*;

public class SetOngoingNotificationStatusTest {

    private SetOngoingNotificationStatus useCase;

    @Mock
    private UModsRepository uModsRepositoryMock;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Spy
    private SchedulerProvider schedulerProvider;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(Schedulers.immediate()).when(schedulerProvider).ui();
        this.useCase = new SetOngoingNotificationStatus(uModsRepositoryMock, schedulerProvider);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void When_SetOngoingNotificationStatus_Then_RepositorySetUModNotificationStatusIsCalled(){
        this.useCase.complete(new SetOngoingNotificationStatus.RequestValues("777",true));
        Mockito.verify(this.uModsRepositoryMock,Mockito.times(1)).setUModNotificationStatus("777",true);
        Mockito.verifyNoMoreInteractions(this.uModsRepositoryMock);
    }
}