package com.urbit_iot.onekey.umods;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.ncorti.slidetoact.SlideToActView;
import com.urbit_iot.onekey.R;
import com.urbit_iot.onekey.umodconfig.UModConfigActivity;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.umodconfig.UModConfigFragment;
import com.urbit_iot.onekey.umodsnotification.UModsNotifService;
import com.urbit_iot.onekey.util.GlobalConstants;
import com.urbit_iot.onekey.util.schedulers.BaseSchedulerProvider;
import com.yarolegovich.lovelydialog.LovelyChoiceDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Display a grid of {@link UMod}s. User can choose to view all, active or completed tasks.
 */
public class UModsFragment extends Fragment implements UModsContract.View {

    @NonNull
    private static final int REQUEST_UMOD_CONFIG = 1;

    private UModsContract.Presenter mPresenter;

    private UModsAdapter mListAdapter;

    private View mNoUModsView;

    private ImageView mNoUModsIcon;

    private TextView mNoUModsMainView;

    //private TextView mNoUModsAddView;

    private LinearLayout mUModsView;

    private TextView mFilteringLabelView;

    private Vibrator mVibrator;

    private Switch ongoingNotificationSwitch;

    private ProgressBar umodsScanProgressBar;

    private ListView mListView;

    private LovelyChoiceDialog calibrationDialogGateStatus;
    private LovelyStandardDialog calibrationDialogInitialConfirmation;

    @NonNull
    private BaseSchedulerProvider mSchedulerProvider;

    public UModsFragment() {
        // Requires empty public constructor
    }

    public static UModsFragment newInstance() {
        return new UModsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListAdapter = new UModsAdapter(this.getContext(), new ArrayList<>(0), mItemListener, getResources(), mPresenter);
        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("umods_fr", "On Resume");
        mPresenter.subscribe();
    }

    @Override
    public void setPresenter(@NonNull UModsContract.Presenter presenter) {
        mPresenter = checkNotNull(presenter);
    }

    @Override
    public void setSchedulerProvider(@NonNull BaseSchedulerProvider schedulerProvider) {
        mSchedulerProvider = checkNotNull(schedulerProvider);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.umods_frag, container, false);

        // Set up tasks view
        this.mListView = (ListView) root.findViewById(R.id.umods_list);
        this.mListView.setAdapter(mListAdapter);
        //mFilteringLabelView = (TextView) root.findViewById(R.id.umods_filtering_label);
        mUModsView = (LinearLayout) root.findViewById(R.id.umods_linear_layout);

        // Set up  no tasks view
        mNoUModsView = root.findViewById(R.id.no_umods);
        mNoUModsIcon = (ImageView) root.findViewById(R.id.no_umods_icon);
        mNoUModsMainView = (TextView) root.findViewById(R.id.no_umods_main);
        this.umodsScanProgressBar = root.findViewById(R.id.umods_scan_load_bar);
        this.umodsScanProgressBar.setVisibility(View.INVISIBLE);

        setupCalibrationDialogGateStatus();
        setupCalibrationDialogInitialConfirmation();

        this.ongoingNotificationSwitch = (Switch) root.findViewById(R.id.umods_frag__ongoing_notif_switch);
        this.ongoingNotificationSwitch.setChecked(this.mPresenter.fetchOngoingNotificationPreference());
        this.ongoingNotificationSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            mPresenter.saveOngoingNotificationPreference(isChecked);
            if (isChecked){
                startOngoingNotification();
                mPresenter.saveOngoingNotificationPreference(true);
            } else {
                shutdownOngoingNotification();
                mPresenter.saveOngoingNotificationPreference(false);
            }

        });
        // Set up floating action button
        FloatingActionButton fab = getActivity().findViewById(R.id.fab_add_umod);
        fab.setImageResource(R.drawable.ic_update);
        fab.setOnClickListener(v -> mPresenter.loadUMods(true));

        setHasOptionsMenu(true);

        return root;
    }

    @Override
    public void showDisconnectedSensorDialog() {

    }

    @Override
    public void showCalibrationDialogs() {
        this.calibrationDialogInitialConfirmation.show();
    }

    private void setupCalibrationDialogInitialConfirmation() {
        this.calibrationDialogInitialConfirmation = new LovelyStandardDialog(this.getContext(), LovelyStandardDialog.ButtonLayout.VERTICAL)
                .setTopColorRes(R.color.colorAccent)
                .setButtonsColorRes(R.color.colorPrimary)
                .setIcon(R.drawable.ic_calibration_ic)
                .setTitle("CALIBRATION")
                .setMessage("Si desea calibrar su portón espere hasta que se haya  detenido completamente y luego presione \"SIGUIENTE\"")
                .setPositiveButton("SIGUIENTE", v -> calibrationDialogGateStatus.show())
                .setNegativeButton("CANCELAR", null);
    }

    private void setupCalibrationDialogGateStatus() {
        String[] items = {"Abierto completamente", "Cerrado", "Desconozco"};
        this.calibrationDialogGateStatus = new LovelyChoiceDialog(this.getContext())
                .setTopColorRes(R.color.colorAccent)
                .setTitle("CALIBRACION")
                .setMessage("Ahora mismo, en qué estado se encuentra su portón?")
                .setIcon(R.drawable.ic_calibration_ic)
                //.setItems(items, (positions, items1) -> showRequestAccessCompletedMessage())
                .setItems(items,(position, item) -> {
                    mPresenter.processCalibrationDialogChoice(position);
                });
    }

    @Override
    public void startOngoingNotification() {
        Context fragmentContext = getContext();
        if (fragmentContext != null){
            Intent serviceIntent = new Intent(fragmentContext, UModsNotifService.class);
            serviceIntent.setAction(GlobalConstants.ACTION.STARTFOREGROUND);
            fragmentContext.startService(serviceIntent);
        } else {
            Log.e("umods_frag", "Fragment content returned null.");
        }
    }

    @Override
    public void shutdownOngoingNotification() {
        Context fragmentContext = getContext();
        if (fragmentContext != null){
            Intent serviceIntent = new Intent(fragmentContext, UModsNotifService.class);
            serviceIntent.setAction(GlobalConstants.ACTION.SHUTDOWN_SERVICE);
            fragmentContext.startService(serviceIntent);
        } else {
            Log.e("umods_frag", "Fragment content returned null.");
        }

    }

    @Override
    public void refreshOngoingNotification() {
        if (UModsNotifService.SERVICE_IS_ALIVE){
            Context context = getContext();
            if (context != null){
                Intent serviceIntent = new Intent(context, UModsNotifService.class);
                serviceIntent.setAction(GlobalConstants.ACTION.REFRESH_ON_CACHED);
                context.startService(serviceIntent);
            } else {
                Log.e("config_fr", "Context is null");
            }
        }
    }

    @Override
    public void showAlienUModsCleared() {
        showMessage(getString(R.string.all_alien_umods_removed));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_umods:
                mPresenter.clearAlienUMods();
                break;
            case R.id.menu_filter_umods:
                showFilteringPopUpMenu();
                break;
            case R.id.menu_refresh_umods:
                mPresenter.loadUMods(true);
                break;
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.umods_fragment_menu, menu);
    }

    @Override
    public void showFilteringPopUpMenu() {
        PopupMenu popup = new PopupMenu(getContext(), getActivity().findViewById(R.id.menu_filter_umods));
        popup.getMenuInflater().inflate(R.menu.filter_umods, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.active_umods:
                        mPresenter.setFiltering(UModsFilterType.NOTIF_DIS_UMODS);
                        break;
                    case R.id.completed_umods:
                        mPresenter.setFiltering(UModsFilterType.NOTIF_EN_UMODS);
                        break;
                    default:
                        mPresenter.setFiltering(UModsFilterType.ALL_UMODS);
                        break;
                }
                mPresenter.loadUMods(false);
                return true;
            }
        });

        popup.show();
    }

    @Override
    public void showOpenCloseSuccess() {
        showMessage(getString(R.string.trigger_success_message));
    }

    @Override
    public void showOpenCloseFail() {
        showMessage(getString(R.string.trigger_fail_message));
    }

    @Override
    public void showRequestAccessFailedMessage() {
        showMessage(getString(R.string.request_access_failed_message));
    }

    @Override
    public void showRequestAccessCompletedMessage() {
        showMessage(getString(R.string.request_access_completed_message));
    }

    @Override
    public void showCalibrationSuccessMessage() {
        showMessage("CALIBRACIÓN EXITOSA");
    }

    @Override
    public void showCalibrationFailureMessage() {
        showMessage("CALIBRACIÓN FALLIDA, REINTENTE LUEGO");
    }

    @Override
    public void enableActionSlider(String uModUUID) {
        mListAdapter.makeButtonVisible(uModUUID);
    }


    @Override
    public void setLoadingIndicator(final boolean active) {

        /*
        if (getView() == null) {
            return;
        }
        final SwipeRefreshLayout srl =
                (SwipeRefreshLayout) getView().findViewById(R.id.umods_refresh_layout);

        // Make sure setRefreshing() is called after the layout is done with everything else.
        srl.post(new Runnable() {
            @Override
            public void run() {
                srl.setRefreshing(active);
            }
        });
        */
        if (active){
            this.umodsScanProgressBar.setVisibility(View.VISIBLE);
        } else {
            this.umodsScanProgressBar.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void showUMods(List<UModViewModel> uModViewModels) {
        mListAdapter.replaceData(uModViewModels);

        mUModsView.setVisibility(View.VISIBLE);
        mNoUModsView.setVisibility(View.GONE);
    }

    public void showUMod(UModViewModel uModViewModel){
        /*
        if(mNoUModsAddView.getVisibility() == View.VISIBLE){
            mNoUModsView.setVisibility(View.GONE);
        }
        */
        mListAdapter.addItem(uModViewModel);
        mUModsView.setVisibility(View.VISIBLE);
        mNoUModsView.setVisibility(View.GONE);
    }

    @Override
    public void appendUMod(UModViewModel uMod) {
        mListAdapter.addItem(uMod);
    }

    @Override
    public void removeItem(String uuid) {
        mListAdapter.removeItem(uuid);
    }

    @Override
    public void showNotificationDisabledUMods() {
        showNoTasksViews(
                getResources().getString(R.string.no_umods_active),
                R.drawable.ic_check_circle_24dp,
                false
        );
    }

    @Override
    public void showNoUMods() {
        showNoTasksViews(
                getResources().getString(R.string.no_umdos_all),
                R.drawable.ic__modules_not_found_black,
                false
        );
    }

    @Override
    public void showNotificationEnabledUMods() {
        showNoTasksViews(
                getResources().getString(R.string.no_umods_completed),
                R.drawable.ic_verified_user_24dp,
                false
        );
    }

    private void showNoTasksViews(String mainText, int iconRes, boolean showAddView) {
        mUModsView.setVisibility(View.GONE);
        mNoUModsView.setVisibility(View.VISIBLE);

        mNoUModsMainView.setText(mainText);
        mNoUModsIcon.setImageDrawable(getResources().getDrawable(iconRes));
        //mNoUModsAddView.setVisibility(showAddView ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showNotificationDisabledUModsFilterLabel() {
        mFilteringLabelView.setText(getResources().getString(R.string.label_umods_active));
    }

    @Override
    public void showNotificationEnabledUModsFilterLabel() {
        mFilteringLabelView.setText(getResources().getString(R.string.label_umods_completed));
    }

    @Override
    public void showAllFilterLabel() {
        mFilteringLabelView.setText(getResources().getString(R.string.label_umods_all));
    }

    @Override
    public void showAddUMod() {
        Intent intent = new Intent(getContext(), UModConfigActivity.class);
        startActivityForResult(intent, UModConfigActivity.REQUEST_ADD_TASK);
    }

    @Override
    public void showUModConfigUi(String uModUUID) {
        // in it's own Activity, since it makes more sense that way and it gives us the flexibility
        // to show some Intent stubbing.
        Intent intent = new Intent(getContext(), UModConfigActivity.class);
        intent.putExtra(UModConfigFragment.ARGUMENT_CONFIG_UMOD_ID, uModUUID);
        startActivityForResult(intent,REQUEST_UMOD_CONFIG);
    }

    @Override
    public void showUModNotificationEnabled() {
        showMessage(getString(R.string.ongoing_notif_enabled));
    }

    @Override
    public void showOngoingNotifStatusChanged(Boolean ongoingNotifStatus) {
        if(ongoingNotifStatus){
            showMessage(getString(R.string.ongoing_notif_enabled));
        } else {
            showMessage(getString(R.string.ongoing_notif_disbled));
        }
    }

    @Override
    public void showUModNotificationDisabled() {
        showMessage(getString(R.string.ongoing_notif_disbled));
    }

    @Override
    public void showLoadingUModsError() {
        showMessage(getString(R.string.loading_umod_error));
    }

    private void showMessage(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean isNotificationDisabled() {
        return isAdded();
    }

    @Override
    public void clearAllItems() {
        mListAdapter.replaceData(new ArrayList<>());
    }

    /**
     * Listener for clicks on tasks in the ListView.
     */
    UModItemListener mItemListener = new UModItemListener() {

        @Override
        public boolean isFragmentVisible() {
            return isVisible();
        }

        public void vibrateOnActionButtonClick(){
            //TODO make three consecutive shorter vibrations
            mVibrator.vibrate(100L);
            Observable.interval(500L, TimeUnit.MILLISECONDS)
                    .take(2)
                    .subscribeOn(mSchedulerProvider.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(aLong -> {
                        mVibrator.vibrate((aLong+1L)*200L);
                        return null;
                    })
                    .subscribe(o -> {},
                            throwable -> Log.e("UMODS_FRAG", "FAILED TO VIBRATE: "
                                    + throwable.getMessage(),throwable));
        }
        /*
        @Override
        public void onUModClick(UMod clickedUMod) {
            mPresenter.openUModConfig(clickedUMod);
        }
        */

        /*
        @Override
        public void onCompleteUModClick(UMod completedUMod) {
            mPresenter.enableUModNotification(completedUMod);
        }
        */

        /*
        @Override
        public void onActivateUModClick(UMod activatedUMod) {
            mPresenter.disableUModNotification(activatedUMod);
        }
        */

        /*
        @Override
        public void onActionButtonClick(UMod actedUMod) {
            mPresenter.userTriggerUMod(actedUMod);
        }

        @Override
        public void onRequestAccess(UMod requestedUMod) {
            mPresenter.requestAccess(requestedUMod);
        }

        */
    };

    private static class UModsAdapter extends BaseAdapter {

        private List<UModViewModel> mViewModelsList;
        private UModItemListener mItemListener;
        private Resources resources;
        private Context activityContext;
        private UModsContract.Presenter presenter;

        public UModsAdapter(Context context,
                            List<UModViewModel> uMods,
                            UModItemListener itemListener,
                            Resources resources, UModsContract.Presenter presenter) {
            this.activityContext = context;
            setList(uMods);
            mItemListener = itemListener;
            this.resources = resources;
            this.presenter = presenter;
        }

        public void replaceData(List<UModViewModel> uMods) {
            setList(uMods);
            notifyDataSetChanged();
        }

        private void setList(List<UModViewModel> uMods) {
            mViewModelsList = checkNotNull(uMods);
        }

        public void addItem(final UModViewModel viewModel){
            Log.d("umods_frag", "ViewModel: " + viewModel.hashCode() + "\nVM: " + viewModel.toString());
            //refreshList();
            String vmsCodes = "";
            for (UModViewModel vm:
                 mViewModelsList) {
                vmsCodes = vmsCodes + ", " + vm.hashCode();
            }

            Log.d("umods_frag", "VMS: " + vmsCodes);

            if( ! mViewModelsList.contains(viewModel)){
                mViewModelsList.add(viewModel);
            } else {
                //Iterator<UModViewModel> iterator = mViewModelsList.iterator();
                /*
                //for-each wont work since the removal causes the list size to change
                while (iterator.hasNext()){
                    if (iterator.next().equals(viewModel)){
                        iterator.remove();
                    }
                }
                mViewModelsList.add(viewModel);


                        */
                mViewModelsList.set(mViewModelsList.indexOf(viewModel),viewModel);
                /*
                Collections.sort(mViewModelsList, new Comparator<UModViewModel>() {
                    @Override
                    public int compare(UModViewModel uModViewModel, UModViewModel t1) {
                        return uModViewModel.getItemMainText().compareTo(t1.getItemMainText());
                    }
                });
                */
            }
            notifyDataSetChanged();
        }

        public void makeButtonVisible(String uModUUID){
            for (UModViewModel viewModel : mViewModelsList){
                if (viewModel.getuModUUID().equals(uModUUID)){
                    viewModel.setSliderVisible(true);
                    viewModel.setSliderEnabled(true);
                    notifyDataSetChanged();
                }
            }
        }
        /*
        public void refreshList(){
            for (UModViewModel viewModel: mViewModelsList) {
                Long minutesOld = TimeUnit.MILLISECONDS.toMinutes((new Date()).getTime() - viewModel.getLastUpdateDate().getTime());
                if (minutesOld > 5L){
                    mViewModelsList.remove(viewModel);
                }
            }
        }
        */

        @Override
        public int getCount() {
            return mViewModelsList.size();
        }

        @Override
        public UModViewModel getItem(int i) {
            return mViewModelsList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            View rowView = view;

            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            //TODO consider the view holder pattern. Or the below null check.
            rowView = inflater.inflate(R.layout.umod_item_card4, viewGroup, false);
            /*
            if (rowView == null) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                rowView = inflater.inflate(R.layout.umod_item_card2, viewGroup, false);
            }
            */
            final UModViewModel viewModel = getItem(i);
            //TODO if R.id.title is renamed then the project doesn't build. Why??
            TextView mainText = (TextView) rowView.findViewById(R.id.card_main_text);
            mainText.setSelected(true);//This will make the text rotate when too large.
            mainText.setText(viewModel.getItemMainText());

            final ImageView ongoingNotifIndicator = rowView.findViewById(R.id.card_item_notif_indicator);

            RelativeLayout umodTags = rowView.findViewById(R.id.umod_tags);

            if (viewModel.isModuleTagsVisible()){
                RelativeLayout connectionTag = rowView.findViewById(R.id.connection_tag);
                connectionTag.setBackground(resources.getDrawable(viewModel.getConnectionTagColor().asActualResource()));
                TextView connectionTagText = rowView.findViewById(R.id.connection_tag_text);
                connectionTagText.setText(viewModel.getConnectionTagText());
                connectionTagText.setTextColor(ContextCompat.getColor(activityContext, viewModel.getConnectionTagTextColor().asActualResource()));

                RelativeLayout gateStatusTag = rowView.findViewById(R.id.gate_status_tag);
                gateStatusTag.setBackground(resources.getDrawable(viewModel.getGateStatusTagColor().asActualResource()));
                TextView gateStatusTagText = rowView.findViewById(R.id.gate_status_tag_text);
                gateStatusTagText.setText(viewModel.getGateStatusTagText());
                gateStatusTagText.setTextColor(ContextCompat.getColor(activityContext, viewModel.getGateStatusTagTextColor().asActualResource()));

                umodTags.setVisibility(View.VISIBLE);
            } else {
                umodTags.setVisibility(View.GONE);
            }

            TextView timeText = rowView.findViewById(R.id.item_time_text);
            if (viewModel.isTimeTextVisible()){
                timeText.setText(viewModel.getTimeText());
                timeText.setVisibility(View.VISIBLE);
            } else {
                timeText.setVisibility(View.INVISIBLE);
            }


            ImageButton settingsButton = rowView.findViewById(R.id.umod_item_settings_button);
            if (viewModel.isSettingsButtonVisible()){
                settingsButton.setOnClickListener(buttonView -> {
                    if (mItemListener.isFragmentVisible() && this.presenter!=null){
                        Log.d("umods_frag","INDEX:" + i
                                + "VM_HashCode: " + viewModel.hashCode()
                                + "\nVM: " + viewModel.toString());
                        viewModel.onSettingsButtonClicked(this.presenter);
                    }
                });
                settingsButton.setVisibility(View.VISIBLE);
            } else {
                settingsButton.setVisibility(View.INVISIBLE);
            }


            final SlideToActView actionSlider = rowView.findViewById(R.id.card_slider);

            if(viewModel.isSliderVisible()){
                actionSlider.setOuterColor(
                        ContextCompat.getColor(this.activityContext,
                                viewModel.getSliderBackgroundColor().asActualResource()));
                actionSlider.setInnerColor(ContextCompat.getColor(this.activityContext,viewModel.getSliderTextColor().asActualResource()));
                actionSlider.setLocked(!viewModel.isSliderEnabled());
                actionSlider.setEnabled(viewModel.isSliderEnabled());
                actionSlider.setText(viewModel.getSliderText());
                if (viewModel.isSliderEnabled()){
                    //actionSlider.setLocked(false);
                    actionSlider.setEnabled(true);
                }
                actionSlider.setVisibility(View.VISIBLE);
            } else {
                actionSlider.setVisibility(View.GONE);
            }

            // NotifEnabled checkbox state

            if (viewModel.isOngoingNotifVisible()){
                if (viewModel.isOngoingNotifIndicatorOn()){
                    ongoingNotifIndicator.setImageDrawable(activityContext.getResources().getDrawable(R.drawable.ic_notif_enabled_tilted_icon));
                } else {
                    ongoingNotifIndicator.setImageDrawable(activityContext.getResources().getDrawable(R.drawable.notif_disabled_icon));
                }
                ongoingNotifIndicator.setVisibility(View.VISIBLE);
            } else {
                ongoingNotifIndicator.setVisibility(View.INVISIBLE);
            }


            actionSlider.setOnSlideCompleteListener(slideToActView -> {
                if (mItemListener.isFragmentVisible() && this.presenter!=null){
                    viewModel.onSlideCompleted(this.presenter);
                    mItemListener.vibrateOnActionButtonClick();
                    //actionSlider.setLocked(true);
                    actionSlider.setEnabled(false);
                    Log.d("umods_frag","INDEX:" + i
                            + "VM_HashCode: " + viewModel.hashCode()
                            + "\nVM: " + viewModel.toString());
                }
            });


            return rowView;
        }

        void removeItem(String uuid) {
            int index = 0;
            for (UModViewModel viewModel : this.mViewModelsList){
                if (viewModel.getuModUUID().equals(uuid)){
                    break;
                }
                index++;
            }
            this.mViewModelsList.remove(index);
        }
    }

    public interface UModItemListener {

        //void onUModClick(UMod clickedUMod);

        //void onCompleteUModClick(UMod completedUMod);

        //void onActivateUModClick(UMod activatedUMod);

        //void onActionButtonClick(UMod actedUMod);

        //void onRequestAccess(UMod requestedUMod);

        boolean isFragmentVisible();

        void vibrateOnActionButtonClick();
    }


    public enum UModViewModelColors{
        TRIGGER_SLIDER_BACKGROUND {
            @Override
            public int asActualResource() {
                return R.color.trigger_slider_background;
            }
        },
        TRIGGER_SLIDER_TEXT{
            @Override
            public int asActualResource() {
                return R.color.trigger_slider_text;
            }
        },
        OFFLINE_TAG {
            @Override
            public int asActualResource() {
                return R.drawable.umods_tags__offline_background;
            }
        },
        ONLINE_TAG {
            @Override
            public int asActualResource() {
                return R.drawable.umods_tags__online_background;
            }
        },
        ACCESS_REQUEST_SLIDER_BACKGROUND{
            @Override
            public int asActualResource() {
                return R.color.request_access_slider_background;
            }
        },
        ACCESS_REQUEST_SLIDER_TEXT{
            @Override
            public int asActualResource() {
                return R.color.request_access_slider_text;
            }
        }, OPEN_GATE_TAG {
            @Override
            public int asActualResource() {
                return R.drawable.umods_tags__open_gate_background;
            }
        }, OPEN_GATE_TAG_TEXT {
            @Override
            public int asActualResource() {
                return R.color.request_access_slider_text;
            }
        }, CLOSED_GATE_TAG {
            @Override
            public int asActualResource() {
                return R.drawable.umods_tags__closed_gate_background;
            }
        }, CLOSED_GATE_TAG_TEXT {
            @Override
            public int asActualResource() {
                return R.color.white;
            }
        }, UNKNOWN_GATE_STATUS_TAG {
            @Override
            public int asActualResource() {
                return R.drawable.umods_tags__unknown_gate_status_background;
            }
        }, UNKNOWN_GATE_STATUS_TAG_TEXT {
            @Override
            public int asActualResource() {
                return R.color.offline_light_gray;
            }
        }, ONLINE_TAG_TEXT {
            @Override
            public int asActualResource() {
                return R.color.white;
            }
        }, OFFLINE_TAG_TEXT {
            @Override
            public int asActualResource() {
                return R.color.offline_light_gray;
            }
        };
        public abstract int asActualResource();
    }

}
