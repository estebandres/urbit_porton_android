package com.urbit_iot.onekey.data.rpc;

/**
 * Created by andresteve07 on 8/11/17.
 */

public enum CommandCodes {
    CREAR_ADMIN(1),
    BORRAR_ADMIN(2),
    CREAR_USUARIO(3),
    BORRAR_USUARIO(4),
    OBTENER_USUARIOS(5),
    CONECTAR(6),
    ACCIONAR(7),
    ACTUALIZAR(8),
    GET_VERSION(90),
    I_AM_CONECTED(91),
    FACTORY_RESET(99),
    GET_USER(92);

    private final Integer commandCode;
    CommandCodes(Integer commandCode){
        this.commandCode = commandCode;
    }
    public Integer getCommandCode(){
        return this.commandCode;
    }
}
