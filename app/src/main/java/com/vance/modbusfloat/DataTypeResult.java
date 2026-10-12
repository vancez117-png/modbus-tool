package com.vance.modbusfloat;

public final class DataTypeResult {
    public final String order;
    public final String dataType;
    public final String value;
    public final String hex;
    public final String binary;
    public final String registers;

    public DataTypeResult(String order, String dataType, String value, String hex, String binary, String registers) {
        this.order = order;
        this.dataType = dataType;
        this.value = value;
        this.hex = hex;
        this.binary = binary;
        this.registers = registers;
    }
}
