package com.urbit_iot.onekey.appuser;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.urbit_iot.onekey.R;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Created by andresteve07 on 11/14/17.
 */

public class AppUserFragment extends Fragment implements AppUserContract.View {

    private AppUserContract.Presenter mPresenter;
    private TextView mPhoneNumber;

    public AppUserFragment(){
        //Requires empty public constructor
    }

    public static AppUserFragment newInstance(){
        return new AppUserFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.subscribe();
    }

    @Override
    public void setPresenter(AppUserContract.Presenter presenter) {
        mPresenter = checkNotNull(presenter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FloatingActionButton fab =
                (FloatingActionButton) getActivity().findViewById(R.id.fab_edit_app_user_done);
        fab.setImageResource(R.drawable.ic_done);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUserViewModel appUser = new AppUserViewModel(mPhoneNumber.getText().toString());
                mPresenter.saveAppUser(appUser);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.appuser_frag, container, false);
        mPhoneNumber = (TextView) root.findViewById(R.id.app_user_phone_number);

        setHasOptionsMenu(true);
        setRetainInstance(true);
        return root;
    }

    @Override
    public void showUModsList() {
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    @Override
    public void setAppUserFirstName(String firstName) {

    }

    @Override
    public void setAppUserLastName(String lastName) {

    }

    @Override
    public void setAppUserAlias(String alias) {

    }

    @Override
    public void setAppUserPhoneNumber(String phoneNumber) {

    }


    @Override
    public boolean isActive() {
        return isAdded();
    }
}
