package com.vance.modbusfloat;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegisterByteOrderTest {
    @Test
    public void exposesExplicitMappingTables() {
        assertLabels(RegisterByteOrder.rulesForLength(2), "AB", "BA");
        assertLabels(RegisterByteOrder.rulesForLength(4), "ABCD", "CDAB", "BADC", "DCBA");
        assertLabels(RegisterByteOrder.rulesForLength(8), "ABCDEFGH", "GHEFCDAB", "BADCFEHG", "HGFEDCBA");
        assertLabels(RegisterByteOrder.rulesForLength(6), "ABCDEF", "EFCDAB", "BADCFE", "FEDCBA");
    }

    @Test
    public void byteAndRegisterOrderOperationsAreInvolutions() {
        byte[] source = hexToBytes("414243444546");
        assertArrayEquals(source, RegisterByteOrder.swapBytesWithinRegisters(RegisterByteOrder.swapBytesWithinRegisters(source)));
        assertArrayEquals(source, RegisterByteOrder.reverseRegisterOrder(RegisterByteOrder.reverseRegisterOrder(source)));
        assertArrayEquals(source, RegisterByteOrder.reverseRegistersAndSwapBytes(RegisterByteOrder.reverseRegistersAndSwapBytes(source)));
        assertArrayEquals(hexToBytes("424144434645"), RegisterByteOrder.swapBytesWithinRegisters(source));
        assertArrayEquals(hexToBytes("454643444142"), RegisterByteOrder.reverseRegisterOrder(source));
        assertArrayEquals(hexToBytes("464544434241"), RegisterByteOrder.reverseRegistersAndSwapBytes(source));
    }

    @Test
    public void propertyBasedOrderRulesAreReversibleAndLossless() {
        Random random = new Random(0x4D4F4442L);
        for (int registers = 1; registers <= 64; registers++) {
            for (int sample = 0; sample < 40; sample++) {
                byte[] source = new byte[registers * 2];
                if (sample == 0) {
                    Arrays.fill(source, (byte) 0x00);
                } else if (sample == 1) {
                    Arrays.fill(source, (byte) 0xFF);
                } else if (sample == 2) {
                    for (int i = 0; i < source.length; i++) {
                        source[i] = (byte) (i % 2 == 0 ? 0x55 : 0xAA);
                    }
                } else {
                    random.nextBytes(source);
                }
                byte[] sortedSource = sorted(source);
                for (RegisterByteOrder.Rule rule : RegisterByteOrder.rulesForLength(source.length)) {
                    byte[] ordered = RegisterByteOrder.applyByteOrder(source, rule);
                    assertEquals(source.length, ordered.length);
                    assertArrayEquals(sortedSource, sorted(ordered));
                    assertArrayEquals(source, RegisterByteOrder.restoreByteOrder(ordered, rule));
                    assertArrayEquals(source, RegisterByteOrder.applyByteOrder(ordered, rule));
                }
            }
        }
    }

    @Test
    public void asciiPaddingPositionIsTrackableAfterOrderChanges() {
        DataTypeConverter.PaddingInfo padding = DataTypeConverter.asciiPaddingInfo("hello");
        assertEquals(5, padding.effectiveBytes);
        assertEquals(6, padding.storageBytes);
        assertEquals(1, padding.paddingBytes);
        assertEquals(5, padding.paddingStartByte);

        RegisterByteOrder.Rule swap = RegisterByteOrder.ruleForLabel(6, "BADCFE");
        assertEquals(4, RegisterByteOrder.mappedIndex(padding.paddingStartByte, swap));
        byte[] swapped = RegisterByteOrder.applyByteOrder(hexToBytes("68656C6C6F00"), swap);
        assertArrayEquals(hexToBytes("65686C6C006F"), swapped);
    }

    @Test
    public void bcdPaddingKeepsEffectiveDigitsAndLeadingZeroes() {
        DataTypeConverter.PaddingInfo even = DataTypeConverter.bcdPaddingInfo("00123456");
        assertEquals(8, even.effectiveDigits);
        assertEquals(8, even.storageNibbles);
        assertEquals(0, even.paddingNibbles);
        assertEquals(4, even.storageBytes);

        DataTypeConverter.PaddingInfo odd = DataTypeConverter.bcdPaddingInfo("12345");
        assertEquals(5, odd.effectiveDigits);
        assertEquals(6, odd.storageNibbles);
        assertEquals(1, odd.paddingNibbles);
        assertEquals(4, odd.storageBytes);
        assertEquals(1, odd.paddingBytes);
    }

    @Test
    public void reverseAndForwardRoundTripForEverySupportedOrder() {
        assertForwardReverseRoundTrip("-1", DataTypeMode.INT16, "-1");
        assertForwardReverseRoundTrip("65535", DataTypeMode.UINT16, "65535");
        assertForwardReverseRoundTrip("10.0", DataTypeMode.FLOAT32, "10.00000");
        assertForwardReverseRoundTrip("10.0", DataTypeMode.FLOAT64, "10.0000000000");
        assertForwardReverseRoundTrip("-1", DataTypeMode.INT32, "-1");
        assertForwardReverseRoundTrip("4294967295", DataTypeMode.UINT32, "4294967295");
        assertForwardReverseRoundTrip("ABCDEF", DataTypeMode.ASCII, "ABCDEF");
        assertForwardReverseRoundTrip("00123456", DataTypeMode.BCD, "00123456");
    }

    @Test
    public void sixteenBitByteSwapUsesSharedOrderCore() {
        List<DataTypeResult> int16 = DataTypeConverter.parse(new int[]{0x1234}, DataTypeMode.INT16);
        assertEquals("AB", int16.get(0).order);
        assertEquals("4660", int16.get(0).value);
        assertEquals("1234", int16.get(0).hex);
        assertEquals("BA", int16.get(1).order);
        assertEquals("13330", int16.get(1).value);
        assertEquals("3412", int16.get(1).hex);

        List<DataTypeResult> uint16 = DataTypeConverter.parse(new int[]{0xFF00}, DataTypeMode.UINT16);
        assertEquals("65280", uint16.get(0).value);
        assertEquals("255", uint16.get(1).value);
    }

    @Test
    public void floatingPointRawBitsRoundTripThroughOrderCore() {
        int[] floatBits = {
                0x00000000,
                0x80000000,
                0x3F800000,
                0xBF800000,
                0x41200000,
                0x3F9E0419,
                0x7F7FFFFF,
                0x00800000,
                0x00000001,
                0x7F800000,
                0xFF800000,
                0x7FC01234
        };
        for (int bits : floatBits) {
            assertRawRoundTrip(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bits).array());
        }

        long[] doubleBits = {
                0x0000000000000000L,
                0x8000000000000000L,
                0x3FF0000000000000L,
                0xBFF0000000000000L,
                0x4024000000000000L,
                0x40251C28F5C28F5CL,
                0x7FEFFFFFFFFFFFFFL,
                0x0010000000000000L,
                0x0000000000000001L,
                0x7FF0000000000000L,
                0xFFF0000000000000L,
                0x7FF8000000001234L
        };
        for (long bits : doubleBits) {
            assertRawRoundTrip(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(bits).array());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void oddByteLengthIsRejectedInsteadOfTruncated() {
        RegisterByteOrder.rulesForLength(3);
    }

    private static void assertForwardReverseRoundTrip(String input, DataTypeMode mode, String expectedValue) {
        String canonicalHex = DataTypeConverter.encode(input, mode).get(0).hex;
        for (ReverseResult encoded : DataTypeConverter.encode(input, mode)) {
            int[] registers = registersFromHex(encoded.hex);
            List<DataTypeResult> decoded = mode == DataTypeMode.ASCII
                    ? DataTypeConverter.parseAscii(registers, false)
                    : DataTypeConverter.parse(registers, mode);
            DataTypeResult matching = findOrder(decoded, encoded.order);
            assertEquals(canonicalHex, matching.hex);
            assertEquals(expectedValue, matching.value);
        }
    }

    private static void assertRawRoundTrip(byte[] raw) {
        for (RegisterByteOrder.Rule rule : RegisterByteOrder.rulesForLength(raw.length)) {
            byte[] ordered = RegisterByteOrder.applyByteOrder(raw, rule);
            assertArrayEquals(raw, RegisterByteOrder.restoreByteOrder(ordered, rule));
        }
    }

    private static DataTypeResult findOrder(List<DataTypeResult> rows, String order) {
        for (DataTypeResult row : rows) {
            if (row.order.equals(order)) {
                return row;
            }
        }
        throw new AssertionError("找不到排列結果：" + order);
    }

    private static void assertLabels(List<RegisterByteOrder.Rule> rules, String... labels) {
        assertEquals(labels.length, rules.size());
        for (int i = 0; i < labels.length; i++) {
            assertEquals(labels[i], rules.get(i).label);
        }
    }

    private static int[] registersFromHex(String hex) {
        String compact = hex.replaceAll("\\s+", "").toUpperCase();
        int[] registers = new int[compact.length() / 4];
        for (int i = 0; i < registers.length; i++) {
            registers[i] = Integer.parseInt(compact.substring(i * 4, i * 4 + 4), 16);
        }
        return registers;
    }

    private static byte[] hexToBytes(String hex) {
        String compact = hex.replaceAll("\\s+", "");
        byte[] bytes = new byte[compact.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(compact.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private static byte[] sorted(byte[] bytes) {
        byte[] copy = Arrays.copyOf(bytes, bytes.length);
        Arrays.sort(copy);
        return copy;
    }
}
