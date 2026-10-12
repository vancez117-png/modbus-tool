package com.vance.modbusfloat;

public final class ConversionResult {
    public final ByteOrderMode order;
    public final String binary;
    public final String hex;
    public final int register1;
    public final int register2;
    public final float floatValue;

    public ConversionResult(ByteOrderMode order, String binary, String hex, int register1, int register2, float floatValue) {
        this.order = order;
        this.binary = binary;
        this.hex = hex;
        this.register1 = register1;
        this.register2 = register2;
        this.floatValue = floatValue;
    }
}
