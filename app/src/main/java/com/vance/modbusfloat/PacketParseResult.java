package com.vance.modbusfloat;

import java.util.ArrayList;
import java.util.List;

public final class PacketParseResult {
    public boolean tcp;
    public String status;
    public String error;
    public String frameKind = "";
    public String action = "";
    public String transactionId = "";
    public String protocolId = "";
    public String length = "";
    public String unitId = "";
    public String slaveId = "";
    public String functionCode = "";
    public String byteCount = "";
    public String registerDataHex = "";
    public String registerDataSource = "";
    public String transferBlockedReason = "";
    public boolean registerDataDecodable;
    public String crcReceived = "";
    public String crcCalculated = "";
    public boolean crcPresent;
    public boolean crcValid;
    public String mbapLengthCheck = "";
    public final List<String[]> details = new ArrayList<>();
    public final List<Integer> registers = new ArrayList<>();
    public final List<RegisterFormat> registerFormats = new ArrayList<>();
}
