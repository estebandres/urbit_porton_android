package com.urbit_iot.onekey.appuser;

import com.urbit_iot.onekey.BasePresenter;
import com.urbit_iot.onekey.BaseView;
import com.urbit_iot.onekey.appuser.AppUserContract;

/**
 * Created by andresteve07 on 11/9/17.
 */

public interface AppUserContract {
    interface View extends BaseView<AppUserContract.Presenter> {

        void showUModsList();

        void setAppUserFirstName(String firstName);

        void setAppUserLastName(String lastName);

        void setAppUserAlias(String alias);

        void setAppUserPhoneNumber(String phoneNumber);

        boolean isActive();
    }

    interface Presenter extends BasePresenter {

        void saveAppUser(AppUserViewModel appUserViewModel);

        void populateAppUserView();
    }
}
