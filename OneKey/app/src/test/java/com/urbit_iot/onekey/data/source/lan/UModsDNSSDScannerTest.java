package com.urbit_iot.onekey.data.source.lan;

import android.support.annotation.NonNull;

import com.github.druk.rxdnssd.BonjourService;
import com.github.druk.rxdnssd.RxDnssd;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.observers.AssertableSubscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.ReplaySubject;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UModsDNSSDScannerTest {

    private UModsDNSSDScanner dnssdScanner;

    @Mock
    private RxDnssd rxDnssdMock;

    @Mock
    private BaseSchedulerProvider schedulerProviderMock;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Before
    public void setUp() throws Exception {
        when(schedulerProviderMock.io()).thenReturn(Schedulers.io());
        when(schedulerProviderMock.computation()).thenReturn(Schedulers.computation());
        when(schedulerProviderMock.ui()).thenReturn(Schedulers.immediate());
        this.dnssdScanner = new UModsDNSSDScanner(rxDnssdMock,schedulerProviderMock);
    }

    @After
    public void tearDown() throws Exception {
        reset(rxDnssdMock,schedulerProviderMock);
        this.dnssdScanner = null;
    }

    @Test
    public void Given_NoScanningInProgress_When_browseLANForUMods_Then_SingleScanIsCalledAndTheScanInProgressFlagIsSet() {
        //Given
        UModsDNSSDScanner dnssdScannerSpy = spy(this.dnssdScanner);
        doReturn(Observable.empty()).when(dnssdScannerSpy).singleScan();
        //When
        AssertableSubscriber<UMod> testSubscriber = dnssdScannerSpy.browseLANForUMods().test();
        //Then
        testSubscriber.assertCompleted();
        testSubscriber.assertUnsubscribed();
        verify(dnssdScannerSpy,times(1)).singleScan();
        assertEquals(1,dnssdScannerSpy.scanMutex.availablePermits());
        assertTrue(dnssdScannerSpy.scanInProgress);
    }

    @Test
    public void Given_AScanningInProgress_When_browseLANForUMods_Then_scanSubjectIsSubscribed() {
        //Given
        UModsDNSSDScanner dnssdScannerSpy = spy(this.dnssdScanner);
        //doReturn(Observable.empty()).when(dnssdScannerSpy).singleScan();
        dnssdScannerSpy.scanInProgress = true;
        TestScheduler testScheduler = new TestScheduler();
        when(schedulerProviderMock.computation()).thenReturn(testScheduler);
        //When
        AssertableSubscriber<UMod> testSubscriber = dnssdScannerSpy.browseLANForUMods().test();
        //Then
        testScheduler.advanceTimeBy(5000L,TimeUnit.MILLISECONDS);
        testSubscriber.assertCompleted();
        testSubscriber.assertUnsubscribed();
        assertEquals(1,dnssdScannerSpy.scanMutex.availablePermits());
        assertTrue(dnssdScannerSpy.scanInProgress);
    }

    @Test
    public void singleScan_Then_scanInProgressIsCleared() {
        UModsDNSSDScanner dnssdScannerSpy = spy(this.dnssdScanner);
        //BonjourService bonjourService = mock(BonjourService.class);
        when(rxDnssdMock.browse("_http._tcp.","local.")).thenReturn(Observable.empty());
        Observable.Transformer<BonjourService,BonjourService> rxDnsSdTransformer = bonjourServiceObservable -> bonjourServiceObservable;
        when(rxDnssdMock.resolve()).thenReturn(rxDnsSdTransformer);
        when(rxDnssdMock.queryIPRecords()).thenReturn(rxDnsSdTransformer);
        TestScheduler testScheduler = new TestScheduler();
        when(schedulerProviderMock.computation()).thenReturn(testScheduler);
        //doNothing().when(dnssdScannerSpy).tryToConnect(any(UMod.class));

        AssertableSubscriber<UMod> testSubscriber = dnssdScannerSpy.singleScan().test();

        testScheduler.advanceTimeBy(5000L,TimeUnit.MILLISECONDS);

        testSubscriber.assertCompleted();
        testSubscriber.assertUnsubscribed();
        verify(rxDnssdMock,times(1)).browse("_http._tcp.","local.");
        assertEquals(1,dnssdScannerSpy.scanMutex.availablePermits());
        assertFalse(dnssdScannerSpy.scanInProgress);

    }

    @Test
    public void Given_OneUModWasFound_When_singleScan_Then_scanEmitsResults() throws UnknownHostException {
        UModsDNSSDScanner dnssdScannerSpy = spy(this.dnssdScanner);
        //BonjourService bonjourService = mock(BonjourService.class);
        BonjourService bonjourService =
                new BonjourService.Builder(123, 123, "urbit-AAAEEE","_http._tcp.", "local.")
                        .hostname("urbit-AAAEEE")
                        .inet4Address((Inet4Address) Inet4Address.getByName("192.168.8.9"))
                        .build();
        when(rxDnssdMock.browse("_http._tcp.","local."))
                .thenReturn(Observable.concat(Observable.just(bonjourService),Observable.never()));
        Observable.Transformer<BonjourService,BonjourService> rxDnsSdTransformer = bonjourServiceObservable -> bonjourServiceObservable;
        when(rxDnssdMock.resolve()).thenReturn(rxDnsSdTransformer);
        when(rxDnssdMock.queryIPRecords()).thenReturn(rxDnsSdTransformer);
        TestScheduler testScheduler = new TestScheduler();
        when(schedulerProviderMock.computation()).thenReturn(testScheduler);
        when(schedulerProviderMock.io()).thenReturn(testScheduler);
        doNothing().when(dnssdScannerSpy).tryToConnect(any(UMod.class));
        doReturn("AAAEEE").when(dnssdScannerSpy).getUUIDFromDiscoveryServiceName(any());

        AssertableSubscriber<UMod> testSubscriber = dnssdScannerSpy.singleScan().test();

        testScheduler.advanceTimeBy(5000L,TimeUnit.MILLISECONDS);
        //testSubscriber.awaitTerminalEvent();//Necessary because there is an ObserveOn on the tested chain
        testSubscriber.assertValueCount(1);
        testSubscriber.assertCompleted();
        testSubscriber.assertUnsubscribed();
        verify(rxDnssdMock,times(1)).browse("_http._tcp.","local.");
        assertEquals(1,dnssdScannerSpy.scanMutex.availablePermits());
        assertFalse(dnssdScannerSpy.scanInProgress);

    }

    @Test
    public void Given_OneUnrelatedServiceWasFound_When_singleScan_Then_noResultsAreEmitted() throws UnknownHostException {
        UModsDNSSDScanner dnssdScannerSpy = spy(this.dnssdScanner);
        //BonjourService bonjourService = mock(BonjourService.class);
        BonjourService bonjourService =
                new BonjourService.Builder(123, 123, "unrelated_service","_http._tcp.", "local.")
                        .hostname("unrelated_service")
                        .inet4Address((Inet4Address) Inet4Address.getByName("192.168.8.9"))
                        .build();
        when(rxDnssdMock.browse("_http._tcp.","local."))
                .thenReturn(Observable.concat(Observable.just(bonjourService),Observable.never()));
        Observable.Transformer<BonjourService,BonjourService> rxDnsSdTransformer = bonjourServiceObservable -> bonjourServiceObservable;
        when(rxDnssdMock.resolve()).thenReturn(rxDnsSdTransformer);
        when(rxDnssdMock.queryIPRecords()).thenReturn(rxDnsSdTransformer);
        TestScheduler testScheduler = new TestScheduler();
        when(schedulerProviderMock.computation()).thenReturn(testScheduler);
        when(schedulerProviderMock.io()).thenReturn(testScheduler);

        AssertableSubscriber<UMod> testSubscriber = dnssdScannerSpy.singleScan().test();

        testScheduler.advanceTimeBy(5000L,TimeUnit.MILLISECONDS);
        //testSubscriber.awaitTerminalEvent();//Necessary because there is an ObserveOn on the tested chain
        testSubscriber.assertNoValues();
        testSubscriber.assertCompleted();
        testSubscriber.assertUnsubscribed();
        verify(rxDnssdMock,times(1)).browse("_http._tcp.","local.");
        assertEquals(1,dnssdScannerSpy.scanMutex.availablePermits());
        assertFalse(dnssdScannerSpy.scanInProgress);
    }

    @Test
    public void Given_OneUModWasFoundButRetrievedWithoutIPAddress_When_singleScan_Then_noResultsAreEmitted() {
        UModsDNSSDScanner dnssdScannerSpy = spy(this.dnssdScanner);
        //BonjourService bonjourService = mock(BonjourService.class);
        BonjourService bonjourService =
                new BonjourService.Builder(123, 123, "urbit-AAAEEE","_http._tcp.", "local.")
                        .hostname("urbit-AAAEEE")
                        .build();
        when(rxDnssdMock.browse("_http._tcp.","local."))
                .thenReturn(Observable.concat(Observable.just(bonjourService),Observable.never()));
        Observable.Transformer<BonjourService,BonjourService> rxDnsSdTransformer = bonjourServiceObservable -> bonjourServiceObservable;
        when(rxDnssdMock.resolve()).thenReturn(rxDnsSdTransformer);
        when(rxDnssdMock.queryIPRecords()).thenReturn(rxDnsSdTransformer);
        TestScheduler testScheduler = new TestScheduler();
        when(schedulerProviderMock.computation()).thenReturn(testScheduler);
        when(schedulerProviderMock.io()).thenReturn(testScheduler);

        AssertableSubscriber<UMod> testSubscriber = dnssdScannerSpy.singleScan().test();

        testScheduler.advanceTimeBy(5000L,TimeUnit.MILLISECONDS);
        //testSubscriber.awaitTerminalEvent();//Necessary because there is an ObserveOn on the tested chain
        testSubscriber.assertNoValues();
        testSubscriber.assertCompleted();
        testSubscriber.assertUnsubscribed();
        verify(rxDnssdMock,times(1)).browse("_http._tcp.","local.");
        assertEquals(1,dnssdScannerSpy.scanMutex.availablePermits());
        assertFalse(dnssdScannerSpy.scanInProgress);
    }

    @Test
    public void Given_OneNullServiceFound_When_singleScan_Then_NoResultsAreEmitted() {
        UModsDNSSDScanner dnssdScannerSpy = spy(this.dnssdScanner);
        //BonjourService bonjourService = mock(BonjourService.class);
        BonjourService bonjourService = null;
        when(rxDnssdMock.browse("_http._tcp.","local."))
                .thenReturn(Observable.concat(Observable.just(bonjourService),Observable.never()));
        Observable.Transformer<BonjourService,BonjourService> rxDnsSdTransformer = bonjourServiceObservable -> bonjourServiceObservable;
        when(rxDnssdMock.resolve()).thenReturn(rxDnsSdTransformer);
        when(rxDnssdMock.queryIPRecords()).thenReturn(rxDnsSdTransformer);
        TestScheduler testScheduler = new TestScheduler();
        when(schedulerProviderMock.computation()).thenReturn(testScheduler);
        when(schedulerProviderMock.io()).thenReturn(testScheduler);

        AssertableSubscriber<UMod> testSubscriber = dnssdScannerSpy.singleScan().test();

        testScheduler.advanceTimeBy(5000L,TimeUnit.MILLISECONDS);
        //testSubscriber.awaitTerminalEvent();//Necessary because there is an ObserveOn on the tested chain
        testSubscriber.assertNoValues();
        testSubscriber.assertCompleted();
        testSubscriber.assertUnsubscribed();
        verify(rxDnssdMock,times(1)).browse("_http._tcp.","local.");
        assertEquals(1,dnssdScannerSpy.scanMutex.availablePermits());
        assertFalse(dnssdScannerSpy.scanInProgress);

    }

    @Test
    public void tryToConnect() {
    }

    @Test
    public void testTcpConnection() {
    }
}