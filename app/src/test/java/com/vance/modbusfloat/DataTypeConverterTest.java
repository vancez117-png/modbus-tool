package com.vance.modbusfloat;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DataTypeConverterTest {
    @Test
    public void parsesRegisterFormats() {
        assertEquals(65535, DataTypeConverter.parseRegister("-1", RegisterInputMode.SIGNED));
        assertEquals(17096, DataTypeConverter.parseRegister("42C8", RegisterInputMode.HEX));
        assertEquals(17096, DataTypeConverter.parseRegister("01000010 11001000", RegisterInputMode.BINARY));
        assertEquals(16672, DataTypeConverter.parseRegister("0100 0001\n0010 0000", RegisterInputMode.BINARY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void incompleteRegisterBinaryIsRejectedOnlyForFormalParse() {
        DataTypeConverter.parseRegister("01000001001000", RegisterInputMode.BINARY);
    }

    @Test
    public void parsesInt32UInt32AndFloat32Orders() {
        int[] registers = new int[]{17096, 0, 0, 0};
        List<DataTypeResult> float32 = DataTypeConverter.parse(registers, DataTypeMode.FLOAT32);
        assertEquals("ABCD", float32.get(0).order);
        assertEquals("100.0000", float32.get(0).value);

        List<DataTypeResult> float32Ten = DataTypeConverter.parse(registersFromHex("41200000"), DataTypeMode.FLOAT32);
        assertEquals("10.00000", float32Ten.get(0).value);

        List<DataTypeResult> uint32 = DataTypeConverter.parse(registers, DataTypeMode.UINT32);
        assertEquals("1120403456", uint32.get(0).value);

        List<DataTypeResult> int32 = DataTypeConverter.parse(registers, DataTypeMode.INT32);
        assertEquals("1120403456", int32.get(0).value);
    }

    @Test
    public void parsesFloat64Orders() {
        int[] registers = new int[]{16420, 0, 0, 0};
        List<DataTypeResult> rows = DataTypeConverter.parse(registers, DataTypeMode.FLOAT64);
        assertEquals("ABCDEFGH", rows.get(0).order);
        assertEquals("10.0000000000", rows.get(0).value);
        assertEquals("4024000000000000", rows.get(0).hex);
        assertEquals("0000000000004024", rows.get(1).hex);
    }

    @Test
    public void parsesFloat64FromHexBinaryAndRegisters() {
        assertFloat64("4024000000000000", 10.0);
        assertFloat64("BFF0000000000000", -1.0);
        assertFloat64("3FF0000000000000", 1.0);
        assertFloat64("0000000000000000", 0.0);
        double value = Double.parseDouble(DataTypeConverter.parse(registersFromHex("40251C28F5C28F5C"), DataTypeMode.FLOAT64).get(0).value);
        assertEquals(10.555, value, 0.00000000001);
    }

    @Test
    public void parsesFloat64SpecialRawBits() {
        List<DataTypeResult> infinity = DataTypeConverter.parse(registersFromHex("7FF0000000000000"), DataTypeMode.FLOAT64);
        assertTrue(Double.isInfinite(Double.parseDouble(infinity.get(0).value)));

        List<DataTypeResult> negativeInfinity = DataTypeConverter.parse(registersFromHex("FFF0000000000000"), DataTypeMode.FLOAT64);
        assertTrue(Double.parseDouble(negativeInfinity.get(0).value) < 0);
        assertTrue(Double.isInfinite(Double.parseDouble(negativeInfinity.get(0).value)));

        List<DataTypeResult> nan = DataTypeConverter.parse(registersFromHex("7FF8000000000001"), DataTypeMode.FLOAT64);
        assertTrue(Double.isNaN(Double.parseDouble(nan.get(0).value)));

        List<DataTypeResult> subnormal = DataTypeConverter.parse(registersFromHex("0000000000000001"), DataTypeMode.FLOAT64);
        assertEquals(Double.MIN_VALUE, Double.parseDouble(subnormal.get(0).value), 0.0);

        List<DataTypeResult> negativeZero = DataTypeConverter.parse(registersFromHex("8000000000000000"), DataTypeMode.FLOAT64);
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(Double.parseDouble(negativeZero.get(0).value)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsFloat64WhenOnlyFloat32RegistersArePresent() {
        DataTypeConverter.parse(new int[]{0x4024, 0x0000}, DataTypeMode.FLOAT64);
    }

    @Test
    public void encodesFloat64ReverseOrders() {
        List<ReverseResult> rows = DataTypeConverter.encodeFloat64(10.0);
        assertEquals("ABCDEFGH", rows.get(0).order);
        assertEquals("4024000000000000", rows.get(0).hex);
        assertEquals("16420", rows.get(0).register1);
        assertEquals("0", rows.get(0).register2);
        assertEquals("0", rows.get(0).register3);
        assertEquals("0", rows.get(0).register4);
        assertEquals("GHEFCDAB", rows.get(1).order);
    }

    @Test
    public void encodesFloat32SpecialValues() {
        assertEquals("80000000", DataTypeConverter.encodeFloat32(-0.0f).get(0).hex);
        assertEquals("7F800000", DataTypeConverter.encodeFloat32(Float.POSITIVE_INFINITY).get(0).hex);
        assertEquals("7FC01234", DataTypeConverter.encodeFloat32(Float.intBitsToFloat(0x7FC01234)).get(0).hex);
    }

    @Test
    public void encodesAllReverseDataTypes() {
        List<ReverseResult> int16 = DataTypeConverter.encode("-1", DataTypeMode.INT16);
        assertEquals("AB", int16.get(0).order);
        assertEquals("FFFF", int16.get(0).hex);
        assertEquals("BA", int16.get(1).order);

        List<ReverseResult> uint16 = DataTypeConverter.encode("65535", DataTypeMode.UINT16);
        assertEquals("FFFF", uint16.get(0).hex);

        List<ReverseResult> int32 = DataTypeConverter.encode("-1", DataTypeMode.INT32);
        assertEquals("ABCD", int32.get(0).order);
        assertEquals("FFFFFFFF", int32.get(0).hex);
        assertEquals("DCBA", int32.get(3).order);

        List<ReverseResult> uint32 = DataTypeConverter.encode("4294967295", DataTypeMode.UINT32);
        assertEquals("FFFFFFFF", uint32.get(0).hex);

        assertEquals("41200000", DataTypeConverter.encode("10.0", DataTypeMode.FLOAT32).get(0).hex);
        assertEquals("4024000000000000", DataTypeConverter.encode("10.0", DataTypeMode.FLOAT64).get(0).hex);
        assertEquals("80000000", DataTypeConverter.encode("-0.0", DataTypeMode.FLOAT32).get(0).hex);

        List<ReverseResult> ascii = DataTypeConverter.encode("hello", DataTypeMode.ASCII);
        assertEquals("68656C6C6F00", ascii.get(0).hex);
        assertEquals("26725, 27756, 28416", ascii.get(0).registers);

        List<ReverseResult> bcd = DataTypeConverter.encode("00123456", DataTypeMode.BCD);
        assertEquals("00123456", bcd.get(0).hex);
        assertEquals("18, 13398", bcd.get(0).registers);
    }

    @Test
    public void reverseAndForwardRoundTripUseSameOrderMapping() {
        for (ReverseResult row : DataTypeConverter.encode("10.0", DataTypeMode.FLOAT32)) {
            int[] registers = registersFromHex(row.hex);
            assertEquals(row.hex, DataTypeConverter.parse(registers, DataTypeMode.FLOAT32).get(0).hex);
        }
        List<ReverseResult> float64 = DataTypeConverter.encode("10.0", DataTypeMode.FLOAT64);
        assertEquals("4024000000000000", float64.get(0).hex);
        assertEquals("0000000000004024", float64.get(1).hex);
        assertEquals("2440000000000000", float64.get(2).hex);
        assertEquals("0000000000002440", float64.get(3).hex);
    }

    @Test
    public void parsesAsciiAndBcd() {
        int[] ascii = new int[]{0x4142, 0x4344};
        assertEquals("ABCD", DataTypeConverter.parse(ascii, DataTypeMode.ASCII).get(0).value);
        assertEquals("AB", DataTypeConverter.parse(registersFromHex("4142"), DataTypeMode.ASCII).get(0).value);
        assertEquals("ABCD", DataTypeConverter.parse(registersFromBinary("01000001010000100100001101000100"), DataTypeMode.ASCII).get(0).value);
        assertEquals("ABCDEF", DataTypeConverter.parse(registersFromHex("414243444546"), DataTypeMode.ASCII).get(0).value);
        assertEquals("ABCDEFGH", DataTypeConverter.parse(registersFromHex("4142434445464748"), DataTypeMode.ASCII).get(0).value);

        int[] paddedAscii = registersFromHex("41424300");
        assertEquals("ABC\\x00", DataTypeConverter.parseAscii(paddedAscii, false).get(0).value);
        assertEquals("ABC", DataTypeConverter.parseAscii(paddedAscii, true).get(0).value);

        int[] asciiWithControls = new int[]{0x4100, 0x421B};
        assertEquals("A\\x00B\\x1B", DataTypeConverter.parse(asciiWithControls, DataTypeMode.ASCII).get(0).value);
        assertEquals("A\\x80", DataTypeConverter.parse(new int[]{0x4180}, DataTypeMode.ASCII).get(0).value);

        int[] bcd = new int[]{0x1234, 0x5678};
        assertEquals("1234", DataTypeConverter.parse(registersFromHex("1234"), DataTypeMode.BCD).get(0).value);
        assertEquals("12345678", DataTypeConverter.parse(bcd, DataTypeMode.BCD).get(0).value);
        assertEquals("12345678", DataTypeConverter.parse(registersFromBinary("00010010001101000101011001111000"), DataTypeMode.BCD).get(0).value);
        assertEquals("00123456", DataTypeConverter.parse(registersFromHex("00123456"), DataTypeMode.BCD).get(0).value);
        assertEquals("0001", DataTypeConverter.parse(new int[]{0x0001}, DataTypeMode.BCD).get(0).value);
        assertEquals("1234567898760001", DataTypeConverter.parse(registersFromHex("1234567898760001"), DataTypeMode.BCD).get(0).value);

        List<DataTypeResult> orderedBcd = DataTypeConverter.parse(bcd, DataTypeMode.BCD);
        assertEquals("56781234", orderedBcd.get(1).value);
        assertEquals("34127856", orderedBcd.get(2).value);
        assertEquals("78563412", orderedBcd.get(3).value);

        String invalidBcd = DataTypeConverter.parse(new int[]{0x12AF}, DataTypeMode.BCD).get(0).value;
        assertTrue(invalidBcd.startsWith("BCD 格式錯誤"));
        assertTrue(invalidBcd.contains("A"));
        assertTrue(invalidBcd.contains("F"));
    }

    @Test
    public void parsesVariableLengthAsciiAndBcdBeyondFourRegisters() {
        int[] ascii = new int[]{0x4142, 0x4344, 0x4546, 0x4748, 0x494A};
        List<DataTypeResult> asciiRows = DataTypeConverter.parse(ascii, DataTypeMode.ASCII);
        assertEquals("ABCDEFGHIJ", asciiRows.get(0).value);
        assertEquals("IJGHEFCDAB", asciiRows.get(1).value);
        assertEquals("BADCFEHGJI", asciiRows.get(2).value);
        assertEquals("JIHGFEDCBA", asciiRows.get(3).value);

        int[] bcd = new int[]{0x1234, 0x5678, 0x9012, 0x3456, 0x7890};
        List<DataTypeResult> bcdRows = DataTypeConverter.parse(bcd, DataTypeMode.BCD);
        assertEquals("12345678901234567890", bcdRows.get(0).value);
        assertEquals("78903456901256781234", bcdRows.get(1).value);
        assertEquals("34127856129056349078", bcdRows.get(2).value);
        assertEquals("90785634129078563412", bcdRows.get(3).value);
    }

    private static void assertFloat64(String hex, double expected) {
        int[] fromHex = registersFromHex(hex);
        int[] fromBinary = registersFromBinary(binaryFromHex(hex));
        assertEquals(Double.toString(expected), Double.toString(Double.parseDouble(DataTypeConverter.parse(fromHex, DataTypeMode.FLOAT64).get(0).value)));
        assertEquals(Double.toString(expected), Double.toString(Double.parseDouble(DataTypeConverter.parse(fromBinary, DataTypeMode.FLOAT64).get(0).value)));
        assertEquals(hex, DataTypeConverter.parse(fromHex, DataTypeMode.FLOAT64).get(0).hex);
    }

    private static int[] registersFromHex(String hex) {
        String compact = hex.replaceAll("\\s+", "").toUpperCase();
        int[] registers = new int[compact.length() / 4];
        for (int i = 0; i < registers.length; i++) {
            registers[i] = Integer.parseInt(compact.substring(i * 4, i * 4 + 4), 16);
        }
        return registers;
    }

    private static int[] registersFromBinary(String binary) {
        return registersFromHex(hexFromBinary(binary));
    }

    private static String binaryFromHex(String hex) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            int value = Integer.parseInt(hex.substring(i, i + 2), 16);
            builder.append(String.format("%8s", Integer.toBinaryString(value)).replace(' ', '0'));
        }
        return builder.toString();
    }

    private static String hexFromBinary(String binary) {
        String compact = binary.replaceAll("\\s+", "");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < compact.length(); i += 4) {
            builder.append(Integer.toHexString(Integer.parseInt(compact.substring(i, i + 4), 2)).toUpperCase());
        }
        return builder.toString();
    }
}
