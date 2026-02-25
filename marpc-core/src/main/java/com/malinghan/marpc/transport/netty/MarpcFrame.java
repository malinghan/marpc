package com.malinghan.marpc.transport.netty;

import lombok.Data;

@Data
public class MarpcFrame {
    private byte type;
    private int sequenceId;
    private byte[] payload;

    public MarpcFrame(byte type, int sequenceId, byte[] payload) {
        this.type = type;
        this.sequenceId = sequenceId;
        this.payload = payload;
    }
}
