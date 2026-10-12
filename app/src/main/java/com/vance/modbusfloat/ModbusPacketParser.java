package com.vance.modbusfloat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ModbusPacketParser {
    private ModbusPacketParser() {
    }

    public static PacketParseResult parseRtu(String hex) {
        PacketParseResult result = new PacketParseResult();
        result.tcp = false;
        try {
            byte[] bytes = parseHexBytes(hex);
            if (bytes.length < 5) {
                throw new IllegalArgumentException("RTU 封包至少需要 Slave ID、Function 與 CRC");
            }
            result.slaveId = unsignedText(bytes[0]);
            result.functionCode = hexByte(bytes[1]);
            if (bytes.length >= 4) {
                result.crcPresent = true;
                int crcOffset = bytes.length - 2;
                int received = (bytes[crcOffset] & 0xFF) | ((bytes[crcOffset + 1] & 0xFF) << 8);
                int calculated = crc16(bytes, 0, crcOffset);
                result.crcReceived = String.format(Locale.US, "%04X", received);
                result.crcCalculated = String.format(Locale.US, "%04X", calculated);
                result.crcValid = received == calculated;
            }
            parseRtuPdu(bytes, 0, bytes.length - 2, result);
            result.status = result.crcPresent ? (result.crcValid ? "CRC 正確" : "CRC 錯誤") : "未包含 CRC";
        } catch (Exception ex) {
            result.error = ex.getMessage();
            result.status = "解析失敗";
        }
        return result;
    }

    public static PacketParseResult parseTcp(String hex) {
        PacketParseResult result = new PacketParseResult();
        result.tcp = true;
        try {
            byte[] bytes = parseHexBytes(hex);
            if (bytes.length < 9) {
                throw new IllegalArgumentException("TCP 封包至少需要 MBAP Header 與 Function / Byte Count");
            }
            result.transactionId = String.valueOf(u16(bytes, 0));
            result.protocolId = String.valueOf(u16(bytes, 2));
            result.length = String.valueOf(u16(bytes, 4));
            result.unitId = unsignedText(bytes[6]);
            result.functionCode = hexByte(bytes[7]);
            if (u16(bytes, 2) != 0) {
                throw new IllegalArgumentException("Protocol ID 必須為 0，實際值=" + result.protocolId);
            }
            int expectedLength = 6 + u16(bytes, 4);
            if (bytes.length != expectedLength) {
                result.mbapLengthCheck = "錯誤，預期總長度 " + expectedLength + " bytes，實際 " + bytes.length + " bytes";
                throw new IllegalArgumentException("MBAP Length 與封包實際長度不一致，Length=" + result.length + "，實際 PDU 長度=" + (bytes.length - 6));
            }
            result.mbapLengthCheck = "正確，總長度 " + bytes.length + " bytes";
            parseTcpPdu(bytes, result);
            result.status = "TCP 解析完成";
        } catch (Exception ex) {
            result.error = ex.getMessage();
            result.status = "解析失敗";
        }
        return result;
    }

    public static PacketParseResult fromRegisters(int register1, int register2) {
        PacketParseResult result = new PacketParseResult();
        result.status = "Register 解析完成";
        result.registers.add(register1 & 0xFFFF);
        result.registers.add(register2 & 0xFFFF);
        result.registerFormats.addAll(DataTypeConverter.registerFormats(new int[]{register1, register2}));
        return result;
    }

    public static int crc16(byte[] bytes, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= bytes[i] & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    public static byte[] parseHexBytes(String hex) {
        String compact = hex == null ? "" : hex.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.US);
        if (compact.length() == 0 || compact.length() % 2 != 0) {
            throw new IllegalArgumentException("HEX 字串必須是偶數長度");
        }
        byte[] bytes = new byte[compact.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(compact.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    public static String summarize(PacketParseResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append(result.tcp ? "Modbus TCP" : "Modbus RTU").append('\n');
        builder.append("Status: ").append(result.status).append('\n');
        if (result.error != null) {
            builder.append("Error: ").append(result.error).append('\n');
        }
        if (result.tcp) {
            builder.append("Transaction ID: ").append(result.transactionId).append('\n');
            builder.append("Protocol ID: ").append(result.protocolId).append('\n');
            builder.append("Length: ").append(result.length).append('\n');
            builder.append("Unit ID: ").append(result.unitId).append('\n');
        } else {
            builder.append("Slave ID: ").append(result.slaveId).append('\n');
        }
        builder.append("Frame: ").append(result.frameKind).append('\n');
        builder.append("Action: ").append(result.action).append('\n');
        builder.append("Function: ").append(result.functionCode).append('\n');
        builder.append("Byte Count: ").append(result.byteCount).append('\n');
        builder.append("Data: ").append(result.registerDataHex).append('\n');
        if (result.crcPresent) {
            builder.append("CRC Received: ").append(result.crcReceived).append('\n');
            builder.append("CRC Calculated: ").append(result.crcCalculated).append('\n');
        }
        return builder.toString();
    }

    private static void parseRtuPdu(byte[] bytes, int pduStart, int pduEndExclusive, PacketParseResult result) {
        int function = bytes[pduStart + 1] & 0xFF;
        if ((function & 0x80) != 0 && pduEndExclusive - pduStart == 3) {
            parseException(bytes[pduStart + 1], bytes[pduStart + 2], result);
            return;
        }
        if ((function == 0x03 || function == 0x04) && pduEndExclusive - pduStart == 6) {
            result.frameKind = "Request";
            int address = u16(bytes, pduStart + 2);
            int quantity = u16(bytes, pduStart + 4);
            result.action = (function == 0x03 ? "讀取 Holding Registers" : "讀取 Input Registers")
                    + "，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            setNoRegisterData(result, "讀取 Request 只有起始位址與數量，沒有可轉入資料解碼的 Register Data。");
            return;
        }
        if (function == 0x05 && pduEndExclusive - pduStart == 6) {
            result.frameKind = "Request / Response 皆可能";
            int address = u16(bytes, pduStart + 2);
            int value = u16(bytes, pduStart + 4);
            result.action = "寫入單一 Coil，位址 " + address + " (0x" + hexWord(address) + ")，值 0x" + hexWord(value)
                    + "；此封包格式的 Request 與 Response 相同";
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Write Value", "0x" + hexWord(value));
            setNoRegisterData(result, "FC05 是 Coil 寫入，不包含 16-bit Register Data。");
            return;
        }
        if (function == 0x06 && pduEndExclusive - pduStart == 6) {
            result.frameKind = "Request / Response 皆可能";
            int address = u16(bytes, pduStart + 2);
            int value = u16(bytes, pduStart + 4);
            result.action = "寫入單一 Holding Register，位址 " + address + " (0x" + hexWord(address) + ")，值 " + value + " (0x" + hexWord(value)
                    + ")；此封包格式的 Request 與 Response 相同";
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Write Value", value + " (0x" + hexWord(value) + ")");
            result.registers.add(value);
            result.registerFormats.addAll(DataTypeConverter.registerFormats(new int[]{value}));
            setRegisterData(result, hexWord(value), "FC06 單一 Register 寫入值");
            return;
        }
        if (function == 0x10 && pduEndExclusive - pduStart >= 7) {
            result.frameKind = "Request";
            int address = u16(bytes, pduStart + 2);
            int quantity = u16(bytes, pduStart + 4);
            int byteCount = bytes[pduStart + 6] & 0xFF;
            if (byteCount != quantity * 2) {
                throw new IllegalArgumentException("Byte Count 與 Register 數量不一致");
            }
            int dataStart = pduStart + 7;
            int dataEnd = dataStart + byteCount;
            if (dataEnd > pduEndExclusive) {
                throw new IllegalArgumentException("Byte Count 超出封包長度");
            }
            result.byteCount = String.valueOf(byteCount);
            result.action = "寫入多個 Holding Registers，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity + "，資料 " + bytesHex(bytes, dataStart, dataEnd);
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            parseRegisters(bytes, dataStart, dataEnd, result);
            result.registerDataSource = "FC16 Request 寫入的 Register Data";
            result.registerDataDecodable = true;
            return;
        }
        if (function == 0x10 && pduEndExclusive - pduStart == 6) {
            result.frameKind = "Response";
            int address = u16(bytes, pduStart + 2);
            int quantity = u16(bytes, pduStart + 4);
            result.action = "回覆寫入多個 Holding Registers，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            setNoRegisterData(result, "FC16 Response 只確認起始位址與數量，不包含寫入資料內容。");
            return;
        }
        if (function == 0x0F && pduEndExclusive - pduStart >= 7) {
            result.frameKind = "Request";
            int address = u16(bytes, pduStart + 2);
            int quantity = u16(bytes, pduStart + 4);
            int byteCount = bytes[pduStart + 6] & 0xFF;
            int dataStart = pduStart + 7;
            int dataEnd = dataStart + byteCount;
            if (dataEnd > pduEndExclusive) {
                throw new IllegalArgumentException("Byte Count 超出封包長度");
            }
            result.byteCount = String.valueOf(byteCount);
            String coilData = bytesHex(bytes, dataStart, dataEnd);
            result.action = "寫入多個 Coils，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity + "，資料 " + coilData;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            addDetail(result, "Coil Data", coilData);
            setNoRegisterData(result, "FC15 是 Coil 寫入資料，不是 Register Data。");
            return;
        }
        if (function == 0x0F && pduEndExclusive - pduStart == 6) {
            result.frameKind = "Response";
            int address = u16(bytes, pduStart + 2);
            int quantity = u16(bytes, pduStart + 4);
            result.action = "回覆寫入多個 Coils，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            setNoRegisterData(result, "FC15 Response 只確認起始位址與數量，不包含 Coil 實際資料。");
            return;
        }
        if ((function == 0x01 || function == 0x02) && pduEndExclusive - pduStart == 6) {
            result.frameKind = "Request";
            int address = u16(bytes, pduStart + 2);
            int quantity = u16(bytes, pduStart + 4);
            result.action = (function == 0x01 ? "讀取 Coils" : "讀取 Discrete Inputs") + "，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            setNoRegisterData(result, "讀取 Coil/Input Request 只有位址與數量，沒有 Register Data。");
            return;
        }
        if ((function == 0x01 || function == 0x02) && pduEndExclusive - pduStart >= 3) {
            result.frameKind = "Response";
            int byteCount = bytes[pduStart + 2] & 0xFF;
            int dataStart = pduStart + 3;
            int dataEnd = dataStart + byteCount;
            if (dataEnd > pduEndExclusive) {
                throw new IllegalArgumentException("Byte Count 超出封包長度");
            }
            result.byteCount = String.valueOf(byteCount);
            String data = bytesHex(bytes, dataStart, dataEnd);
            result.action = (function == 0x01 ? "回覆 Coils 狀態" : "回覆 Discrete Inputs 狀態") + "，資料長度 " + byteCount + " bytes，資料 " + data;
            addDetail(result, function == 0x01 ? "Coil Data" : "Input Data", data);
            setNoRegisterData(result, "Coil/Input 狀態資料不是 Register Data。");
            return;
        }
        if ((function == 0x03 || function == 0x04) && pduEndExclusive - pduStart >= 3) {
            result.frameKind = "Response";
            int byteCount = bytes[pduStart + 2] & 0xFF;
            if ((byteCount % 2) != 0) {
                throw new IllegalArgumentException("Register 回應 Byte Count 必須為偶數");
            }
            result.byteCount = String.valueOf(byteCount);
            int dataStart = pduStart + 3;
            int dataEnd = dataStart + byteCount;
            if (dataEnd > pduEndExclusive) {
                throw new IllegalArgumentException("Byte Count 超出封包長度");
            }
            result.action = (function == 0x03 ? "回覆 Holding Registers 資料" : "回覆 Input Registers 資料") + "，資料長度 " + byteCount + " bytes";
            parseRegisters(bytes, dataStart, dataEnd, result);
            result.registerDataSource = function == 0x03 ? "FC03 Response Register Data" : "FC04 Response Register Data";
            result.registerDataDecodable = true;
            return;
        }
        result.frameKind = "Unknown";
        result.action = "尚未支援此 Function Code 的動作解析";
    }

    private static void parseTcpPdu(byte[] bytes, PacketParseResult result) {
        int function = bytes[7] & 0xFF;
        if ((function & 0x80) != 0 && bytes.length == 9) {
            parseException(bytes[7], bytes[8], result);
            return;
        }
        if ((function == 0x03 || function == 0x04) && bytes.length == 12) {
            result.frameKind = "Request";
            int address = u16(bytes, 8);
            int quantity = u16(bytes, 10);
            result.action = (function == 0x03 ? "讀取 Holding Registers" : "讀取 Input Registers")
                    + "，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            setNoRegisterData(result, "讀取 Request 只有起始位址與數量，沒有可轉入資料解碼的 Register Data。");
            return;
        }
        if (function == 0x05 && bytes.length == 12) {
            result.frameKind = "Request / Response 皆可能";
            int address = u16(bytes, 8);
            int value = u16(bytes, 10);
            result.action = "寫入單一 Coil，位址 " + address + " (0x" + hexWord(address) + ")，值 0x" + hexWord(value)
                    + "；此封包格式的 Request 與 Response 相同";
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Write Value", "0x" + hexWord(value));
            setNoRegisterData(result, "FC05 是 Coil 寫入，不包含 16-bit Register Data。");
            return;
        }
        if (function == 0x06 && bytes.length >= 12) {
            result.frameKind = "Request / Response 皆可能";
            int address = u16(bytes, 8);
            int value = u16(bytes, 10);
            result.action = "寫入單一 Holding Register，位址 " + address + " (0x" + hexWord(address) + ")，值 " + value + " (0x" + hexWord(value)
                    + ")；此封包格式的 Request 與 Response 相同";
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Write Value", value + " (0x" + hexWord(value) + ")");
            result.registers.add(value);
            result.registerFormats.addAll(DataTypeConverter.registerFormats(new int[]{value}));
            setRegisterData(result, hexWord(value), "FC06 單一 Register 寫入值");
            return;
        }
        if (function == 0x10 && bytes.length >= 14) {
            result.frameKind = "Request";
            int address = u16(bytes, 8);
            int quantity = u16(bytes, 10);
            int byteCount = bytes[12] & 0xFF;
            if (byteCount != quantity * 2) {
                throw new IllegalArgumentException("Byte Count 與 Register 數量不一致");
            }
            int dataStart = 13;
            int dataEnd = dataStart + byteCount;
            if (dataEnd > bytes.length) {
                throw new IllegalArgumentException("Byte Count 超出封包長度");
            }
            result.byteCount = String.valueOf(byteCount);
            result.registerDataHex = bytesHex(bytes, dataStart, dataEnd);
            result.action = "寫入多個 Holding Registers，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity + "，資料 " + result.registerDataHex;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            parseRegisters(bytes, dataStart, dataEnd, result);
            result.registerDataSource = "FC16 Request 寫入的 Register Data";
            result.registerDataDecodable = true;
            return;
        }
        if (function == 0x10 && bytes.length == 12) {
            result.frameKind = "Response";
            int address = u16(bytes, 8);
            int quantity = u16(bytes, 10);
            result.action = "回覆寫入多個 Holding Registers，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            setNoRegisterData(result, "FC16 Response 只確認起始位址與數量，不包含寫入資料內容。");
            return;
        }
        if (function == 0x0F && bytes.length >= 14) {
            result.frameKind = "Request";
            int address = u16(bytes, 8);
            int quantity = u16(bytes, 10);
            int byteCount = bytes[12] & 0xFF;
            int dataStart = 13;
            int dataEnd = dataStart + byteCount;
            if (dataEnd > bytes.length) {
                throw new IllegalArgumentException("Byte Count 超出封包長度");
            }
            result.byteCount = String.valueOf(byteCount);
            String coilData = bytesHex(bytes, dataStart, dataEnd);
            result.action = "寫入多個 Coils，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity + "，資料 " + coilData;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            addDetail(result, "Coil Data", coilData);
            setNoRegisterData(result, "FC15 是 Coil 寫入資料，不是 Register Data。");
            return;
        }
        if (function == 0x0F && bytes.length == 12) {
            result.frameKind = "Response";
            int address = u16(bytes, 8);
            int quantity = u16(bytes, 10);
            result.action = "回覆寫入多個 Coils，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            setNoRegisterData(result, "FC15 Response 只確認起始位址與數量，不包含 Coil 實際資料。");
            return;
        }
        if ((function == 0x01 || function == 0x02) && bytes.length == 12) {
            result.frameKind = "Request";
            int address = u16(bytes, 8);
            int quantity = u16(bytes, 10);
            result.action = (function == 0x01 ? "讀取 Coils" : "讀取 Discrete Inputs") + "，起始位址 " + address + " (0x" + hexWord(address) + ")，數量 " + quantity;
            addDetail(result, "Address", address + " (0x" + hexWord(address) + ")");
            addDetail(result, "Quantity", String.valueOf(quantity));
            setNoRegisterData(result, "讀取 Coil/Input Request 只有位址與數量，沒有 Register Data。");
            return;
        }
        if ((function == 0x01 || function == 0x02) && bytes.length >= 9) {
            result.frameKind = "Response";
            int byteCount = bytes[8] & 0xFF;
            int dataStart = 9;
            int dataEnd = dataStart + byteCount;
            if (dataEnd > bytes.length) {
                throw new IllegalArgumentException("Byte Count 超出封包長度");
            }
            result.byteCount = String.valueOf(byteCount);
            String data = bytesHex(bytes, dataStart, dataEnd);
            result.action = (function == 0x01 ? "回覆 Coils 狀態" : "回覆 Discrete Inputs 狀態") + "，資料長度 " + byteCount + " bytes，資料 " + data;
            addDetail(result, function == 0x01 ? "Coil Data" : "Input Data", data);
            setNoRegisterData(result, "Coil/Input 狀態資料不是 Register Data。");
            return;
        }
        if ((function == 0x03 || function == 0x04) && bytes.length >= 9) {
            result.frameKind = "Response";
            int byteCount = bytes[8] & 0xFF;
            if ((byteCount % 2) != 0) {
                throw new IllegalArgumentException("Register 回應 Byte Count 必須為偶數");
            }
            result.byteCount = String.valueOf(byteCount);
            int dataStart = 9;
            int dataEnd = dataStart + byteCount;
            if (dataEnd > bytes.length) {
                throw new IllegalArgumentException("Byte Count 超出封包長度");
            }
            result.action = (function == 0x03 ? "回覆 Holding Registers 資料" : "回覆 Input Registers 資料") + "，資料長度 " + byteCount + " bytes";
            parseRegisters(bytes, dataStart, dataEnd, result);
            result.registerDataSource = function == 0x03 ? "FC03 Response Register Data" : "FC04 Response Register Data";
            result.registerDataDecodable = true;
            return;
        }
        result.frameKind = "Unknown";
        result.action = "尚未支援此 Function Code 的動作解析";
    }

    private static void parseRegisters(byte[] bytes, int dataStart, int dataEnd, PacketParseResult result) {
        StringBuilder hex = new StringBuilder();
        List<Integer> registers = new ArrayList<>();
        for (int i = dataStart; i < dataEnd; i++) {
            hex.append(hexByte(bytes[i]));
        }
        result.registerDataHex = hex.toString();
        for (int i = dataStart; i + 1 < dataEnd; i += 2) {
            registers.add(u16(bytes, i));
        }
        result.registers.addAll(registers);
        int[] values = new int[registers.size()];
        for (int i = 0; i < registers.size(); i++) {
            values[i] = registers.get(i);
        }
        result.registerFormats.addAll(DataTypeConverter.registerFormats(values));
    }

    private static void setRegisterData(PacketParseResult result, String hex, String source) {
        result.registerDataHex = hex;
        result.registerDataSource = source;
        result.registerDataDecodable = hex != null && hex.length() >= 4 && hex.length() % 4 == 0;
        if (!result.registerDataDecodable) {
            result.transferBlockedReason = "Register Data 長度不足或不是完整 16-bit Register。";
        }
    }

    private static void setNoRegisterData(PacketParseResult result, String reason) {
        result.registerDataHex = "";
        result.registerDataSource = "";
        result.registerDataDecodable = false;
        result.transferBlockedReason = reason;
    }

    private static void parseException(byte function, byte exceptionCode, PacketParseResult result) {
        int originalFunction = (function & 0xFF) & 0x7F;
        int code = exceptionCode & 0xFF;
        result.frameKind = "Exception Response";
        result.action = "異常回應，原 Function Code " + hexByte((byte) originalFunction)
                + "，Exception Code " + hexByte(exceptionCode) + "：" + exceptionDescription(code);
        addDetail(result, "Exception Function Code", hexByte(function));
        addDetail(result, "Original Function Code", hexByte((byte) originalFunction));
        addDetail(result, "Exception Code", hexByte(exceptionCode));
        addDetail(result, "Exception 說明", exceptionDescription(code));
    }

    private static String exceptionDescription(int code) {
        switch (code) {
            case 1:
                return "Illegal Function，設備不支援此 Function Code";
            case 2:
                return "Illegal Data Address，資料位址不存在或不可讀寫";
            case 3:
                return "Illegal Data Value，資料值或數量不合法";
            case 4:
                return "Slave Device Failure，從站設備處理失敗";
            default:
                return "未列出的 Modbus Exception Code";
        }
    }

    private static int u16(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    private static String unsignedText(byte value) {
        return String.valueOf(value & 0xFF);
    }

    private static String hexByte(byte value) {
        return String.format(Locale.US, "%02X", value & 0xFF);
    }

    private static String hexWord(int value) {
        return String.format(Locale.US, "%04X", value & 0xFFFF);
    }

    private static String bytesHex(byte[] bytes, int start, int end) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; i++) {
            builder.append(hexByte(bytes[i]));
        }
        return builder.toString();
    }

    private static void addDetail(PacketParseResult result, String key, String value) {
        result.details.add(new String[]{key, value});
    }
}
