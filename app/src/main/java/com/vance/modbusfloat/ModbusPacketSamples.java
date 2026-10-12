package com.vance.modbusfloat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ModbusPacketSamples {
    public enum Direction {
        REQUEST("Request", "主站請求"),
        RESPONSE("Response", "從站回應");

        public final String label;
        public final String zh;

        Direction(String label, String zh) {
            this.label = label;
            this.zh = zh;
        }
    }

    public static final class SamplePacket {
        public final String name;
        public final boolean tcp;
        public final Direction direction;
        public final int functionCode;
        public final String hex;
        public final String purpose;
        public final String expected;
        public final String expectedRegisterDataHex;

        private SamplePacket(String name, boolean tcp, Direction direction, int functionCode,
                             String hex, String purpose, String expected, String expectedRegisterDataHex) {
            this.name = name;
            this.tcp = tcp;
            this.direction = direction;
            this.functionCode = functionCode;
            this.hex = hex;
            this.purpose = purpose;
            this.expected = expected;
            this.expectedRegisterDataHex = expectedRegisterDataHex == null ? "" : expectedRegisterDataHex;
        }
    }

    private ModbusPacketSamples() {
    }

    public static List<SamplePacket> all() {
        List<SamplePacket> samples = new ArrayList<>();
        addMode(samples, false);
        addMode(samples, true);
        return samples;
    }

    public static List<SamplePacket> filter(boolean tcp, Direction direction) {
        List<SamplePacket> filtered = new ArrayList<>();
        for (SamplePacket sample : all()) {
            if (sample.tcp == tcp && sample.direction == direction) {
                filtered.add(sample);
            }
        }
        return filtered;
    }

    private static void addMode(List<SamplePacket> samples, boolean tcp) {
        String mode = tcp ? "TCP" : "RTU";
        add(samples, tcp, Direction.REQUEST, 0x01, "FC01 讀取 Coils Request", requestPdu(0x01),
                "讀取 16 個 Coils，自位址 0x0010 開始。",
                "起始位址：0x0010\nCoil 數量：16\n不包含 Register Data。", "");
        add(samples, tcp, Direction.REQUEST, 0x02, "FC02 讀取 Discrete Inputs Request", requestPdu(0x02),
                "讀取 16 個 Discrete Inputs，自位址 0x0020 開始。",
                "起始位址：0x0020\nInput 數量：16\n不包含 Register Data。", "");
        add(samples, tcp, Direction.REQUEST, 0x03, "FC03 讀取 Holding Registers Request", requestPdu(0x03),
                "讀取 2 個 Holding Registers，自位址 0x0000 開始。",
                "起始位址：0x0000\nRegister 數量：2\n不包含 Register Data。", "");
        add(samples, tcp, Direction.REQUEST, 0x04, "FC04 讀取 Input Registers Request", requestPdu(0x04),
                "讀取 2 個 Input Registers，自位址 0x0000 開始。",
                "起始位址：0x0000\nRegister 數量：2\n不包含 Register Data。", "");
        add(samples, tcp, Direction.REQUEST, 0x05, "FC05 寫入單一 Coil Request", pdu(0x05, 0x00, 0x10, 0xFF, 0x00),
                "寫入 Coil 0x0010 為 ON。封包格式與正常 Response 相同，單一封包方向具歧義。",
                "位址：0x0010\n寫入值：0xFF00\nRequest / Response 皆可能。", "");
        add(samples, tcp, Direction.REQUEST, 0x06, "FC06 寫入單一 Register Request", pdu(0x06, 0x00, 0x82, 0x12, 0x34),
                "寫入 Holding Register 0x0082，值 0x1234。封包格式與正常 Response 相同。",
                "位址：0x0082\n寫入值：0x1234\n可轉入資料解碼。", "1234");
        add(samples, tcp, Direction.REQUEST, 0x0F, "FC15 寫入多個 Coils Request", pdu(0x0F, 0x00, 0x10, 0x00, 0x0A, 0x02, 0xCD, 0x01),
                "寫入 10 個 Coils，自位址 0x0010 開始。",
                "起始位址：0x0010\nCoil 數量：10\nByte Count：2\n不包含 Register Data。", "");
        add(samples, tcp, Direction.REQUEST, 0x10, "FC16 寫入多個 Registers Request", pdu(0x10, 0x00, 0x7A, 0x00, 0x02, 0x04, 0x41, 0x20, 0x00, 0x00),
                "寫入 2 個 Holding Registers，資料為 Float32 10.0。",
                "起始位址：0x007A\nRegister 數量：2\nRegister Data：41 20 00 00\n可轉入資料解碼。", "41200000");

        add(samples, tcp, Direction.RESPONSE, 0x01, "FC01 讀取 Coils Response", pdu(0x01, 0x02, 0xCD, 0x01),
                "回應 16 個 Coils 狀態。",
                "Byte Count：2\nCoil Data：CD 01\n不包含 Register Data。", "");
        add(samples, tcp, Direction.RESPONSE, 0x02, "FC02 讀取 Discrete Inputs Response", pdu(0x02, 0x02, 0xAC, 0x00),
                "回應 16 個 Discrete Inputs 狀態。",
                "Byte Count：2\nInput Data：AC 00\n不包含 Register Data。", "");
        add(samples, tcp, Direction.RESPONSE, 0x03, "FC03 Holding Registers Response Float32", pdu(0x03, 0x04, 0x41, 0x20, 0x00, 0x00),
                "回應 2 個 Holding Registers，ABCD Float32 = 10.0。",
                "Byte Count：4\nRegister 1：0x4120\nRegister 2：0x0000\nABCD Float32：10.0", "41200000");
        add(samples, tcp, Direction.RESPONSE, 0x04, "FC04 Input Registers Response Float32", pdu(0x04, 0x04, 0x41, 0x20, 0x00, 0x00),
                "回應 2 個 Input Registers，ABCD Float32 = 10.0。",
                "Byte Count：4\nRegister 1：0x4120\nRegister 2：0x0000\nABCD Float32：10.0", "41200000");
        add(samples, tcp, Direction.RESPONSE, 0x05, "FC05 寫入單一 Coil Response", pdu(0x05, 0x00, 0x10, 0xFF, 0x00),
                "確認寫入 Coil 0x0010 為 ON。封包格式與 Request 相同。",
                "位址：0x0010\n寫入值：0xFF00\nRequest / Response 皆可能。", "");
        add(samples, tcp, Direction.RESPONSE, 0x06, "FC06 寫入單一 Register Response", pdu(0x06, 0x00, 0x82, 0x12, 0x34),
                "確認寫入 Holding Register 0x0082，值 0x1234。",
                "位址：0x0082\n寫入值：0x1234\n可轉入資料解碼。", "1234");
        add(samples, tcp, Direction.RESPONSE, 0x0F, "FC15 寫入多個 Coils Response", pdu(0x0F, 0x00, 0x10, 0x00, 0x0A),
                "確認寫入 10 個 Coils。",
                "起始位址：0x0010\nCoil 數量：10\n不包含 Register Data。", "");
        add(samples, tcp, Direction.RESPONSE, 0x10, "FC16 寫入多個 Registers Response", pdu(0x10, 0x00, 0x7A, 0x00, 0x02),
                "確認寫入 2 個 Holding Registers。",
                "起始位址：0x007A\nRegister 數量：2\nResponse 不包含 Register Data。", "");

        add(samples, tcp, Direction.RESPONSE, 0x81, "Exception Illegal Function", pdu(0x81, 0x01),
                mode + " 異常回應：非法 Function Code。",
                "Exception Function Code：81\nException Code：01\nIllegal Function。", "");
        add(samples, tcp, Direction.RESPONSE, 0x83, "Exception Illegal Data Address", pdu(0x83, 0x02),
                mode + " 異常回應：非法資料位址。",
                "Exception Function Code：83\nException Code：02\nIllegal Data Address。", "");
        add(samples, tcp, Direction.RESPONSE, 0x83, "Exception Illegal Data Value", pdu(0x83, 0x03),
                mode + " 異常回應：非法資料值。",
                "Exception Function Code：83\nException Code：03\nIllegal Data Value。", "");
    }

    private static void add(List<SamplePacket> samples, boolean tcp, Direction direction, int fc,
                            String name, byte[] pdu, String purpose, String expected, String registerData) {
        String hex = tcp ? tcpFrame(samples.size() + 1, 1, pdu) : rtuFrame(1, pdu);
        samples.add(new SamplePacket((tcp ? "TCP " : "RTU ") + name, tcp, direction, fc, hex, purpose, expected, registerData));
    }

    private static byte[] requestPdu(int fc) {
        int address = fc == 0x02 ? 0x20 : fc == 0x01 ? 0x10 : 0x00;
        int quantity = (fc == 0x03 || fc == 0x04) ? 0x02 : 0x10;
        return pdu(fc, (address >> 8) & 0xFF, address & 0xFF, (quantity >> 8) & 0xFF, quantity & 0xFF);
    }

    private static byte[] pdu(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    private static String rtuFrame(int slaveId, byte[] pdu) {
        byte[] frame = new byte[pdu.length + 3];
        frame[0] = (byte) slaveId;
        System.arraycopy(pdu, 0, frame, 1, pdu.length);
        int crc = ModbusPacketParser.crc16(frame, 0, pdu.length + 1);
        frame[frame.length - 2] = (byte) (crc & 0xFF);
        frame[frame.length - 1] = (byte) ((crc >> 8) & 0xFF);
        return hex(frame);
    }

    private static String tcpFrame(int transactionId, int unitId, byte[] pdu) {
        int length = pdu.length + 1;
        byte[] frame = new byte[7 + pdu.length];
        frame[0] = (byte) ((transactionId >> 8) & 0xFF);
        frame[1] = (byte) (transactionId & 0xFF);
        frame[2] = 0;
        frame[3] = 0;
        frame[4] = (byte) ((length >> 8) & 0xFF);
        frame[5] = (byte) (length & 0xFF);
        frame[6] = (byte) unitId;
        System.arraycopy(pdu, 0, frame, 7, pdu.length);
        return hex(frame);
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(String.format(Locale.US, "%02X", bytes[i] & 0xFF));
        }
        return builder.toString();
    }
}
