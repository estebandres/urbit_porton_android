package com.urbit_iot.onekey.data.commands;

import com.google.gson.annotations.SerializedName;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class OpenCloseCmd extends Command {
    public static class Request extends Command.CommandRequest{

        public Request(int commandID, String userID, String appUUID) {
            super(CommandCodes.ACCIONAR.getCommandCode(), commandID, userID, appUUID);
        }
    }

    public static class Response extends Command.CommandResponse{

        public Response(int commandID, String result) {
            super(CommandCodes.ACCIONAR.getCommandCode(), commandID, result);
        }
    }
}
