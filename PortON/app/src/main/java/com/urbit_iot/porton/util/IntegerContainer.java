package com.urbit_iot.porton.util;

/**
 * Created by steve-urbit on 10/02/18.
 */

public class IntegerContainer {
    private int value;

    public IntegerContainer(int value){
        this.value = value;
    }

    public int plusOne(){
        return value++;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
