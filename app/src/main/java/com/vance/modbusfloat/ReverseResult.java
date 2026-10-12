package com.vance.modbusfloat;

public final class ReverseResult {
    public final String order;
    public final String dataType;
    public final String value;
    public final String hex;
    public final String binary;
    public final String register1;
    public final String register2;
    public final String register3;
    public final String register4;
    public final String registers;

    public ReverseResult(String order, String dataType, String value, String hex, String binary, String register1, String register2, String register3, String register4) {
        this(order, dataType, value, hex, binary, register1, register2, register3, register4, "");
    }

    public ReverseResult(String order, String dataType, String value, String hex, String binary, String register1, String register2, String register3, String register4, String registers) {
        this.order = order;
        this.dataType = dataType;
        this.value = value;
        this.hex = hex;
        this.binary = binary;
        this.register1 = register1;
        this.register2 = register2;
        this.register3 = register3;
        this.register4 = register4;
        this.registers = registers;
    }
}
