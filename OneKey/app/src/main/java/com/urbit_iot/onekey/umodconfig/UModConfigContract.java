/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.urbit_iot.onekey.umodconfig;

import com.urbit_iot.onekey.BasePresenter;
import com.urbit_iot.onekey.BaseView;

/**
 * This specifies the contract between the view and the presenter.
 */
public interface UModConfigContract {

    interface View extends BaseView<Presenter> {

        void showEmptyUModError();

        void showUModsList();

        void setUModUUID(String uModUUID);

        void setUModIPAddress(String ipAddress);

        boolean isActive();

        void showEditUModUsers(String uModUUID);
    }

    interface Presenter extends BasePresenter {
        //TODO save umod config: is it possible to separate umod from umod config??
        void saveUMod(String title, String description);//saveUModConfig

        void populateUMod();

        void adminUModUsers();

        void getUModSystemInfo(String uModUUID);
    }
}
