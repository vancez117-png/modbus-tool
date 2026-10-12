package com.vance.modbusfloat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RegisterByteOrder {
    public enum Operation {
        NORMAL,
        REVERSE_REGISTERS,
        SWAP_BYTES_WITHIN_REGISTERS,
        REVERSE_REGISTERS_AND_SWAP_BYTES
    }

    public static final class Rule {
        public final String label;
        public final Operation operation;
        public final int[] map;

        private Rule(String label, Operation operation, int[] map) {
            this.label = label;
            this.operation = operation;
            this.map = map;
        }
    }

    private RegisterByteOrder() {
    }

    public static List<Rule> rulesForLength(int byteLength) {
        requireRegisterAligned(byteLength);
        List<Rule> rules = new ArrayList<>();
        rules.add(rule(byteLength, Operation.NORMAL));
        if (byteLength == 2) {
            rules.add(rule(byteLength, Operation.SWAP_BYTES_WITHIN_REGISTERS));
            return rules;
        }
        rules.add(rule(byteLength, Operation.REVERSE_REGISTERS));
        rules.add(rule(byteLength, Operation.SWAP_BYTES_WITHIN_REGISTERS));
        rules.add(rule(byteLength, Operation.REVERSE_REGISTERS_AND_SWAP_BYTES));
        return rules;
    }

    public static Rule ruleForLabel(int byteLength, String label) {
        for (Rule rule : rulesForLength(byteLength)) {
            if (rule.label.equals(label)) {
                return rule;
            }
        }
        throw new IllegalArgumentException("未知的資料排列：" + label);
    }

    public static byte[] swapBytesWithinRegisters(byte[] source) {
        return applyOperation(source, Operation.SWAP_BYTES_WITHIN_REGISTERS);
    }

    public static byte[] reverseRegisterOrder(byte[] source) {
        return applyOperation(source, Operation.REVERSE_REGISTERS);
    }

    public static byte[] reverseRegistersAndSwapBytes(byte[] source) {
        return applyOperation(source, Operation.REVERSE_REGISTERS_AND_SWAP_BYTES);
    }

    public static byte[] applyByteOrder(byte[] source, Rule rule) {
        requireRegisterAligned(source.length);
        if (source.length != rule.map.length) {
            throw new IllegalArgumentException("資料長度與排列規則不一致。");
        }
        byte[] ordered = new byte[rule.map.length];
        for (int i = 0; i < rule.map.length; i++) {
            ordered[i] = source[rule.map[i]];
        }
        return ordered;
    }

    public static byte[] restoreByteOrder(byte[] ordered, Rule rule) {
        return applyByteOrder(ordered, rule);
    }

    public static int mappedIndex(int originalIndex, Rule rule) {
        for (int i = 0; i < rule.map.length; i++) {
            if (rule.map[i] == originalIndex) {
                return i;
            }
        }
        throw new IllegalArgumentException("原始 Byte index 不在排列規則中。");
    }

    public static void requireRegisterAligned(int byteLength) {
        if (byteLength < 2 || byteLength % 2 != 0) {
            throw new IllegalArgumentException("資料長度必須符合 16-bit Register，且不可默默截斷最後一個 Byte。");
        }
    }

    private static byte[] applyOperation(byte[] source, Operation operation) {
        return applyByteOrder(source, rule(source.length, operation));
    }

    private static Rule rule(int byteLength, Operation operation) {
        int[] map = map(byteLength, operation);
        return new Rule(label(map), operation, map);
    }

    private static int[] map(int byteLength, Operation operation) {
        requireRegisterAligned(byteLength);
        int[] map = new int[byteLength];
        int registerCount = byteLength / 2;
        for (int r = 0; r < registerCount; r++) {
            int targetRegister = r;
            int sourceRegister = operation == Operation.REVERSE_REGISTERS
                    || operation == Operation.REVERSE_REGISTERS_AND_SWAP_BYTES
                    ? registerCount - 1 - targetRegister
                    : targetRegister;
            boolean swapBytes = operation == Operation.SWAP_BYTES_WITHIN_REGISTERS
                    || operation == Operation.REVERSE_REGISTERS_AND_SWAP_BYTES;
            map[r * 2] = sourceRegister * 2 + (swapBytes ? 1 : 0);
            map[r * 2 + 1] = sourceRegister * 2 + (swapBytes ? 0 : 1);
        }
        return map;
    }

    private static String label(int[] map) {
        StringBuilder label = new StringBuilder();
        for (int index : map) {
            label.append(byteName(index));
        }
        return label.toString();
    }

    private static String byteName(int index) {
        if (index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return "B" + index;
    }

    static boolean sameByteMultiset(byte[] left, byte[] right) {
        byte[] a = Arrays.copyOf(left, left.length);
        byte[] b = Arrays.copyOf(right, right.length);
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }
}
