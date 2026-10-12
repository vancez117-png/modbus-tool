package com.vance.modbusfloat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ModbusFloatConverter {
    private ModbusFloatConverter() {
    }

    public static List<ConversionResult> decodeRegisters(int register1, int register2) {
        validateRegister(register1, "Register 1");
        validateRegister(register2, "Register 2");

        byte[] source = registerBytes(register1, register2);
        List<ConversionResult> results = new ArrayList<>();
        for (ByteOrderMode order : ByteOrderMode.values()) {
            byte[] ordered = applyOrder(source, order);
            int bits = bytesToInt(ordered);
            results.add(new ConversionResult(
                    order,
                    toBinary(bits),
                    toHex(bits),
                    register1,
                    register2,
                    Float.intBitsToFloat(bits)
            ));
        }
        return results;
    }

    public static List<ConversionResult> encodeFloat(float value) {
        int bits = Float.floatToRawIntBits(value);
        byte[] base = intToBytes(bits);
        List<ConversionResult> results = new ArrayList<>();
        for (ByteOrderMode order : ByteOrderMode.values()) {
            byte[] registers = applyOrder(base, order);
            results.add(new ConversionResult(
                    order,
                    toBinary(bits),
                    toHex(bits),
                    bytesToRegister(registers[0], registers[1]),
                    bytesToRegister(registers[2], registers[3]),
                    value
            ));
        }
        return results;
    }

    public static List<ConversionResult> decodeHex(String hex) {
        String normalized = validateHex(hex);
        int bits = (int) Long.parseLong(normalized, 16);
        byte[] bytes = intToBytes(bits);
        int register1 = bytesToRegister(bytes[0], bytes[1]);
        int register2 = bytesToRegister(bytes[2], bytes[3]);
        return decodeRegisters(register1, register2);
    }

    public static List<ConversionResult> decodeBinary(String binary) {
        String normalized = validateBinary(binary);
        int bits = (int) Long.parseLong(normalized, 2);
        byte[] bytes = intToBytes(bits);
        int register1 = bytesToRegister(bytes[0], bytes[1]);
        int register2 = bytesToRegister(bytes[2], bytes[3]);
        return decodeRegisters(register1, register2);
    }

    public static void validateRegister(int value, String label) {
        if (value < 0 || value > 65535) {
            throw new IllegalArgumentException(label + " must be 0 to 65535");
        }
    }

    public static String validateHex(String hex) {
        String normalized = hex == null ? "" : hex.trim().toUpperCase(Locale.US);
        if (!normalized.matches("[0-9A-F]{8}")) {
            throw new IllegalArgumentException("Hex must be exactly 8 characters, 0-9 or A-F");
        }
        return normalized;
    }

    public static String validateBinary(String binary) {
        String normalized = binary == null ? "" : binary.replaceAll("\\s+", "");
        if (!normalized.matches("[01]{32}")) {
            throw new IllegalArgumentException("Binary must be exactly 32 bits and only 0 or 1");
        }
        return normalized;
    }

    public static String toHex(int bits) {
        return String.format(Locale.US, "%08X", bits);
    }

    public static String toBinary(int bits) {
        String value = Integer.toBinaryString(bits);
        if (value.length() >= 32) {
            return value.substring(value.length() - 32);
        }
        return String.format(Locale.US, "%32s", value).replace(' ', '0');
    }

    private static byte[] registerBytes(int register1, int register2) {
        return new byte[]{
                (byte) ((register1 >> 8) & 0xFF),
                (byte) (register1 & 0xFF),
                (byte) ((register2 >> 8) & 0xFF),
                (byte) (register2 & 0xFF)
        };
    }

    private static byte[] applyOrder(byte[] source, ByteOrderMode order) {
        int[] map = order.map();
        return new byte[]{source[map[0]], source[map[1]], source[map[2]], source[map[3]]};
    }

    private static int bytesToRegister(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFF);
    }

    private static byte[] intToBytes(int bits) {
        return new byte[]{
                (byte) ((bits >> 24) & 0xFF),
                (byte) ((bits >> 16) & 0xFF),
                (byte) ((bits >> 8) & 0xFF),
                (byte) (bits & 0xFF)
        };
    }
}
