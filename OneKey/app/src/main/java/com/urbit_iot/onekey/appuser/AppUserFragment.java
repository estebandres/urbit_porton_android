package com.urbit_iot.onekey.appuser;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.urbit_iot.onekey.R;
import com.urbit_iot.onekey.umods.UModsActivity;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Created by andresteve07 on 11/14/17.
 */

public class AppUserFragment extends Fragment implements AppUserContract.View {

    private AppUserContract.Presenter mPresenter;
    private TextView phoneNumberTextView;
    AsYouTypeFormatter phoneNumberFormatter;
    PhoneNumberUtil phoneUtil;


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

        this.phoneUtil = PhoneNumberUtil.getInstance();
        this.phoneNumberFormatter = phoneUtil.getAsYouTypeFormatter("AR");

        FloatingActionButton fab =
                (FloatingActionButton) getActivity().findViewById(R.id.fab_edit_app_user_done);
        fab.setImageResource(R.drawable.ic_done);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(phoneNumberIsValid(phoneNumberTextView.getText().toString())){
                    AppUserViewModel appUser = new AppUserViewModel(getPhoneNumberE164Formatted(phoneNumberTextView.getText().toString()));
                    mPresenter.saveAppUser(appUser);
                } else {
                    phoneNumberTextView.setError("Formato incorrecto. Ingrese el numero con caracteristica y sin 15.");
                }
            }
        });
    }

    //TODO better exception handling
    private String getPhoneNumberE164Formatted(String phoneNumberStr){
        Phonenumber.PhoneNumber argentinianNumberProto;
        try{
            argentinianNumberProto = phoneUtil.parse(phoneNumberStr, "AR");
        }
        catch (NumberParseException e) {
            Log.d("appusr_frag","NumberParseException was thrown: " + e.toString());
            return null;
        }
        return this.phoneUtil.format(argentinianNumberProto, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);

    }

    //TODO better exception handling
    private boolean phoneNumberIsValid(String phoneNumberStr){
        boolean isInvalid = false;
        Phonenumber.PhoneNumber argentinianNumberProto;
        try {
            argentinianNumberProto = phoneUtil.parse(phoneNumberStr, "AR");
        } catch (NumberParseException e) {
            Log.d("appusr_frag","NumberParseException was thrown: " + e.toString());
            return isInvalid;
        }
        return phoneUtil.isValidNumber(argentinianNumberProto);
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.appuser_frag, container, false);
        phoneNumberTextView = (TextView) root.findViewById(R.id.app_user_phone_number);

        setHasOptionsMenu(true);
        setRetainInstance(true);
        return root;
    }

    @Override
    public void showUModsList(String userPhoneNumber, String appUUIDHash) {
        //TODO: Should I use some Intent FLAGS?
        Intent goToUModsActivity = new Intent(getActivity(), UModsActivity.class);
        goToUModsActivity.putExtra(UModsActivity.APP_USER_PHONE_NUMBER,userPhoneNumber);
        goToUModsActivity.putExtra(UModsActivity.APP_UUID_HASH,appUUIDHash);
        startActivity(goToUModsActivity);
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
