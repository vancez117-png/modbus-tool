package com.vance.modbusfloat;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ModbusFloatConverterTest {
    @Test
    public void encodeOnePointZeroAllOrders() {
        assertRegisters(1.0f, "3F800000", 16256, 0, 0, 16256, 32831, 0, 0, 32831);
    }

    @Test
    public void encodeTenPointZeroAllOrders() {
        assertRegisters(10.0f, "41200000", 16672, 0, 0, 16672, 8257, 0, 0, 8257);
    }

    @Test
    public void encodeOneHundredPointZeroAllOrders() {
        assertRegisters(100.0f, "42C80000", 17096, 0, 0, 17096, 51266, 0, 0, 51266);
    }

    @Test
    public void encodeNegativeOnePointZeroAllOrders() {
        assertRegisters(-1.0f, "BF800000", 49024, 0, 0, 49024, 32959, 0, 0, 32959);
    }

    @Test
    public void decodeRegistersAllOrders() {
        List<ConversionResult> results = ModbusFloatConverter.decodeRegisters(16672, 0);
        assertEquals(10.0f, results.get(0).floatValue, 0.0f);
        assertEquals(2.3362448E-41f, results.get(1).floatValue, 0.0f);
        assertEquals(1.6347736E-19f, results.get(2).floatValue, 0.0f);
        assertEquals(1.1570521E-41f, results.get(3).floatValue, 0.0f);
    }

    @Test
    public void decodeBinaryAcceptsByteSpaces() {
        List<ConversionResult> results = ModbusFloatConverter.decodeBinary("01000001 00100000 00000000 00000000");
        assertEquals("41200000", results.get(0).hex);
        assertEquals(10.0f, results.get(0).floatValue, 0.0f);
    }

    @Test
    public void decodeBinaryAcceptsNewlineGroups() {
        List<ConversionResult> results = ModbusFloatConverter.decodeBinary("01000001 00100000\n00000000 00000000");
        assertEquals("41200000", results.get(0).hex);
        assertEquals(10.0f, results.get(0).floatValue, 0.0f);
    }

    @Test
    public void preservesFloat32SpecialBitPatterns() {
        List<ConversionResult> negativeZero = ModbusFloatConverter.encodeFloat(-0.0f);
        assertEquals("80000000", negativeZero.get(0).hex);
        assertEquals(32768, negativeZero.get(0).register1);
        assertEquals(0, negativeZero.get(0).register2);

        List<ConversionResult> positiveInfinity = ModbusFloatConverter.encodeFloat(Float.POSITIVE_INFINITY);
        assertEquals("7F800000", positiveInfinity.get(0).hex);

        float rawNan = Float.intBitsToFloat(0x7FC01234);
        List<ConversionResult> nan = ModbusFloatConverter.encodeFloat(rawNan);
        assertEquals("7FC01234", nan.get(0).hex);
    }

    @Test
    public void forwardAndReverseOrdersStayConsistent() {
        float value = 10.22f;
        List<ConversionResult> encoded = ModbusFloatConverter.encodeFloat(value);
        for (int i = 0; i < encoded.size(); i++) {
            ConversionResult row = encoded.get(i);
            List<ConversionResult> decoded = ModbusFloatConverter.decodeRegisters(row.register1, row.register2);
            assertEquals(row.order, decoded.get(i).order);
            assertEquals(value, decoded.get(i).floatValue, 0.0f);
        }
    }

    private void assertRegisters(
            float value,
            String hex,
            int abcd1,
            int abcd2,
            int cdab1,
            int cdab2,
            int badc1,
            int badc2,
            int dcba1,
            int dcba2
    ) {
        List<ConversionResult> results = ModbusFloatConverter.encodeFloat(value);
        assertEquals(hex, results.get(0).hex);
        assertOrder(results.get(0), ByteOrderMode.ABCD, abcd1, abcd2, value);
        assertOrder(results.get(1), ByteOrderMode.CDAB, cdab1, cdab2, value);
        assertOrder(results.get(2), ByteOrderMode.BADC, badc1, badc2, value);
        assertOrder(results.get(3), ByteOrderMode.DCBA, dcba1, dcba2, value);
    }

    private void assertOrder(ConversionResult result, ByteOrderMode order, int register1, int register2, float value) {
        assertEquals(order, result.order);
        assertEquals(register1, result.register1);
        assertEquals(register2, result.register2);
        assertEquals(value, result.floatValue, 0.0f);
    }
}
