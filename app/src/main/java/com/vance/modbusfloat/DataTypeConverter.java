package com.vance.modbusfloat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DataTypeConverter {
    private DataTypeConverter() {
    }

    public static final class PaddingInfo {
        public final int effectiveBytes;
        public final int storageBytes;
        public final int paddingBytes;
        public final int paddingStartByte;
        public final int effectiveDigits;
        public final int storageNibbles;
        public final int paddingNibbles;

        private PaddingInfo(int effectiveBytes, int storageBytes, int paddingBytes, int paddingStartByte,
                            int effectiveDigits, int storageNibbles, int paddingNibbles) {
            this.effectiveBytes = effectiveBytes;
            this.storageBytes = storageBytes;
            this.paddingBytes = paddingBytes;
            this.paddingStartByte = paddingStartByte;
            this.effectiveDigits = effectiveDigits;
            this.storageNibbles = storageNibbles;
            this.paddingNibbles = paddingNibbles;
        }
    }

    public static int parseRegister(String raw, RegisterInputMode mode) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return 0;
        }
        switch (mode) {
            case SIGNED:
                int signed = Integer.parseInt(value);
                if (signed < -32768 || signed > 32767) {
                    throw new IllegalArgumentException("Signed Int16 必須介於 -32768 到 32767");
                }
                return signed & 0xFFFF;
            case HEX:
                String hex = value.toUpperCase(Locale.US).replace("0X", "");
                if (!hex.matches("[0-9A-F]{1,4}")) {
                    throw new IllegalArgumentException("Register Hex 必須是 1 到 4 碼");
                }
                return Integer.parseInt(hex, 16);
            case BINARY:
                String binary = value.replaceAll("\\s+", "");
                if (!binary.matches("[01]{16}")) {
                    throw new IllegalArgumentException("Register Binary 必須剛好 16 bits");
                }
                return Integer.parseInt(binary, 2);
            case UNSIGNED:
            default:
                int unsigned = Integer.parseInt(value);
                ModbusFloatConverter.validateRegister(unsigned, "Register");
                return unsigned;
        }
    }

    public static String formatRegisterInput(int value, RegisterInputMode mode) {
        switch (mode) {
            case SIGNED:
                return String.valueOf((short) value);
            case HEX:
                return String.format(Locale.US, "%04X", value & 0xFFFF);
            case BINARY:
                return groupBinary16(toBinary16(value));
            case UNSIGNED:
            default:
                return String.valueOf(value & 0xFFFF);
        }
    }

    public static List<RegisterFormat> registerFormats(int[] registers) {
        List<RegisterFormat> formats = new ArrayList<>();
        for (int i = 0; i < registers.length; i++) {
            int value = registers[i] & 0xFFFF;
            formats.add(new RegisterFormat(
                    i + 1,
                    value,
                    (short) value,
                    String.format(Locale.US, "%04X", value),
                    groupBinary16(toBinary16(value))
            ));
        }
        return formats;
    }

    public static List<DataTypeResult> parse(int[] registers, DataTypeMode mode) {
        switch (mode) {
            case INT16:
                require(registers, 1, mode);
                return parseInt16(registers);
            case UINT16:
                require(registers, 1, mode);
                return parseUInt16(registers);
            case INT32:
                require(registers, 2, mode);
                return parse32(registers, mode);
            case UINT32:
                require(registers, 2, mode);
                return parse32(registers, mode);
            case FLOAT64:
                require(registers, 4, mode);
                return parse64(registers);
            case ASCII:
                return parseAscii(registers, false);
            case BCD:
                return parseBcd(registers);
            case FLOAT32:
            default:
                require(registers, 2, mode);
                return parse32(registers, DataTypeMode.FLOAT32);
        }
    }

    public static List<ReverseResult> encodeFloat32(float value) {
        int bits = Float.floatToRawIntBits(value);
        byte[] base = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bits).array();
        return encodeBytes(base, DataTypeMode.FLOAT32.label(), String.format(Locale.US, "%.7g", value));
    }

    public static List<ReverseResult> encodeFloat64(double value) {
        long bits = Double.doubleToRawLongBits(value);
        byte[] base = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(bits).array();
        return encodeBytes(base, DataTypeMode.FLOAT64.label(), String.format(Locale.US, "%.12g", value));
    }

    public static List<ReverseResult> encode(String input, DataTypeMode mode) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("請輸入要反向轉換的資料。");
        }
        switch (mode) {
            case INT16:
                int int16 = Integer.parseInt(value);
                if (int16 < -32768 || int16 > 32767) {
                    throw new IllegalArgumentException("Int16 必須介於 -32768 到 32767。");
                }
                return encodeBytes(shortBytes(int16), mode.label(), String.valueOf(int16));
            case UINT16:
                int uint16 = Integer.parseInt(value);
                if (uint16 < 0 || uint16 > 65535) {
                    throw new IllegalArgumentException("UInt16 必須介於 0 到 65535。");
                }
                return encodeBytes(shortBytes(uint16), mode.label(), String.valueOf(uint16));
            case INT32:
                int int32 = Integer.parseInt(value);
                return encodeBytes(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(int32).array(), mode.label(), String.valueOf(int32));
            case UINT32:
                long uint32 = Long.parseLong(value);
                if (uint32 < 0 || uint32 > 0xFFFFFFFFL) {
                    throw new IllegalArgumentException("UInt32 必須介於 0 到 4294967295。");
                }
                return encodeBytes(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt((int) uint32).array(), mode.label(), String.valueOf(uint32));
            case FLOAT64:
                return encodeFloat64(Double.parseDouble(value));
            case ASCII:
                return encodeAscii(value);
            case BCD:
                return encodeBcd(value);
            case FLOAT32:
            default:
                return encodeFloat32(Float.parseFloat(value));
        }
    }

    private static List<ReverseResult> encodeAscii(String value) {
        byte[] raw = new byte[value.length()];
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 0x7F) {
                throw new IllegalArgumentException("ASCII 只能輸入 0x00 到 0x7F 的字元。");
            }
            raw[i] = (byte) c;
        }
        PaddingInfo padding = asciiPaddingInfo(value);
        int paddedLength = padding.storageBytes;
        byte[] padded = new byte[paddedLength];
        System.arraycopy(raw, 0, padded, 0, raw.length);
        String label = "ASCII\n原始資料長度：" + raw.length + " Bytes\nRegister 儲存長度：" + paddedLength
                + " Bytes\nPadding：" + (paddedLength - raw.length) + " Byte（尾端 0x00）";
        return encodeBytes(padded, label, asciiText(raw));
    }

    private static List<ReverseResult> encodeBcd(String value) {
        if (!value.matches("[0-9]+")) {
            throw new IllegalArgumentException("Packed BCD 只能輸入十進位數字 0 到 9。");
        }
        PaddingInfo padding = bcdPaddingInfo(value);
        String paddedDigits = value.length() % 2 == 0 ? value : value + "0";
        byte[] bytes = new byte[paddedDigits.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int high = paddedDigits.charAt(i * 2) - '0';
            int low = paddedDigits.charAt(i * 2 + 1) - '0';
            bytes[i] = (byte) ((high << 4) | low);
        }
        int paddedLength = padding.storageBytes;
        byte[] registerBytes = new byte[paddedLength];
        System.arraycopy(bytes, 0, registerBytes, 0, bytes.length);
        String label = "Packed BCD\n有效數字：" + value.length() + " 位\nNibble Padding：" + (paddedDigits.length() - value.length())
                + " 位（尾端 0）\nRegister Padding：" + (paddedLength - bytes.length) + " Byte（尾端 0x00）";
        return encodeBytes(registerBytes, label, value);
    }

    public static PaddingInfo asciiPaddingInfo(String value) {
        int effectiveBytes = value == null ? 0 : value.length();
        int storageBytes = effectiveBytes % 2 == 0 ? effectiveBytes : effectiveBytes + 1;
        if (storageBytes == 0) {
            storageBytes = 2;
        }
        int paddingBytes = storageBytes - effectiveBytes;
        return new PaddingInfo(effectiveBytes, storageBytes, paddingBytes,
                paddingBytes > 0 ? effectiveBytes : -1, 0, 0, 0);
    }

    public static PaddingInfo bcdPaddingInfo(String value) {
        int effectiveDigits = value == null ? 0 : value.length();
        int storageNibbles = effectiveDigits % 2 == 0 ? effectiveDigits : effectiveDigits + 1;
        int byteCount = storageNibbles / 2;
        int storageBytes = byteCount % 2 == 0 ? byteCount : byteCount + 1;
        if (storageBytes == 0) {
            storageBytes = 2;
            storageNibbles = 2;
        }
        int paddingBytes = storageBytes - byteCount;
        int paddingNibbles = storageNibbles - effectiveDigits;
        return new PaddingInfo(byteCount, storageBytes, paddingBytes,
                paddingBytes > 0 ? byteCount : -1, effectiveDigits, storageNibbles, paddingNibbles);
    }

    private static byte[] shortBytes(int value) {
        return new byte[]{(byte) ((value >> 8) & 0xFF), (byte) (value & 0xFF)};
    }

    private static List<ReverseResult> encodeBytes(byte[] base, String dataType, String value) {
        RegisterByteOrder.requireRegisterAligned(base.length);
        List<ReverseResult> rows = new ArrayList<>();
        for (RegisterByteOrder.Rule rule : RegisterByteOrder.rulesForLength(base.length)) {
            byte[] ordered = RegisterByteOrder.applyByteOrder(base, rule);
            int[] registers = registersFromBytes(ordered);
            rows.add(new ReverseResult(
                    rule.label,
                    dataType,
                    value,
                    toHex(ordered),
                    groupBinaryForLength(toBinary(ordered)),
                    registers.length > 0 ? String.valueOf(registers[0]) : "",
                    registers.length > 1 ? String.valueOf(registers[1]) : "",
                    registers.length > 2 ? String.valueOf(registers[2]) : "",
                    registers.length > 3 ? String.valueOf(registers[3]) : "",
                    registerText(registers, registers.length)
            ));
        }
        return rows;
    }

    private static List<DataTypeResult> parseInt16(int[] registers) {
        byte[] source = toBytes(registers, 1);
        List<DataTypeResult> rows = new ArrayList<>();
        for (RegisterByteOrder.Rule rule : RegisterByteOrder.rulesForLength(source.length)) {
            byte[] ordered = RegisterByteOrder.applyByteOrder(source, rule);
            int value = bytesToRegister(ordered[0], ordered[1]);
            rows.add(new DataTypeResult(rule.label, "Int16", String.valueOf((short) value),
                    toHex(ordered), groupBinary16(toBinary(ordered)), registerText(registers, 1)));
        }
        return rows;
    }

    private static List<DataTypeResult> parseUInt16(int[] registers) {
        byte[] source = toBytes(registers, 1);
        List<DataTypeResult> rows = new ArrayList<>();
        for (RegisterByteOrder.Rule rule : RegisterByteOrder.rulesForLength(source.length)) {
            byte[] ordered = RegisterByteOrder.applyByteOrder(source, rule);
            int value = bytesToRegister(ordered[0], ordered[1]);
            rows.add(new DataTypeResult(rule.label, "UInt16", String.valueOf(value),
                    toHex(ordered), groupBinary16(toBinary(ordered)), registerText(registers, 1)));
        }
        return rows;
    }

    private static List<DataTypeResult> parse32(int[] registers, DataTypeMode mode) {
        byte[] source = toBytes(registers, 2);
        List<DataTypeResult> rows = new ArrayList<>();
        for (RegisterByteOrder.Rule rule : RegisterByteOrder.rulesForLength(source.length)) {
            byte[] ordered = RegisterByteOrder.applyByteOrder(source, rule);
            long unsigned = bytesToUInt32(ordered);
            String value;
            if (mode == DataTypeMode.INT32) {
                value = String.valueOf((int) unsigned);
            } else if (mode == DataTypeMode.UINT32) {
                value = String.valueOf(unsigned);
            } else {
                value = String.format(Locale.US, "%.7g", ByteBuffer.wrap(ordered).order(ByteOrder.BIG_ENDIAN).getFloat());
            }
            rows.add(new DataTypeResult(rule.label, mode.label(), value,
                    toHex(ordered), groupBinary32(toBinary(ordered)), registerText(registers, 2)));
        }
        return rows;
    }

    private static List<DataTypeResult> parse64(int[] registers) {
        byte[] source = toBytes(registers, 4);
        List<DataTypeResult> rows = new ArrayList<>();
        for (RegisterByteOrder.Rule rule : RegisterByteOrder.rulesForLength(source.length)) {
            byte[] ordered = RegisterByteOrder.applyByteOrder(source, rule);
            double value = ByteBuffer.wrap(ordered).order(ByteOrder.BIG_ENDIAN).getDouble();
            rows.add(new DataTypeResult(rule.label, "Float64 IEEE754", String.format(Locale.US, "%.12g", value),
                    toHex(ordered), groupBinary64(toBinary(ordered)), registerText(registers, 4)));
        }
        return rows;
    }

    public static List<DataTypeResult> parseAscii(int[] registers, boolean trimTrailingNull) {
        byte[] bytes = toBytes(registers, registers.length);
        List<DataTypeResult> rows = new ArrayList<>();
        for (RegisterByteOrder.Rule rule : RegisterByteOrder.rulesForLength(bytes.length)) {
            byte[] ordered = RegisterByteOrder.applyByteOrder(bytes, rule);
            byte[] displayBytes = trimTrailingNull ? trimTrailingNulls(ordered) : ordered;
            rows.add(new DataTypeResult(rule.label, trimTrailingNull ? "ASCII（忽略尾端 NULL）" : "ASCII", asciiText(displayBytes), toHex(ordered),
                    groupBinaryForLength(toBinary(ordered)), registerText(registers, registers.length)));
        }
        return rows;
    }

    private static List<DataTypeResult> parseBcd(int[] registers) {
        byte[] bytes = toBytes(registers, registers.length);
        List<DataTypeResult> rows = new ArrayList<>();
        for (RegisterByteOrder.Rule rule : RegisterByteOrder.rulesForLength(bytes.length)) {
            byte[] ordered = RegisterByteOrder.applyByteOrder(bytes, rule);
            String hex = toHex(ordered);
            StringBuilder digits = new StringBuilder();
            StringBuilder invalid = new StringBuilder();
            for (int n = 0; n < hex.length(); n++) {
                char c = hex.charAt(n);
                if (c > '9') {
                    if (invalid.length() > 0) {
                        invalid.append('、');
                    }
                    invalid.append(c);
                }
                digits.append(c);
            }
            String value = invalid.length() == 0
                    ? digits.toString()
                    : "BCD 格式錯誤：包含不合法的十進位數字 " + invalid;
            rows.add(new DataTypeResult(rule.label, DataTypeMode.BCD.label(), value, hex, groupBinaryForLength(toBinary(ordered)), registerText(registers, registers.length)));
        }
        return rows;
    }

    private static void require(int[] registers, int count, DataTypeMode mode) {
        if (registers.length < count) {
            throw new IllegalArgumentException(mode.label() + " needs " + count + " registers");
        }
    }

    private static byte[] toBytes(int[] registers, int count) {
        byte[] bytes = new byte[count * 2];
        for (int i = 0; i < count; i++) {
            int register = registers[i] & 0xFFFF;
            bytes[i * 2] = (byte) ((register >> 8) & 0xFF);
            bytes[i * 2 + 1] = (byte) (register & 0xFF);
        }
        return bytes;
    }

    private static long bytesToUInt32(byte[] bytes) {
        return ((long) (bytes[0] & 0xFF) << 24)
                | ((long) (bytes[1] & 0xFF) << 16)
                | ((long) (bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFFL);
    }

    private static int bytesToRegister(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    private static int[] registersFromBytes(byte[] bytes) {
        int[] registers = new int[bytes.length / 2];
        for (int i = 0; i < registers.length; i++) {
            registers[i] = bytesToRegister(bytes[i * 2], bytes[i * 2 + 1]);
        }
        return registers;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format(Locale.US, "%02X", b & 0xFF));
        }
        return builder.toString();
    }

    private static String toBinary(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format(Locale.US, "%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return builder.toString();
    }

    private static String toBinary16(int value) {
        return String.format(Locale.US, "%16s", Integer.toBinaryString(value & 0xFFFF)).replace(' ', '0');
    }

    private static String groupBinary16(String binary) {
        return binary.substring(0, 8) + " " + binary.substring(8, 16);
    }

    private static String groupBinary32(String binary) {
        return binary.substring(0, 8) + " " + binary.substring(8, 16) + "\n"
                + binary.substring(16, 24) + " " + binary.substring(24, 32);
    }

    private static String groupBinary64(String binary) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < binary.length(); i += 8) {
            if (i > 0) {
                builder.append(i % 16 == 0 ? '\n' : ' ');
            }
            builder.append(binary, i, Math.min(i + 8, binary.length()));
        }
        return builder.toString();
    }

    private static String groupBinaryForLength(String binary) {
        if (binary.length() == 16) {
            return groupBinary16(binary);
        }
        if (binary.length() == 32) {
            return groupBinary32(binary);
        }
        return groupBinary64(binary);
    }

    private static String asciiText(byte[] bytes) {
        StringBuilder printable = new StringBuilder();
        for (byte b : bytes) {
            int value = b & 0xFF;
            if (value >= 32 && value <= 126) {
                printable.append((char) value);
            } else {
                printable.append(String.format(Locale.US, "\\x%02X", value));
            }
        }
        return printable.toString();
    }

    private static byte[] trimTrailingNulls(byte[] bytes) {
        int length = bytes.length;
        while (length > 0 && bytes[length - 1] == 0) {
            length--;
        }
        byte[] trimmed = new byte[length];
        System.arraycopy(bytes, 0, trimmed, 0, length);
        return trimmed;
    }

    private static String registerText(int[] registers, int count) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(String.valueOf(registers[i] & 0xFFFF));
        }
        return String.join(", ", values);
    }

}
