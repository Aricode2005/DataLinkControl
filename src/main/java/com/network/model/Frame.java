package com.network.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents a Data Frame as specified in the assignment.
 */
public class Frame implements Serializable {
    private static final long serialVersionUID = 1L;

    // Header (12 bytes conceptually)
    private byte[] sourceAddress = new byte[6];
    private byte[] destinationAddress = new byte[6];
    private int length; // 2 bytes
    private int seqNo;  // 1 byte

    // Data
    private byte[] payload; // 46-1500 bytes

    // Trailer
    private long fcs; // Frame Check Sequence (CRC/Checksum) - 4 bytes conceptually

    public Frame(byte[] sourceAddress, byte[] destinationAddress, int seqNo, byte[] payload) {
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.seqNo = seqNo;
        this.payload = payload;
        this.length = payload.length;
    }

    public byte[] getSourceAddress() { return sourceAddress; }
    public byte[] getDestinationAddress() { return destinationAddress; }
    public int getLength() { return length; }
    public int getSeqNo() { return seqNo; }
    public byte[] getPayload() { return payload; }
    public long getFcs() { return fcs; }

    public void setFcs(long fcs) { this.fcs = fcs; }
    
    // Simulating corruption for testing
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
