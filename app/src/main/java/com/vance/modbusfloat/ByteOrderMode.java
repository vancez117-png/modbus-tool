package com.vance.modbusfloat;

public enum ByteOrderMode {
    ABCD("ABCD", new int[]{0, 1, 2, 3}),
    CDAB("CDAB", new int[]{2, 3, 0, 1}),
    BADC("BADC", new int[]{1, 0, 3, 2}),
    DCBA("DCBA", new int[]{3, 2, 1, 0});

    private final String label;
    private final int[] map;

    ByteOrderMode(String label, int[] map) {
        this.label = label;
        this.map = map;
    }

    public String label() {
        return label;
    }

    public int[] map() {
        return map;
    }
}
