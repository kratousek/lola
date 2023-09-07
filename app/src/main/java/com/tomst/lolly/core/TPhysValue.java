package com.tomst.lolly.core;

public enum TPhysValue {
    vT1(1),vT2(2),vT3(3),vHum(4),vMvs(5),vAD(6),vMicro(7);

    private int value;
    private TPhysValue(int value) {
        this.value = value;
    }

    static TPhysValue fromValue(int value) {
        for (TPhysValue my : TPhysValue.values()) {
            if (my.value == value) {
                return my;
            }
        }
        return null;
    }

    public String valToString(TPhysValue val){
        String s = "";
        switch (val){
            case vT1: s = "T1"; break;
            case vT2: s ="T2"; break;
            case vT3: s = "T3"; break;
            case vHum: s="Hum"; break;
            case vAD: s = "AD"; break;
            case vMicro: s = "Micro"; break;
        }
        return (s);
    }

    int value() {
        return value;
    }
}

