package com.vance.modbusfloat;

public final class RegisterFormat {
    public final int index;
    public final int unsignedValue;
    public final short signedValue;
    public final String hex;
    public final String binary;

    public RegisterFormat(int index, int unsignedValue, short signedValue, String hex, String binary) {
        this.index = index;
        this.unsignedValue = unsignedValue;
        this.signedValue = signedValue;
        this.hex = hex;
        this.binary = binary;
    }
}
