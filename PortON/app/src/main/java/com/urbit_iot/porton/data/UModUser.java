package com.urbit_iot.porton.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.urbit_iot.porton.data.rpc.APIUserType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by andresteve07 on 8/21/17.
 */

public class UModUser {

    public enum Level {
        PENDING(2) {
            @Override
            public APIUserType asAPIUserType() {
                return APIUserType.Guest;
            }
        },
        AUTHORIZED(1) {
            @Override
            public APIUserType asAPIUserType() {
                return APIUserType.User;
            }
        },
        INVITED(8) {
            @Override
            public APIUserType asAPIUserType() {
                return APIUserType.User;
            }
        },
        UNAUTHORIZED(0) {
            @Override
            public APIUserType asAPIUserType() {
                return APIUserType.NotUser;
            }
        },
        TEMPORAL(3) {
            @Override
            public APIUserType asAPIUserType() {
                return null;
            }
        },
        WATCHER(4) {
            @Override
            public APIUserType asAPIUserType() {
                return null;
            }
        },
        PRE_APPROVED(5) {
            @Override
            public APIUserType asAPIUserType() {
                return null;
            }
        },
        ADMINISTRATOR(6){
            @Override
            public APIUserType asAPIUserType() {
                return APIUserType.Admin;
            }
        },
        UNKNOWN(7) {
            @Override
            public APIUserType asAPIUserType() {
                return null;
            }
        };
        private final Integer statusID;
        private static Map<Integer, Level> map = new HashMap<>();

        static {
            for (Level stateEnum : Level.values()) {
                map.put(stateEnum.statusID, stateEnum);
            }
        }

        Level(Integer statusID) {
            this.statusID = statusID;
        }

        public Integer getStatusID() {
            return this.statusID;
        }

        public static Level from(int value) {
            return map.get(value);
        }

        public abstract APIUserType asAPIUserType();
    }

    @NonNull
    private String uModUUID;
    @NonNull
    private String phoneNumber;
    @Nullable
    private String userAlias;
    @NonNull
    private Level userStatus;

    public UModUser(@NonNull String uModUUID, @NonNull String phoneNumber, @NonNull Level userStatus) {
        this.uModUUID = uModUUID;
        this.phoneNumber = phoneNumber;
        this.userStatus = userStatus;
    }

    @NonNull
    public String getuModUUID() {
        return uModUUID;
    }

    public void setuModUUID(@NonNull String uModUUID) {
        this.uModUUID = uModUUID;
    }

    @NonNull
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(@NonNull String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Nullable
    public String getUserAlias() {
        return userAlias;
    }

    public void setUserAlias(@Nullable String userAlias) {
        this.userAlias = userAlias;
    }

    @NonNull
    public Level getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(@NonNull Level userStatus) {
        this.userStatus = userStatus;
    }

    public String getNameForList(){
        return this.phoneNumber;
    }

    public boolean isAdmin(){
        return this.userStatus == Level.ADMINISTRATOR;
    }

    @Override
    public String toString() {
        return "[" + getPhoneNumber() + " - " + getUserAlias() + "]";
    }
}
