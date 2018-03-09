package com.urbit_iot.onekey.usersxumod;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.urbit_iot.onekey.R;
import com.urbit_iot.onekey.umodconfig.UModConfigActivity;
import com.urbit_iot.onekey.data.UMod;
import com.urbit_iot.onekey.umods.ScrollChildSwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Display a grid of {@link UMod}s. User can choose to view all, active or completed tasks.
 */
public class UModUsersFragment extends Fragment implements UModUsersContract.View {

    private UModUsersContract.Presenter mPresenter;

    private UModUsersAdapter mListAdapter;

    private View mNoUsersView;

    private ImageView mNoUsersIcon;

    private TextView mNoUsersMainView;

    private TextView mNoUsersAddView;

    private LinearLayout mUsersView;

    private TextView mFilteringLabelView;

    private ProgressBar mProgressBar;

    private PhoneNumberUtil phoneUtil;

    public UModUsersFragment() {
        // Requires empty public constructor
    }

    public static UModUsersFragment newInstance() {
        return new UModUsersFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListAdapter = new UModUsersAdapter(new ArrayList<UModUserViewModel>(0), mItemListener);
        this.phoneUtil = PhoneNumberUtil.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.subscribe();
    }

    @Override
    public void setPresenter(@NonNull UModUsersContract.Presenter presenter) {
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
        View root = inflater.inflate(R.layout.umod_users_frag, container, false);

        mProgressBar = (ProgressBar) root.findViewById(R.id.umod_users_load_bar);
        mProgressBar.setVisibility(View.INVISIBLE);
        // Set up tasks view
        ListView listView = (ListView) root.findViewById(R.id.umod_users_list);
        listView.setAdapter(mListAdapter);
        mFilteringLabelView = (TextView) root.findViewById(R.id.umod_users_filtering_label);
        mUsersView = (LinearLayout) root.findViewById(R.id.umod_users_ll);

        // Set up  no users view
        mNoUsersView = root.findViewById(R.id.no_umod_users);
        mNoUsersIcon = (ImageView) root.findViewById(R.id.no_umod_users_icon);
        mNoUsersMainView = (TextView) root.findViewById(R.id.no_umod_users_main);
        mNoUsersAddView = (TextView) root.findViewById(R.id.no_umod_users_add);
        mNoUsersAddView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddUModUser();
            }
        });

        // Set up floating action button
        FloatingActionButton fab =
                (FloatingActionButton) getActivity().findViewById(R.id.fab_add_umod_user);

        fab.setImageResource(R.drawable.ic_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.addNewUModUser();
            }
        });

        // Set up progress indicator
        final ScrollChildSwipeRefreshLayout swipeRefreshLayout =
                (ScrollChildSwipeRefreshLayout) root.findViewById(R.id.umod_users_refresh_layout);
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
                mPresenter.loadUModUsers(false);
            }
        });

        setHasOptionsMenu(true);

        return root;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.umod_users_menu_clear:
                mPresenter.clearAllPendingUsers();
                break;
            case R.id.umod_users_menu_filter:
                showFilteringPopUpMenu();
                break;
            case R.id.umod_users_menu_refresh:
                mPresenter.loadUModUsers(true);
                break;
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.umod_users_fragment_menu, menu);
    }

    @Override
    public void showFilteringPopUpMenu() {
        PopupMenu popup = new PopupMenu(getContext(), getActivity().findViewById(R.id.umod_users_menu_filter));
        popup.getMenuInflater().inflate(R.menu.filter_umod_users, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.umod_users_not_admins:
                        mPresenter.setFiltering(UModUsersFilterType.NOT_ADMINS);
                        break;
                    case R.id.umod_users_admins:
                        mPresenter.setFiltering(UModUsersFilterType.ADMINS);
                        break;
                    default:
                        mPresenter.setFiltering(UModUsersFilterType.ALL_UMOD_USERS);
                        break;
                }
                mPresenter.loadUModUsers(false);
                return true;
            }
        });

        popup.show();
    }

    @Override
    public void showUserDeletionSuccessMessage() {
        showMessage(getString(R.string.delete_user_success_message));
    }

    @Override
    public void showUserDeletionFailMessage() {
        showMessage(getString(R.string.delete_user_fail_message));
    }

    @Override
    public void showUserApprovalSuccessMessage() {
        showMessage(getString(R.string.authorize_user_success_message));
    }

    @Override
    public void showUserApprovalFailMessage() {
        showMessage(getString(R.string.authorize_user_fail_message));
    }

    @Override
    public void showUserLevelUpdateFailMessage(boolean toAdmin) {
        if (toAdmin){
            showMessage(getString(R.string.upgrade_user_type_fail_message));
        } else {
            showMessage(getString(R.string.downgrade_user_type_fail_message));
        }
    }

    @Override
    public void showUserLevelUpdateSuccessMessage(boolean toAdmin) {
        if (toAdmin){
            showMessage(getString(R.string.upgrade_user_type_success_message));
        } else {
            showMessage(getString(R.string.downgrade_user_type_success_message));
        }
    }

    @Override
    public void setLoadingIndicator(final boolean active) {

        if (getView() == null) {
            return;
        }
        final SwipeRefreshLayout srl =
                (SwipeRefreshLayout) getView().findViewById(R.id.umod_users_refresh_layout);

        // Make sure setRefreshing() is called after the layout is done with everything else.
        srl.post(new Runnable() {
            @Override
            public void run() {
                srl.setRefreshing(active);
            }
        });
    }

    @Override
    public void showUModUsers(List<UModUserViewModel> uModUsers) {
        mListAdapter.replaceData(uModUsers);

        mUsersView.setVisibility(View.VISIBLE);
        mNoUsersView.setVisibility(View.GONE);
    }

    @Override
    public void showNoActiveTasks() {
        showNoTasksViews(
                getResources().getString(R.string.no_umods_active),
                R.drawable.ic_check_circle_24dp,
                false
        );
    }

    @Override
    public void showNoUModUsers() {
        showNoTasksViews(
                getResources().getString(R.string.no_umdos_all),
                R.drawable.ic_assignment_turned_in_24dp,
                false
        );
    }

    @Override
    public void showNoCompletedTasks() {
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
        mUsersView.setVisibility(View.GONE);
        mNoUsersView.setVisibility(View.VISIBLE);

        mNoUsersMainView.setText(mainText);
        mNoUsersIcon.setImageDrawable(getResources().getDrawable(iconRes));
        mNoUsersAddView.setVisibility(showAddView ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showNotAdminsFilterLabel() {
        mFilteringLabelView.setText(getResources().getString(R.string.umod_users_label_not_admins));
    }

    @Override
    public void showAdminsFilterLabel() {
        mFilteringLabelView.setText(getResources().getString(R.string.umod_users_label_admins));
    }

    @Override
    public void showAllFilterLabel() {
        mFilteringLabelView.setText(getResources().getString(R.string.umod_users_label_all));
    }

    @Override
    public void showAddUModUser() {
        Intent intent = new Intent(getContext(), UModConfigActivity.class);
        startActivityForResult(intent, UModConfigActivity.REQUEST_ADD_TASK);
    }


    @Override
    public void showAllPendingUModUsersCleared() {
        showMessage(getString(R.string.completed_tasks_cleared));
    }

    @Override
    public void showLoadingUModUsersError() {
        showMessage(getString(R.string.loading_tasks_error));
    }

    private void showMessage(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Listener for clicks on UModUsers in the ListView.
     */
    UModUserItemListener mItemListener = new UModUserItemListener() {
        /*
        @Override
        public void onActionButtonClick(UModUser actedUModUser) {
            mPresenter.authorizeUser(actedUModUser);
        }

        @Override
        public void onIsAdminCBClick(UModUser uModUser, boolean toAdmin) {
            mPresenter.UpDownAdminLevel(uModUser, toAdmin);
        }
        */
    };

    @Override
    public boolean isActive() {
        return isAdded();
    }

    private static class UModUsersAdapter extends BaseAdapter {

        private List<UModUserViewModel> uModUsers;
        private UModUserItemListener mItemListener;

        public UModUsersAdapter(List<UModUserViewModel> uModUsers, UModUserItemListener itemListener) {
            setList(uModUsers);
            mItemListener = itemListener;
        }

        public void replaceData(List<UModUserViewModel> uModUsers) {
            setList(uModUsers);
            notifyDataSetChanged();
        }

        private void setList(List<UModUserViewModel> uModUsers) {
            this.uModUsers = checkNotNull(uModUsers);
        }

        @Override
        public int getCount() {
            return uModUsers.size();
        }

        @Override
        public UModUserViewModel getItem(int i) {
            return uModUsers.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, final View view, ViewGroup viewGroup) {
            View rowView = view;
            if (rowView == null) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                rowView = inflater.inflate(R.layout.umod_user_item, viewGroup, false);
            }

            final UModUserViewModel viewModel = getItem(i);
            //TODO: After an admin upgrade or downgrade is performed the users checkbox state should be updated acordingly.
            final int viewIndex = i;

            TextView titleTV = (TextView) rowView.findViewById(R.id.umod_user_title);
            titleTV.setText(viewModel.getItemMainText());
            //titleTV.setText(getContactNameFromPhoneNumber(uModUser.getPhoneNumber()));

            Button actionButton = (Button) rowView.findViewById(R.id.umod_user_action_button);
            actionButton.setText(viewModel.getButtonText());

            final CheckBox isAdminCB = (CheckBox) rowView.findViewById(R.id.umod_user_is_admin_checkbox);

            if (viewModel.isCheckboxVisible()){
                isAdminCB.setVisibility(View.VISIBLE);
            } else {
                isAdminCB.setVisibility(View.INVISIBLE);
            }

            if(viewModel.isCheckboxChecked()){
                isAdminCB.setChecked(true);
            } else {
                isAdminCB.setChecked(false);
            }

            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewModel.onButtonClicked();
                }
            });
            //TODO: Potential bug, customer could play with the checkbox so many times the actual umod may hang or reboot.
            // Analyse the posibility of changing default checkbox behavior to long click!!
            isAdminCB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewModel.onCheckBoxClicked(isAdminCB.isChecked());
                }
            });

            return rowView;
        }
    }

    public interface UModUserItemListener {

        //void onActionButtonClick(UModUser actedUModUser);

        //void onIsAdminCBClick(UModUser uModUser, boolean toAdmin);
    }

    //TODO: Make this function compatible with Android 6.0 with on run time permission grant.
    //This will fail for Android >= 6.0 given the new permissions policies.
    @Override
    public String getContactNameFromPhoneNumber(String phoneNumber){
        phoneNumber = getPhoneNumberNationalFormatted(phoneNumber);
        String displayName = phoneNumber;
        Context context = this.getContext();
        Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor c = context.getContentResolver().query(lookupUri, new String[]{ContactsContract.Data.DISPLAY_NAME},null,null,null);
        try {
            if(c.moveToFirst()) {
                displayName = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                //displayName = c.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            c.close();
        }
        return displayName;
    }

    private String getPhoneNumberNationalFormatted(String phoneNumberStr){
        Phonenumber.PhoneNumber argentinianNumberProto;
        try{
            argentinianNumberProto = phoneUtil.parse(phoneNumberStr, "AR");
        }
        catch (NumberParseException e) {
            Log.d("appusr_frag","NumberParseException was thrown: " + e.toString());
            return phoneNumberStr;
        }
        return this.phoneUtil.format(argentinianNumberProto, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);

    }

    @Override
    public void showProgressBar() {
        this.mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgressBar() {
        this.mProgressBar.setVisibility(View.INVISIBLE);
    }
}
