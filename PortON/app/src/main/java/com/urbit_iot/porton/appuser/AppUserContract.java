package com.urbit_iot.porton.appuser;

import com.urbit_iot.porton.BasePresenter;
import com.urbit_iot.porton.BaseView;

/**
 * Created by andresteve07 on 11/9/17.
 */

public interface AppUserContract {
    interface View extends BaseView<AppUserContract.Presenter> {

        void showUModsList(String userPhoneNumber, String appUUIDHash);

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
