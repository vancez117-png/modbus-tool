package com.vance.modbusfloat;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModbusPacketParserTest {
    @Test
    public void parsesRtuAndChecksCrc() {
        PacketParseResult result = ModbusPacketParser.parseRtu("01 03 04 42 48 00 00 6E 5D");
        assertEquals("1", result.slaveId);
        assertEquals("03", result.functionCode);
        assertEquals("4", result.byteCount);
        assertEquals("42480000", result.registerDataHex);
        assertTrue(result.crcValid);
        assertEquals("5D6E", result.crcCalculated);
        assertEquals("Response", result.frameKind);
        assertEquals("回覆 Holding Registers 資料，資料長度 4 bytes", result.action);
        assertEquals(2, result.registers.size());
    }

    @Test
    public void reportsBadCrc() {
        PacketParseResult result = ModbusPacketParser.parseRtu("01 03 04 42 48 00 00 00 00");
        assertFalse(result.crcValid);
        assertEquals("5D6E", result.crcCalculated);
    }

    @Test
    public void parsesTcp() {
        PacketParseResult result = ModbusPacketParser.parseTcp("00 01 00 00 00 07 01 03 04 42 48 00 00");
        assertEquals("1", result.transactionId);
        assertEquals("0", result.protocolId);
        assertEquals("7", result.length);
        assertEquals("1", result.unitId);
        assertEquals("Response", result.frameKind);
        assertEquals(2, result.registers.size());
    }

    @Test
    public void parsesRtuReadRequestAction() {
        PacketParseResult result = ModbusPacketParser.parseRtu("01 03 00 7A 00 01 A5 D3");
        assertTrue(result.crcValid);
        assertEquals("Request", result.frameKind);
        assertEquals("讀取 Holding Registers，起始位址 122 (0x007A)，數量 1", result.action);
    }

    @Test
    public void parsesRtuWriteSingleRegisterAction() {
        PacketParseResult result = ModbusPacketParser.parseRtu("01 06 00 82 00 00 29 E2");
        assertTrue(result.crcValid);
        assertEquals("Request / Response 皆可能", result.frameKind);
        assertEquals("寫入單一 Holding Register，位址 130 (0x0082)，值 0 (0x0000)；此封包格式的 Request 與 Response 相同", result.action);
        assertTrue(result.registerDataDecodable);
        assertEquals("0000", result.registerDataHex);
    }

    @Test
    public void parsesRtuWriteMultipleRegistersAction() {
        PacketParseResult result = ModbusPacketParser.parseRtu("01 10 00 7A 00 02 04 42 48 00 00 E0 9A");
        assertTrue(result.crcValid);
        assertEquals("Request", result.frameKind);
        assertEquals("寫入多個 Holding Registers，起始位址 122 (0x007A)，數量 2，資料 42480000", result.action);
        assertEquals(2, result.registers.size());
    }

    @Test
    public void parsesRtuWriteMultipleRegistersResponse() {
        PacketParseResult result = ModbusPacketParser.parseRtu("01 10 00 7A 00 02 60 11");
        assertTrue(result.crcValid);
        assertEquals("Response", result.frameKind);
        assertEquals("回覆寫入多個 Holding Registers，起始位址 122 (0x007A)，數量 2", result.action);
    }

    @Test
    public void parsesRtuWriteMultipleCoilsAction() {
        PacketParseResult result = ModbusPacketParser.parseRtu("01 0F 00 10 00 0A 02 CD 01 72 F8");
        assertTrue(result.crcValid);
        assertEquals("Request", result.frameKind);
        assertEquals("寫入多個 Coils，起始位址 16 (0x0010)，數量 10，資料 CD01", result.action);
    }

    @Test
    public void parsesRtuWriteMultipleCoilsResponse() {
        PacketParseResult result = ModbusPacketParser.parseRtu("01 0F 00 10 00 0A D4 09");
        assertTrue(result.crcValid);
        assertEquals("Response", result.frameKind);
        assertEquals("回覆寫入多個 Coils，起始位址 16 (0x0010)，數量 10", result.action);
    }

    @Test
    public void parsesRtuReadCoilsAction() {
        PacketParseResult result = ModbusPacketParser.parseRtu("01 01 00 10 00 10 3C 03");
        assertTrue(result.crcValid);
        assertEquals("Request", result.frameKind);
        assertEquals("讀取 Coils，起始位址 16 (0x0010)，數量 16", result.action);
    }

    @Test
    public void parsesRtuReadCoilsResponse() {
        PacketParseResult result = ModbusPacketParser.parseRtu("01 01 02 CD 01 2C AC");
        assertTrue(result.crcValid);
        assertEquals("Response", result.frameKind);
        assertEquals("回覆 Coils 狀態，資料長度 2 bytes，資料 CD01", result.action);
    }

    @Test
    public void parsesRtuReadMultipleRegistersRequestAndResponse() {
        PacketParseResult request = ModbusPacketParser.parseRtu("01 03 00 7A 00 04 65 D0");
        assertTrue(request.crcValid);
        assertEquals("Request", request.frameKind);
        assertEquals("讀取 Holding Registers，起始位址 122 (0x007A)，數量 4", request.action);
        assertFalse(request.registerDataDecodable);
        assertEquals("", request.registerDataHex);

        PacketParseResult response = ModbusPacketParser.parseRtu("01 03 08 00 01 00 02 00 03 00 04 0D 14");
        assertTrue(response.crcValid);
        assertEquals("Response", response.frameKind);
        assertEquals("回覆 Holding Registers 資料，資料長度 8 bytes", response.action);
        assertEquals(4, response.registers.size());
        assertTrue(response.registerDataDecodable);
    }

    @Test
    public void parsesTcpReadMultipleRegistersRequestAndResponse() {
        PacketParseResult request = ModbusPacketParser.parseTcp("00 01 00 00 00 06 01 03 00 7A 00 04");
        assertEquals("Request", request.frameKind);
        assertEquals("讀取 Holding Registers，起始位址 122 (0x007A)，數量 4", request.action);
        assertEquals("正確，總長度 12 bytes", request.mbapLengthCheck);
        assertFalse(request.crcPresent);
        assertFalse(request.registerDataDecodable);

        PacketParseResult response = ModbusPacketParser.parseTcp("00 01 00 00 00 0B 01 03 08 00 01 00 02 00 03 00 04");
        assertEquals("Response", response.frameKind);
        assertEquals("回覆 Holding Registers 資料，資料長度 8 bytes", response.action);
        assertEquals(4, response.registers.size());
        assertTrue(response.registerDataDecodable);
    }

    @Test
    public void parsesTcpWriteMultipleRegistersRequestAndResponse() {
        PacketParseResult request = ModbusPacketParser.parseTcp("00 02 00 00 00 0B 01 10 00 7A 00 02 04 42 48 00 00");
        assertEquals("Request", request.frameKind);
        assertEquals("寫入多個 Holding Registers，起始位址 122 (0x007A)，數量 2，資料 42480000", request.action);
        assertEquals(2, request.registers.size());

        PacketParseResult response = ModbusPacketParser.parseTcp("00 02 00 00 00 06 01 10 00 7A 00 02");
        assertEquals("Response", response.frameKind);
        assertEquals("回覆寫入多個 Holding Registers，起始位址 122 (0x007A)，數量 2", response.action);
    }

    @Test
    public void parsesTcpReadAndWriteMultipleCoils() {
        PacketParseResult readRequest = ModbusPacketParser.parseTcp("00 03 00 00 00 06 01 01 00 10 00 10");
        assertEquals("Request", readRequest.frameKind);
        assertEquals("讀取 Coils，起始位址 16 (0x0010)，數量 16", readRequest.action);

        PacketParseResult readResponse = ModbusPacketParser.parseTcp("00 03 00 00 00 05 01 01 02 CD 01");
        assertEquals("Response", readResponse.frameKind);
        assertEquals("回覆 Coils 狀態，資料長度 2 bytes，資料 CD01", readResponse.action);

        PacketParseResult writeRequest = ModbusPacketParser.parseTcp("00 04 00 00 00 09 01 0F 00 10 00 0A 02 CD 01");
        assertEquals("Request", writeRequest.frameKind);
        assertEquals("寫入多個 Coils，起始位址 16 (0x0010)，數量 10，資料 CD01", writeRequest.action);

        PacketParseResult writeResponse = ModbusPacketParser.parseTcp("00 04 00 00 00 06 01 0F 00 10 00 0A");
        assertEquals("Response", writeResponse.frameKind);
        assertEquals("回覆寫入多個 Coils，起始位址 16 (0x0010)，數量 10", writeResponse.action);
    }

    @Test
    public void parsesTeachingExamples() {
        PacketParseResult rtuRead = ModbusPacketParser.parseRtu("01 03 00 00 00 02 C4 0B");
        assertTrue(rtuRead.crcValid);
        assertEquals("Request", rtuRead.frameKind);
        assertEquals("讀取 Holding Registers，起始位址 0 (0x0000)，數量 2", rtuRead.action);

        PacketParseResult rtuFloat = ModbusPacketParser.parseRtu("01 03 04 41 20 00 00 EF C5");
        assertTrue(rtuFloat.crcValid);
        assertEquals("Response", rtuFloat.frameKind);
        assertEquals("41200000", rtuFloat.registerDataHex);
        assertEquals(2, rtuFloat.registers.size());

        PacketParseResult fc06 = ModbusPacketParser.parseRtu("01 06 00 82 12 34 24 95");
        assertTrue(fc06.crcValid);
        assertEquals("Request / Response 皆可能", fc06.frameKind);
        assertEquals("寫入單一 Holding Register，位址 130 (0x0082)，值 4660 (0x1234)；此封包格式的 Request 與 Response 相同", fc06.action);

        PacketParseResult fc16 = ModbusPacketParser.parseRtu("01 10 00 7A 00 02 04 41 20 00 00 61 02");
        assertTrue(fc16.crcValid);
        assertEquals("41200000", fc16.registerDataHex);

        PacketParseResult exception = ModbusPacketParser.parseRtu("01 83 02 C0 F1");
        assertTrue(exception.crcValid);
        assertEquals("Exception Response", exception.frameKind);
        assertEquals("83", exception.functionCode);
        assertTrue(exception.action.contains("Illegal Data Address"));

        PacketParseResult tcpFloat = ModbusPacketParser.parseTcp("00 01 00 00 00 07 01 03 04 41 20 00 00");
        assertEquals("7", tcpFloat.length);
        assertEquals("Response", tcpFloat.frameKind);
        assertEquals("41200000", tcpFloat.registerDataHex);
        assertEquals(2, tcpFloat.registers.size());
    }

    @Test
    public void rejectsWrongTcpMbapLength() {
        PacketParseResult result = ModbusPacketParser.parseTcp("00 01 00 00 00 08 01 03 04 41 20 00 00");
        assertEquals("解析失敗", result.status);
        assertTrue(result.error.contains("MBAP Length"));
    }

    @Test
    public void packetSamplesAreFilteredByModeAndDirection() {
        List<ModbusPacketSamples.SamplePacket> rtuRequests = ModbusPacketSamples.filter(false, ModbusPacketSamples.Direction.REQUEST);
        List<ModbusPacketSamples.SamplePacket> tcpRequests = ModbusPacketSamples.filter(true, ModbusPacketSamples.Direction.REQUEST);
        List<ModbusPacketSamples.SamplePacket> rtuResponses = ModbusPacketSamples.filter(false, ModbusPacketSamples.Direction.RESPONSE);
        assertEquals(8, rtuRequests.size());
        assertEquals(8, tcpRequests.size());
        assertTrue(rtuRequests.stream().allMatch(sample -> !sample.tcp && sample.direction == ModbusPacketSamples.Direction.REQUEST));
        assertTrue(tcpRequests.stream().allMatch(sample -> sample.tcp && sample.direction == ModbusPacketSamples.Direction.REQUEST));
        assertTrue(rtuResponses.stream().anyMatch(sample -> (sample.functionCode & 0x80) != 0));
        assertFalse(rtuRequests.stream().anyMatch(sample -> (sample.functionCode & 0x80) != 0));
    }

    @Test
    public void allGeneratedSamplesPassTheirParser() {
        for (ModbusPacketSamples.SamplePacket sample : ModbusPacketSamples.all()) {
            PacketParseResult result = sample.tcp
                    ? ModbusPacketParser.parseTcp(sample.hex)
                    : ModbusPacketParser.parseRtu(sample.hex);
            assertEquals("sample failed: " + sample.name + " " + sample.hex, null, result.error);
            if (sample.tcp) {
                assertTrue(result.mbapLengthCheck.startsWith("正確"));
                assertFalse(result.crcPresent);
            } else {
                assertTrue("RTU CRC failed: " + sample.name, result.crcValid);
            }
            assertEquals(sample.expectedRegisterDataHex, result.registerDataHex);
        }
    }

    @Test
    public void fc03RequestDoesNotExposeRegisterDataButResponseDoes() {
        PacketParseResult request = ModbusPacketParser.parseRtu(ModbusPacketSamples.filter(false, ModbusPacketSamples.Direction.REQUEST).get(2).hex);
        assertEquals("Request", request.frameKind);
        assertFalse(request.registerDataDecodable);
        assertEquals("", request.registerDataHex);
        assertTrue(request.transferBlockedReason.contains("沒有"));

        PacketParseResult response = ModbusPacketParser.parseTcp(ModbusPacketSamples.filter(true, ModbusPacketSamples.Direction.RESPONSE).get(2).hex);
        assertEquals("Response", response.frameKind);
        assertTrue(response.registerDataDecodable);
        assertEquals("41200000", response.registerDataHex);
    }
}
