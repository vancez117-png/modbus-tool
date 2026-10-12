package com.vance.modbusfloat;

public enum RegisterInputMode {
    UNSIGNED("Unsigned UInt16"),
    SIGNED("Signed Int16"),
    HEX("Hex"),
    BINARY("Binary");

    private final String label;

    RegisterInputMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
