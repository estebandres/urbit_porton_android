package com.urbit_iot.onekey.data.commands;

import com.google.gson.annotations.SerializedName;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class Command {
    public static class CommandRequest {
        @SerializedName("cmd")
        private int commandCode;
        @SerializedName("cmd_id")
        private int commandID;
        @SerializedName("user")
        private String userID;
        @SerializedName("pass")
        private String appUUID;

        public CommandRequest(int commandCode, int commandID, String userID, String appUUID) {
            this.commandCode = commandCode;
            this.commandID = commandID;
            this.userID = userID;
            this.appUUID = appUUID;
        }

        public int getCommandCode() {
            return commandCode;
        }

        public void setCommandCode(int commandCode) {
            this.commandCode = commandCode;
        }

        public int getCommandID() {
            return commandID;
        }

        public void setCommandID(int commandID) {
            this.commandID = commandID;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public String getAppUUID() {
            return appUUID;
        }

        public void setAppUUID(String appUUID) {
            this.appUUID = appUUID;
        }
    }
    public static class CommandResponse{
        @SerializedName("cmd")
        private int commandCode;
        @SerializedName("cmd_id")
        private int commandID;
        @SerializedName("result")
        private String result;
        /*
        @SerializedName("err_code")
        private String errorCode;
        @SerializedName("error_detail")
        private String errorDetail;
        */

        public CommandResponse(int commandCode, int commandID, String result) {
            this.commandCode = commandCode;
            this.commandID = commandID;
            this.result = result;
        }

        public int getCommandCode() {
            return commandCode;
        }

        public void setCommandCode(int commandCode) {
            this.commandCode = commandCode;
        }

        public int getCommandID() {
            return commandID;
        }

        public void setCommandID(int commandID) {
            this.commandID = commandID;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }
}
