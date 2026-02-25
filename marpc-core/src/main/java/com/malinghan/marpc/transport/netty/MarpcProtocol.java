package com.malinghan.marpc.transport.netty;

public class MarpcProtocol {
    public static final byte MAGIC_1 = (byte) 0xAA;
    public static final byte MAGIC_2 = (byte) 0xBB;
    public static final byte VERSION = 0x01;
    public static final byte TYPE_REQUEST  = 0x01;
    public static final byte TYPE_RESPONSE = 0x02;
    public static final int HEADER_LENGTH  = 12;
}
