package com.vance.modbusfloat;

public enum DataTypeMode {
    INT16("Int16"),
    UINT16("UInt16"),
    INT32("Int32"),
    UINT32("UInt32"),
    FLOAT32("Float32 IEEE754"),
    FLOAT64("Float64 IEEE754"),
    ASCII("ASCII"),
    BCD("Packed BCD");

    private final String label;

    DataTypeMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static DataTypeMode fromLabel(String label) {
        for (DataTypeMode mode : values()) {
            if (mode.label.equals(label)) {
                return mode;
            }
        }
        if ("BCD".equals(label)) {
            return BCD;
        }
        return FLOAT32;
    }
}
