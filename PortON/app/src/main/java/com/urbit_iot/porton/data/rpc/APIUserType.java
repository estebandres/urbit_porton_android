package com.urbit_iot.porton.data.rpc;

import com.urbit_iot.porton.data.UModUser;

/**
 * Created by andresteve07 on 24/02/18.
 */

public enum APIUserType {
    Admin {
        @Override
        public UModUser.Level asUModUserLevel() {
            return UModUser.Level.ADMINISTRATOR;
        }
    },
    Guest {
        @Override
        public UModUser.Level asUModUserLevel() {
            return UModUser.Level.PENDING;
        }
    },
    User {
        @Override
        public UModUser.Level asUModUserLevel() {
            return UModUser.Level.AUTHORIZED;
        }
    },
    NotUser {
        @Override
        public UModUser.Level asUModUserLevel() {
            return UModUser.Level.UNAUTHORIZED;
        }
    };

    public abstract UModUser.Level asUModUserLevel();
}
