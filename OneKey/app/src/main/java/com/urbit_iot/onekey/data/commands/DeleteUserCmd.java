package com.urbit_iot.onekey.data.commands;

/**
 * Created by andresteve07 on 8/11/17.
 */

public class DeleteUserCmd extends Command {
    public static class Request extends CommandRequest{

        public Request(int commandID, String userID, String appUUID) {
            super(CommandCodes.BORRAR_USUARIO.getCommandCode(), commandID, userID, appUUID);
        }
    }

    public static class Response extends CommandResponse{

        public Response(int commandID, String result) {
            super(CommandCodes.BORRAR_USUARIO.getCommandCode(), commandID, result);
        }
    }
}
