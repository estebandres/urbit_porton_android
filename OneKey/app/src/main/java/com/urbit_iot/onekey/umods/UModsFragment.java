package com.urbit_iot.onekey.umods;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.urbit_iot.onekey.R;
import com.urbit_iot.onekey.umodconfig.UModConfigActivity;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.umodconfig.UModConfigFragment;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Display a grid of {@link UMod}s. User can choose to view all, active or completed tasks.
 */
public class UModsFragment extends Fragment implements UModsContract.View {

    @NonNull
    private static final int REQUEST_UMOD_CONFIG = 1;

    private UModsContract.Presenter mPresenter;

    private UModsAdapter mListAdapter;

    private View mNoTasksView;

    private ImageView mNoTaskIcon;

    private TextView mNoTaskMainView;

    private TextView mNoTaskAddView;

    private LinearLayout mTasksView;

    private TextView mFilteringLabelView;

    public UModsFragment() {
        // Requires empty public constructor
    }

    public static UModsFragment newInstance() {
        return new UModsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListAdapter = new UModsAdapter(new ArrayList<UModViewModel>(0), mItemListener);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPresenter.result(requestCode, resultCode);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.umods_frag, container, false);

        // Set up tasks view
        ListView listView = (ListView) root.findViewById(R.id.umods_list);
        listView.setAdapter(mListAdapter);
        mFilteringLabelView = (TextView) root.findViewById(R.id.umods_filtering_label);
        mTasksView = (LinearLayout) root.findViewById(R.id.umods_linear_layout);

        // Set up  no tasks view
        mNoTasksView = root.findViewById(R.id.no_umods);
        mNoTaskIcon = (ImageView) root.findViewById(R.id.no_umods_icon);
        mNoTaskMainView = (TextView) root.findViewById(R.id.no_umods_main);
        mNoTaskAddView = (TextView) root.findViewById(R.id.no_umods_add_some);
        mNoTaskAddView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddUMod();
            }
        });

        // Set up floating action button
        FloatingActionButton fab =
                (FloatingActionButton) getActivity().findViewById(R.id.fab_add_umod);

        fab.setImageResource(R.drawable.ic_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.addNewUMod();
            }
        });

        // Set up progress indicator
        final ScrollChildSwipeRefreshLayout swipeRefreshLayout =
                (ScrollChildSwipeRefreshLayout) root.findViewById(R.id.umods_refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getActivity(), R.color.colorPrimary),
                ContextCompat.getColor(getActivity(), R.color.colorAccent),
                ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark)
        );
        // Set the scrolling view in the custom SwipeRefreshLayout.
        swipeRefreshLayout.setScrollUpChild(listView);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPresenter.loadUMods(true);
            }
        });

        setHasOptionsMenu(true);

        return root;
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
        showMessage(getString(R.string.open_close_success_message));
    }

    @Override
    public void showOpenCloseFail() {
        showMessage(getString(R.string.open_close_fail_message));
    }


    @Override
    public void setLoadingIndicator(final boolean active) {

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
    }

    @Override
    public void showUMods(List<UModViewModel> uModViewModels) {
        mListAdapter.replaceData(uModViewModels);

        mTasksView.setVisibility(View.VISIBLE);
        mNoTasksView.setVisibility(View.GONE);
    }

    public void showUMod(UModViewModel uModViewModel){
        if(mNoTaskAddView.getVisibility() == View.VISIBLE){
            mNoTasksView.setVisibility(View.GONE);
        }
        mListAdapter.addItem(uModViewModel);
    }

    @Override
    public void appendUMod(UModViewModel uMod) {
        mListAdapter.addItem(uMod);
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
                R.drawable.ic_assignment_turned_in_24dp,
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

    @Override
    public void showSuccessfullySavedMessage() {
        showMessage(getString(R.string.successfully_saved_umod_message));
    }

    private void showNoTasksViews(String mainText, int iconRes, boolean showAddView) {
        mTasksView.setVisibility(View.GONE);
        mNoTasksView.setVisibility(View.VISIBLE);

        mNoTaskMainView.setText(mainText);
        mNoTaskIcon.setImageDrawable(getResources().getDrawable(iconRes));
        mNoTaskAddView.setVisibility(showAddView ? View.VISIBLE : View.GONE);
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
    public void showAlienUModsCleared() {
        showMessage(getString(R.string.completed_tasks_cleared));
    }

    @Override
    public void showLoadingUModsError() {
        showMessage(getString(R.string.loading_tasks_error));
    }

    private void showMessage(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean isNotificationDisabled() {
        return isAdded();
    }

    /**
     * Listener for clicks on tasks in the ListView.
     */
    UModItemListener mItemListener = new UModItemListener() {
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
            mPresenter.triggerUMod(actedUMod);
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

        public UModsAdapter(List<UModViewModel> uMods, UModItemListener itemListener) {
            setList(uMods);
            mItemListener = itemListener;
        }

        public void replaceData(List<UModViewModel> uMods) {
            setList(uMods);
            notifyDataSetChanged();
        }

        private void setList(List<UModViewModel> uMods) {
            mViewModelsList = checkNotNull(uMods);
        }

        public void addItem(UModViewModel viewModel){
            //refreshList();
            if( ! mViewModelsList.contains(viewModel)){
                mViewModelsList.add(viewModel);
                notifyDataSetChanged();
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
        public View getView(int i, View view, ViewGroup viewGroup) {
            View rowView = view;
            if (rowView == null) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                rowView = inflater.inflate(R.layout.umod_item, viewGroup, false);
            }

            final UModViewModel viewModel = getItem(i);
            //TODO if R.id.title is renamed then the project doesn't build. Why??
            TextView mainText = (TextView) rowView.findViewById(R.id.title);

            mainText.setText(viewModel.getItemMainText());

            TextView lowerText = (TextView) rowView.findViewById(R.id.lower_text);

            lowerText.setText(viewModel.getItemLowerText());

            final CheckBox notifEnCB = (CheckBox) rowView.findViewById(R.id.umod_notif_enabler);

            Button actionButton = (Button) rowView.findViewById(R.id.umod_action_button);

            if(viewModel.isButtonVisible()){
                actionButton.setVisibility(View.VISIBLE);
            } else {
                actionButton.setVisibility(View.INVISIBLE);
            }
            actionButton.setText(viewModel.getButtonText());

            // NotifEnabled checkbox state
            notifEnCB.setChecked(viewModel.isCheckboxChecked());
            if (viewModel.isCheckboxVisible()){
                notifEnCB.setVisibility(View.VISIBLE);
            } else {
                notifEnCB.setVisibility(View.INVISIBLE);
            }


            /* NO SE QUE HACE ESTO??
            if (uMod.isOngoingNotificationEnabled()) {
                rowView.setBackgroundDrawable(viewGroup.getContext()
                        .getResources().getDrawable(R.drawable.list_completed_touch_feedback));
            } else {
                rowView.setBackgroundDrawable(viewGroup.getContext()
                        .getResources().getDrawable(R.drawable.touch_feedback));
            }
            */

            notifEnCB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewModel.onCheckBoxClicked(notifEnCB.isChecked());
                }
            });

            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewModel.onButtonClicked();
                }
            });

            if(viewModel.isItemOnClickListenerEnabled()){
                rowView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("STEVE_listit",viewModel.toString());
                        viewModel.onItemClicked();
                    }
                });
            }

            return rowView;
        }
    }

    public interface UModItemListener {

        //void onUModClick(UMod clickedUMod);

        //void onCompleteUModClick(UMod completedUMod);

        //void onActivateUModClick(UMod activatedUMod);

        //void onActionButtonClick(UMod actedUMod);

        //void onRequestAccess(UMod requestedUMod);
    }

}
