package com.urbit_iot.porton.umods;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.StringRes;

import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.ncorti.slidetoact.SlideToActView;
import com.urbit_iot.porton.PortONApplication;
import com.urbit_iot.porton.R;
import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.APIUserType;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.TriggerRPC;
import com.urbit_iot.porton.data.source.TestingUModsRepositoryComponent;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.umodconfig.UModConfigActivity;
import com.urbit_iot.porton.umodconfig.UModConfigFragment;
import com.urbit_iot.porton.util.EspressoIdlingResource;
import com.urbit_iot.porton.util.GlobalConstants;
import com.urbit_iot.porton.util.dagger.Local;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;

import javax.inject.Inject;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.Intents.times;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UModsActivityTest {
    @Inject
    UModsRepository uModsRepositoryMock;
    @Inject
    RxSharedPreferences rxSharedPreferences;

    public static Context myTargetContext;

    @Rule
    public IntentsTestRule mIntentsTestRule = new IntentsTestRule<UModsActivity>(UModsActivity.class,true, false){
        @Override
        protected void beforeActivityLaunched() {
            Preference<Boolean> ongoingNotificationPref = rxSharedPreferences.getBoolean(GlobalConstants.ONGOING_NOTIFICATION_STATE_KEY);
            ongoingNotificationPref.set(false);
            Preference<String> userString =rxSharedPreferences.getString(GlobalConstants.SP_KEY__APPUSER);
            userString.set("{\"app_uuid\":\"0ea6ac0b-c1d5-4894-8439-db265fc12bf1\",\"user_credentials_hash\":\"2054c049d457e48f9a156beb207ad928\",\"phone_number\":\"+5490354415123456\",\"user_name\":\"5490354415123456\"}");
        }

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
        if (uModsRepositoryMock == null) {
            Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
            PortONApplication app = (PortONApplication) instrumentation.getTargetContext().getApplicationContext();
            TestingUModsRepositoryComponent testingComponent =
                    (TestingUModsRepositoryComponent) app.createUModsRepositoryComponentSingleton("5490387154623893", "2f6830a0-55d1-461f-b21a-92863089de80");
            testingComponent.inject(this);
        }
        Mockito.when(uModsRepositoryMock.getUModGateStatusUpdates()).thenReturn(Observable.never());
        myTargetContext = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
    }

    @After
    public void tearDown() throws Exception {
        Mockito.reset(uModsRepositoryMock);
        //TODO find out why tests would fail on E/IdlingResourceRegistry: Attempted to register resource with same names:
        Espresso.unregisterIdlingResources(EspressoIdlingResource.getIdlingResource());
    }

    @Test
    public void animations_should_be_disabled(){

        Date today = new Date();
        assertThat(today.getDay(),is(27));
        assertThat(today.getMonth(),is(2));
        assertThat(today.getYear(),is(2019));
        //assertThat("Check testing device has all animations disabled!!",today.isEqual(testerDate),is(true));
    }

    @Test
    public void Given_ThereIsNoUModsFound_When_ActivityIsLaunched_Then_NoUModsHasBeenFoundNotificationsIsShowUp(){
        //Given
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.empty());

        //When
        mIntentsTestRule.launchActivity(null);

        //Then
        onView(withId(R.id.no_umods_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.no_umods_main)).check(matches(isDisplayed()));
        onView(withId(R.id.no_umods_main)).check(matches(withText("No se encontraron módulos conectados a la red.")));
    }

    @Test
    public void Given_ThereIsANotConfiguredUMod_When_ActivityIsLaunched_Then_OnlySettingsButtonAndUModNameAreShowed(){
        //Given
        //When the resource is still busy check the progress bar is visible.
        EspressoIdlingResource.getIdlingResource().registerIdleTransitionCallback(() -> onView(withId(R.id.progress_bar)).check(matches(isDisplayed())));
        Espresso.registerIdlingResources(EspressoIdlingResource.getIdlingResource());

        UMod uMod = new UMod("333333333","555555555");
        uMod.setuModSource(UMod.UModSource.LAN_SCAN);
        uMod.setState(UMod.State.AP_MODE);
        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.just(uMod));

        //When
        mIntentsTestRule.launchActivity(null);

        //When the resource is idle check the progress bar doesn't exists.
        onView(withId(R.id.progress_bar)).check(doesNotExist());

        //Then
        checkNotConfiguredModuleLayout(0,uMod);
    }

    @Test
    public void Given_ThereIsANotConfiguredUMod_WhenSettingsIsRequested_ThenConfigUModActivityIsLaunched(){
        //Given
        UMod uMod = new UMod("333333333","555555555");
        uMod.setuModSource(UMod.UModSource.LAN_SCAN);
        uMod.setState(UMod.State.AP_MODE);
        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.just(uMod));
        mIntentsTestRule.launchActivity(null);
        Intent fakeIntent = new Intent();
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(
                Activity.RESULT_OK, fakeIntent);
        Matcher<Intent> expectedIntent = allOf(hasComponent(UModConfigActivity.class.getName()), hasExtra(UModConfigFragment.ARGUMENT_CONFIG_UMOD_ID,"333333333"));
        intending(expectedIntent).respondWith(result);

        //When
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(0).onChildView(withId(R.id.umod_item_settings_button)).perform(click());

        //Then
        intended(expectedIntent, times(1));
    }

    @Test
    public void Given_ThereIsAUModInStationModeAndUserIsUnauthorized_When_ActivityIsLaunched_Then_OnlyUModAliasAndSliderAreShownUp(){
        //Given
        //When the resource is still busy check the progress bar is visible.
        EspressoIdlingResource.getIdlingResource().registerIdleTransitionCallback(() -> onView(withId(R.id.progress_bar)).check(matches(isDisplayed())));
        Espresso.registerIdlingResources(EspressoIdlingResource.getIdlingResource());

        UMod uMod = new UMod("333333333","555555555");
        uMod.setuModSource(UMod.UModSource.LAN_SCAN);
        uMod.setState(UMod.State.STATION_MODE);
        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.just(uMod));

        //When
        mIntentsTestRule.launchActivity(null);

        //When the resource is idle check the progress bar doesn't exists.
        onView(withId(R.id.progress_bar)).check(doesNotExist());

        //Then
        checkUnauthorizedModuleLayout(0,uMod);
    }

    @Test
    public void Given_ThereIsUModFoundAndUserIsUnauthorizedAndRequestIsSuccessful_When_AccessIsRequested_Then_SnackbarInformsOKAndUModAppearsWithPendingLayout(){
        //Given
        Espresso.registerIdlingResources(EspressoIdlingResource.getIdlingResource());
        UMod uMod = new UMod("333333333","555555555");
        uMod.setuModSource(UMod.UModSource.LAN_SCAN);
        uMod.setState(UMod.State.STATION_MODE);
        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
            //Para initial load
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.just(uMod));
            //Para Request Access Use Case
        Mockito.when(uModsRepositoryMock.getUMod(uMod.getUUID())).thenReturn(Observable.just(uMod));
        CreateUserRPC.Result createResult = new CreateUserRPC.Result(APIUserType.Guest);
        Mockito.when(uModsRepositoryMock.createUModUser(Mockito.any(UMod.class), Mockito.any(CreateUserRPC.Arguments.class))).
                thenReturn(Observable.just(createResult));
            //Para el segundo load
        Mockito.when(uModsRepositoryMock.getUserLevel(Mockito.any(UMod.class), Mockito.any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.just(new GetUserLevelRPC.Result("5615131321351",APIUserType.Guest)));
        mIntentsTestRule.launchActivity(null);

        //When
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(0).onChildView(withId(R.id.card_slider)).perform(swipeRight());
        //try { Thread.sleep(700); } catch (InterruptedException e) { e.printStackTrace(); }
        //Then
        checkPendingModuleLayout(0,uMod);
            //Snackbar
        onView(allOf(withId(com.google.android.material.R.id.snackbar_text),withText(R.string.request_access_completed_message))).check(matches(isDisplayed()));
    }

    @Test
    public void Given_ThereIsAnUnauthorizedUModAndRequestAccessFails_When_AccessIsRequested_Then_SnackbarInformsFailAndUModRemainsUnauthorized(){
        Espresso.registerIdlingResources(EspressoIdlingResource.getIdlingResource());
        //Given
        UMod uMod = new UMod("333333333","555555555");
        uMod.setuModSource(UMod.UModSource.LAN_SCAN);
        uMod.setState(UMod.State.STATION_MODE);
        uMod.setAppUserLevel(UModUser.Level.UNAUTHORIZED);
            //Para initial load
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.just(uMod));
            //Para Request Access Use Case
        when(uModsRepositoryMock.getUMod(uMod.getUUID())).thenReturn(Observable.just(uMod));
        IOException ioException = new IOException("");

        when(uModsRepositoryMock.createUModUser(Mockito.any(UMod.class), Mockito.any(CreateUserRPC.Arguments.class))).
                thenReturn(Observable.error(ioException));
            //Para el segundo load
        Mockito.when(uModsRepositoryMock.getUserLevel(Mockito.any(UMod.class), Mockito.any(GetUserLevelRPC.Arguments.class)))
                .thenReturn(Observable.just(new GetUserLevelRPC.Result("5615131321351",APIUserType.Guest)));

        mIntentsTestRule.launchActivity(null);

        //When
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(0).onChildView(withId(R.id.card_slider)).perform(swipeRight());

        //Then
        checkUnauthorizedModuleLayout(0,uMod);
            //Snackbar
        onView(allOf(withId(com.google.android.material.R.id.snackbar_text),withText(R.string.request_access_failed_message))).check(matches(isDisplayed()));
    }

    @Test
    public void Given_SingleOnlineUModWithClosedStatusWasDiscovered_When_ActivityIsLaunched_Then_AuthorizedOnlineLayoutWithClosedTagIsDisplayed(){
        //Given
        //When the resource is still busy check the progress bar is visible.
        EspressoIdlingResource.getIdlingResource().registerIdleTransitionCallback(() -> onView(withId(R.id.progress_bar)).check(matches(isDisplayed())));
        Espresso.registerIdlingResources(EspressoIdlingResource.getIdlingResource());

        UMod uMod = new UMod("333333333","555555555");
        uMod.setuModSource(UMod.UModSource.LAN_SCAN);
        uMod.setState(UMod.State.STATION_MODE);
        uMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        uMod.setGateStatus(UMod.GateStatus.CLOSED);
        Date date = new Date();
        uMod.setLastUpdateDate(date);
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.just(uMod));

        //When
        mIntentsTestRule.launchActivity(null);

        //When the resource is idle check the progress bar doesn't exists.
        onView(withId(R.id.progress_bar)).check(doesNotExist());

        //Then
        checkAuthorizedModuleOnlineLayout(0,uMod);
        checkGateStatusClosed(0);
    }

    @Test
    public void Given_SingleOfflineUModWithUnknownGateStatusWasRecentlyCached_When_ActivityIsLaunched_Then_AuthorizedOnlineLayoutWithUnknownTagIsDisplayed(){
        //Given
        //When the resource is still busy check the progress bar is visible.
        EspressoIdlingResource.getIdlingResource().registerIdleTransitionCallback(() -> onView(withId(R.id.progress_bar)).check(matches(isDisplayed())));
        Espresso.registerIdlingResources(EspressoIdlingResource.getIdlingResource());

        UMod uMod = new UMod("333333333","555555555");
        uMod.setuModSource(UMod.UModSource.CACHE);
        uMod.setState(UMod.State.STATION_MODE);
        uMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        uMod.setGateStatus(UMod.GateStatus.UNKNOWN);
        Date date = new Date();
        uMod.setLastUpdateDate(date);
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.just(uMod));

        //When
        mIntentsTestRule.launchActivity(null);

        //try { Thread.sleep(4500); } catch (InterruptedException e) { e.printStackTrace(); }

        //Then
        //When the resource is idle check the progress bar doesn't exists.
        onView(withId(R.id.progress_bar)).check(doesNotExist());

        checkAuthorizedModuleOnlineLayout(0,uMod);
        checkGateStatusUnkown(0);
    }

    @Test
    public void Given_ThereIsAnAuthorizedUMod_When_TriggerIsPerformedThen_SnackbarInformsFailsSuccess(){
        Espresso.registerIdlingResources(EspressoIdlingResource.getIdlingResource());
        //Given
        UMod uMod = new UMod("333333333","555555555");
        uMod.setuModSource(UMod.UModSource.LAN_SCAN);
        uMod.setState(UMod.State.STATION_MODE);
        //TODO se cambia el estado automaicamente??
        uMod.setGateStatus(UMod.GateStatus.CLOSED);
        uMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        Date date = new Date();
        uMod.setLastUpdateDate(date);
        Mockito.when(uModsRepositoryMock.getUMod(uMod.getUUID())).thenReturn(Observable.just(uMod));
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.just(uMod));
        mIntentsTestRule.launchActivity(null);

        Mockito.when(uModsRepositoryMock.triggerUMod(Mockito.any(UMod.class),Mockito.any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.just(new TriggerRPC.Result("Success")));

        //When
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(0).onChildView(withId(R.id.card_slider)).perform(swipeRight());

        //try { Thread.sleep(4500); } catch (InterruptedException e) { e.printStackTrace(); }

        //Then
        //onView(allOf(withId(com.google.android.material.R.id.snackbar_text),withText(R.string.trigger_success_message))).check(matches(isDisplayed()));
        checkSnackBarDisplayedByMessage(R.string.trigger_success_message);
        checkAuthorizedModuleLayout(0,uMod);
        //Aunque no está obligado a actualizar los módulos, es necesario testear el estado de la puerta?
    }

    @Test
    public void Given_ThereIsAnAuthorizedUModsAndRequestAccessFailsBecauseUserWasDelete_When_TriggerIsPerformed_Then_SnackbarInformsFailsAndUModIsErased(){
        Espresso.registerIdlingResources(EspressoIdlingResource.getIdlingResource());
        //Given
        UMod uMod = new UMod("333333333","555555555");
        uMod.setuModSource(UMod.UModSource.CACHE);
        uMod.setState(UMod.State.STATION_MODE);
        //TODO se cambia el estado automaicamente??
        uMod.setGateStatus(UMod.GateStatus.UNKNOWN);
        uMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        Date date = new Date();
        date.setTime(date.getTime()-20000L);
        uMod.setLastUpdateDate(date);
        HttpException httpException = new HttpException(
                Response.error(
                        401,
                        ResponseBody.create(
                                MediaType.parse("text/plain"),
                                "Error 401")
                )
        );
        Mockito.when(uModsRepositoryMock.getUMod(uMod.getUUID())).thenReturn(Observable.just(uMod));
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.just(uMod));
        mIntentsTestRule.launchActivity(null);

        Mockito.when(uModsRepositoryMock.triggerUMod(Mockito.any(UMod.class),Mockito.any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.error(httpException));

        //When
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(0).onChildView(withId(R.id.card_slider)).perform(swipeRight());

        //Then
        onView(allOf(withId(com.google.android.material.R.id.snackbar_text),withText(R.string.trigger_fail_message))).check(matches(isDisplayed()));
        //checkNoModulesVisible();
        onView(withId(R.id.umods_list)).check(matches(not(hasDescendant(withText(uMod.getAlias())))));
    }

    @Test
    public void Given_ThereIsAnAuthorizedUModAndRequestAccessFailsBecauseIOException_When_TriggerIsPerfomed_Then(){
        Espresso.registerIdlingResources(EspressoIdlingResource.getIdlingResource());
        //Given
        UMod uMod = new UMod("333333333","555555555");
        uMod.setuModSource(UMod.UModSource.CACHE);
        uMod.setState(UMod.State.STATION_MODE);
        //TODO se cambia el estado automaicamente??
        uMod.setGateStatus(UMod.GateStatus.UNKNOWN);
        uMod.setAppUserLevel(UModUser.Level.AUTHORIZED);
        Date date = new Date();
        date.setTime(date.getTime()-20000L);
        uMod.setLastUpdateDate(date);
        IOException ioException = new IOException("IOExcep");
        Mockito.when(uModsRepositoryMock.getUMod(uMod.getUUID())).thenReturn(Observable.just(uMod));
        Mockito.when(uModsRepositoryMock.getUModsOneByOne()).thenReturn(Observable.just(uMod));
        mIntentsTestRule.launchActivity(null);

        Mockito.when(uModsRepositoryMock.triggerUMod(Mockito.any(UMod.class),Mockito.any(TriggerRPC.Arguments.class)))
                .thenReturn(Observable.error(ioException));

        //When
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(0).onChildView(withId(R.id.card_slider)).perform(swipeRight());

        //try { Thread.sleep(4500); } catch (InterruptedException e) { e.printStackTrace(); }

        //Then
        checkSnackBarDisplayedByMessage(R.string.trigger_fail_message);
        //checkNoModulesVisible();
        checkAuthorizedModuleOfflineLayout(0,uMod);
    }

    private void checkSnackBarDisplayedByMessage(@StringRes int message) {
        onView(withText(message))
                .check(matches(withEffectiveVisibility(
                        ViewMatchers.Visibility.VISIBLE
                )));
    }

    void checkNotConfiguredModuleLayout(int position, UMod uMod){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.umod_item_settings_button)).check(matches(isDisplayed()));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_item_notif_indicator)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_main_text)).check(matches(withText(uMod.getAlias())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.item_time_text)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.connection_tag_text)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(not(isDisplayed())));
    }

    void checkUnauthorizedModuleLayout(int position, UMod uMod){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.umod_item_settings_button)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_item_notif_indicator)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_main_text)).check(matches(withText(uMod.getAlias())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.item_time_text)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.connection_tag_text)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(not(isDisplayed())));
            //Slider
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(isDisplayed()));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderText(GlobalConstants.REQUEST_ACCESS_SLIDER_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderOuterColour(R.color.request_access_slider_background, myTargetContext)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderInnerColour(R.color.request_access_slider_text, myTargetContext)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderTextColor(R.color.request_access_slider_text, myTargetContext)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(not(withSliderLocked())));
    }

    void checkPendingModuleLayout(int position, UMod uMod){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.umod_item_settings_button)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_item_notif_indicator)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_main_text)).check(matches(withText(uMod.getAlias())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.item_time_text)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.connection_tag_text)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(not(isDisplayed())));
            //Slider
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(isDisplayed()));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderText(GlobalConstants.PENDING_SLIDER_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderOuterColour(R.color.request_access_slider_background, myTargetContext)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderInnerColour(R.color.request_access_slider_text, myTargetContext)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderTextColor(R.color.request_access_slider_text, myTargetContext)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderLocked()));
    }

    void checkAuthorizedModuleLayout(int position, UMod uMod){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.umod_item_settings_button)).check(matches(isDisplayed()));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_item_notif_indicator)).check(matches(isDisplayed()));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_main_text)).check(matches(withText(uMod.getAlias())));
            //Tags
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.connection_tag_text)).check(matches(isDisplayed()));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(isDisplayed()));
            //Slider
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(isDisplayed()));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderText(GlobalConstants.TRIGGER_SLIDER_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderOuterColour(R.color.trigger_slider_background, myTargetContext)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderInnerColour(R.color.trigger_slider_text, myTargetContext)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(withSliderTextColor(R.color.trigger_slider_text, myTargetContext)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.card_slider)).check(matches(not(withSliderLocked())));
    }
    void checkAuthorizedModuleOnlineLayout(int position, UMod uMod){
        checkAuthorizedModuleLayout(position,uMod);

        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.item_time_text)).check(matches(not(isDisplayed())));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.connection_tag_text)).check(matches(withText(GlobalConstants.ONLINE_TAG__TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.connection_tag_text)).check(matches(withTagTextColor(UModsFragment.UModViewModelColors.ONLINE_TAG_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.connection_tag)).check(matches(withTagColor(UModsFragment.UModViewModelColors.ONLINE_TAG)));
    }

    void checkAuthorizedModuleOfflineLayout(int position, UMod uMod){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.item_time_text)).check(matches(isDisplayed()));

        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.connection_tag_text)).check(matches(withText("OFFLINE")));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.connection_tag_text)).check(matches(withTagTextColor(UModsFragment.UModViewModelColors.OFFLINE_TAG_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.connection_tag)).check(matches(withTagColor(UModsFragment.UModViewModelColors.OFFLINE_TAG)));

        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag)).check(matches(not(isDisplayed())));
    }

    /*   TODO SOBRE EL BOTON REFRESH
    DEBERIA HACER TEST TAMBIEN SOBRE CUANDO EJECUTO EL BOTON REFRESH, VIENDO SI SE ACTUALIZA:
        - POR EJEMPLO, ANTES ESTABA PENDING Y AHORA EL REPOSITORIO ME DICE QUE ESTOY AUTORIZADO(TENER EN CUENTA LAS ETIQUETAS VISIBLES
        - ALGUNO SE PUSO LA GORRA Y ME BORRÓ (DEBERIA CHEQUEAR SI PUEDO ENVIAR SOLICITUD)
     */
    /*
    Todo hacer un test sobre la campanita
     */

    public static Matcher<View> withSliderText (final String text){
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return ((SlideToActView) item).getText().equals(text);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText ("Slider text: " + text);
            }
        };
    }

    public static Matcher<View> withSliderOuterColour(int colour, Context myContext){
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return ((SlideToActView) item).getOuterColor()
                        == ContextCompat.getColor(myContext, colour);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText ("Slider Outer Color value: " + ContextCompat.getColor(myContext, colour));
            }
        };
    }

    public static Matcher<View> withSliderInnerColour(int colour, Context myContext){
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return ((SlideToActView) item).getInnerColor()
                        == ContextCompat.getColor(myContext, colour);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText ("Slider Inner Color value: " + ContextCompat.getColor(myContext, colour));
            }
        };
    }

    public static Matcher<View> withSliderTextColor(int colour, Context myContext){
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return ((SlideToActView) item).getTextColor()
                        == ContextCompat.getColor(myContext, colour);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText ("Slider Text Color value: " + ContextCompat.getColor(myContext, colour));
            }
        };
    }

    public static Matcher<View> withSliderLocked(){
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return ((SlideToActView) item).isLocked();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText ("Slider Inner is locked");
            }
        };
    }

    public static Matcher<View> withTagColor(UModsFragment.UModViewModelColors color){
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return areDrawablesIdentical(item.getBackground(),
                        myTargetContext.getResources().getDrawable(color.asActualResource()));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText ("Tag color matches: " + color);
            }
        };
    }

    public static Matcher<View> withTagTextColor(UModsFragment.UModViewModelColors color){
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                Log.d("COLOR TEXT TAG"," "+ ((TextView) item).getCurrentTextColor());
                Log.d("COLOR TEXT TAG"," "+ ContextCompat.getColor(myTargetContext, color.asActualResource()));
                return ((TextView) item).getCurrentTextColor()==
                ContextCompat.getColor(myTargetContext, color.asActualResource());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText ("Tag Text color matches: " + color);
            }
        };
    }

    public static void checkGateStatusOpen(int position){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withText("ABIERTO")));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withTagTextColor(UModsFragment.UModViewModelColors.OPEN_GATE_TAG_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag)).check(matches(withTagColor(UModsFragment.UModViewModelColors.OPEN_GATE_TAG)));
    }

    public static void checkGateStatusClosed(int position){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withText("CERRADO")));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withTagTextColor(UModsFragment.UModViewModelColors.CLOSED_GATE_TAG_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag)).check(matches(withTagColor(UModsFragment.UModViewModelColors.CLOSED_GATE_TAG)));
    }
    public static void checkGateStatusClosing(int position){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withText("CERRANDO")));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withTagTextColor(UModsFragment.UModViewModelColors.CLOSED_GATE_TAG_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag)).check(matches(withTagColor(UModsFragment.UModViewModelColors.CLOSED_GATE_TAG)));
    }

    public static void checkGateStatusOpening(int position){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withText("ABRIENDO")));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withTagTextColor(UModsFragment.UModViewModelColors.OPEN_GATE_TAG_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag)).check(matches(withTagColor(UModsFragment.UModViewModelColors.OPEN_GATE_TAG)));
    }

    public static void checkGateStatusPartialOpening(int position){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withText("SEMI ABIERTO")));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withTagTextColor(UModsFragment.UModViewModelColors.OPEN_GATE_TAG_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag)).check(matches(withTagColor(UModsFragment.UModViewModelColors.OPEN_GATE_TAG)));
    }

    public static void checkGateStatusUnkown(int position){
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withText("DESCONOCIDO")));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag_text)).check(matches(withTagTextColor(UModsFragment.UModViewModelColors.UNKNOWN_GATE_STATUS_TAG_TEXT)));
        onData(anything()).inAdapterView(withId(R.id.umods_list)).atPosition(position).onChildView(withId(R.id.gate_status_tag)).check(matches(withTagColor(UModsFragment.UModViewModelColors.UNKNOWN_GATE_STATUS_TAG)));
    }

    public static boolean areDrawablesIdentical(Drawable drawableA, Drawable drawableB) {
        Drawable.ConstantState stateA = drawableA.getConstantState();
        Drawable.ConstantState stateB = drawableB.getConstantState();
        // If the constant state is identical, they are using the same drawable resource.
        //return stateA != null && stateB != null && stateA.equals(stateB);
        // However, the opposite is not necessarily true.

        return (stateA != null && stateB != null && stateA.equals(stateB))
                || getBitmap(drawableA).sameAs(getBitmap(drawableB));
    }

    public static Bitmap getBitmap(Drawable drawable) {
        Bitmap result;
        if (drawable instanceof BitmapDrawable) {
            result = ((BitmapDrawable) drawable).getBitmap();
        } else {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            // Some drawables have no intrinsic width - e.g. solid colours.
            if (width <= 0) {
                width = 1;
            }
            if (height <= 0) {
                height = 1;
            }

            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return result;
    }
}
