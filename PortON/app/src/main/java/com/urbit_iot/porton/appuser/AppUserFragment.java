package com.urbit_iot.porton.appuser;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.i18n.phonenumbers.*;
//import com.google.i18n.phonenumbers.NumberParseException;
//import com.google.i18n.phonenumbers.PhoneNumberUtil;
//import com.google.i18n.phonenumbers.Phonenumber;
import com.urbit_iot.porton.R;
import com.urbit_iot.porton.umods.UModsActivity;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Created by andresteve07 on 11/14/17.
 */

public class AppUserFragment extends Fragment implements AppUserContract.View {

    private AppUserContract.Presenter mPresenter;
    private EditText phoneNumberTextView;
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

        FloatingActionButton fab = getActivity().findViewById(R.id.fab_edit_app_user_done);
        fab.setImageResource(R.drawable.ic_done);
        fab.setOnClickListener(v -> {
            String phoneNumberTypedByUser = phoneNumberTextView.getText().toString();
            String numberE164Formatted = getPhoneNumberE164Formatted(phoneNumberTypedByUser);
            if(numberE164Formatted != null){
                AppUserViewModel appUser = new AppUserViewModel(numberE164Formatted);
                mPresenter.saveAppUser(appUser);
            } else {
                phoneNumberTextView.setError(getString(R.string.phone_format_error_message));
            }
        });
    }

    private String getPhoneNumberE164Formatted(String phoneNumberStr){
        Phonenumber.PhoneNumber argentinianNumberProto;
        try{
            argentinianNumberProto = phoneUtil.parse(phoneNumberStr, "AR");
            if (phoneUtil.isValidNumber(argentinianNumberProto)){
                String nationalNumberWithNine = null;
                if(phoneUtil.getNumberType(argentinianNumberProto)
                        == PhoneNumberUtil.PhoneNumberType.MOBILE){
                    Log.d("APPUSR_FRAG", "MOBILE!");
                    nationalNumberWithNine = "9" + this.phoneUtil.format(argentinianNumberProto,
                            PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                    Log.d("APPUSR_FRAG", "MOBILE NATIONAL FORMAT + 9: " + nationalNumberWithNine);
                    argentinianNumberProto = phoneUtil.parse(nationalNumberWithNine,"AR");
                } else {
                    phoneNumberStr = "9" + phoneNumberStr.replaceFirst("^0","");
                    argentinianNumberProto = phoneUtil.parse(phoneNumberStr, "AR");
                    nationalNumberWithNine = "9" + this.phoneUtil.format(argentinianNumberProto,
                            PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                    Log.d("APPUSR_FRAG", "MOBILIZED NATIONAL FORMAT + 9: " + nationalNumberWithNine);
                    argentinianNumberProto = phoneUtil.parse(nationalNumberWithNine,"AR");
                }
            } else {
                return null;
            }
        }
        catch (NumberParseException e) {
            Log.e("APPUSR_FRAG","ERROR while parsing number: "
                    + e.getClass().getSimpleName()
                    + " was thrown: " + e.getMessage());
            return null;
        }
        return this.phoneUtil.format(argentinianNumberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    //TODO better exception handling
    private boolean phoneNumberIsValid(String phoneNumberStr){
        Phonenumber.PhoneNumber argentinianNumberProto;
        try {
            argentinianNumberProto = phoneUtil.parse(phoneNumberStr, "AR");
        } catch (NumberParseException e) {
            Log.e("appusr_frag","ERROR while parsing number: "
                    + e.getClass().getSimpleName()
                    + " was thrown: " + e.getMessage());
            return false;
        }
        return phoneUtil.isValidNumber(argentinianNumberProto);
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.appuser_frag, container, false);
        phoneNumberTextView = root.findViewById(R.id.app_user_phone_number);
        phoneNumberTextView.setSelection(phoneNumberTextView.getText().length());

        setHasOptionsMenu(true);
        setRetainInstance(true);
        return root;
    }

    @Override
    public void showUModsList(String userName, String appUUIDHash) {
        //TODO: Should I use some Intent FLAGS?
        Intent goToUModsActivity = new Intent(getActivity(), UModsActivity.class);
        goToUModsActivity.putExtra(UModsActivity.APP_USER_NAME,userName);
        goToUModsActivity.putExtra(UModsActivity.APP_UUID,appUUIDHash);
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
