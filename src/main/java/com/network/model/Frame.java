package com.network.model;

import java.io.Serializable;

public class Frame implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final int M_BITS = 4;
    public static final int MAX_SEQ = 1 << M_BITS;

    private byte[] sourceAddress = new byte[6];
    private byte[] destinationAddress = new byte[6];
    private int length;
    private int seqNo;
    private byte[] payload;
    private long fcs;
    private long timestamp;

    public Frame(byte[] sourceAddress, byte[] destinationAddress, int seqNo, byte[] payload) {
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.seqNo = seqNo;
        this.payload = payload;
        this.length = payload.length;
        this.timestamp = System.currentTimeMillis();
    }

    public byte[] getSourceAddress() { return sourceAddress; }
    public byte[] getDestinationAddress() { return destinationAddress; }
    public int getLength() { return length; }
    public int getSeqNo() { return seqNo; }
    public byte[] getPayload() { return payload; }
    public long getFcs() { return fcs; }
    public void setFcs(long fcs) { this.fcs = fcs; }
    public long getTimestamp() { return timestamp; }
    private int transmissions = 0;
    public int getTransmissions() { return transmissions; }
    public void incrementTransmissions() { transmissions++; } public void setTimestamp(long t) { this.timestamp = t; }
    
    public void corruptData() {
        if (payload != null && payload.length > 0) {
            payload[0] = (byte) ~payload[0];
        }
    }

    @Override
    public String toString() {
        return "Frame{" +
                "seqNo=" + seqNo +
                ", length=" + length +
                ", fcs=" + fcs +
                '}';
    }
}
