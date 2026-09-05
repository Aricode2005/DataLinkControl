package com.network.model;

import java.io.Serializable;

public class Ack implements Serializable {
    private static final long serialVersionUID = 1L;

    private int ackNo;
    private boolean isNak; 
    private long fcs;
    private long timestamp;

    public Ack(int ackNo, boolean isNak) {
        this.ackNo = ackNo;
        this.isNak = isNak;
        this.timestamp = System.currentTimeMillis();
    }

    public int getAckNo() { return ackNo; }
    public boolean isNak() { return isNak; }
    public long getFcs() { return fcs; }
    public void setFcs(long fcs) { this.fcs = fcs; }
    public long getTimestamp() { return timestamp; } public void setTimestamp(long t) { this.timestamp = t; }

    public void corruptData() {
        this.ackNo = -1;
    }

    @Override
    public String toString() {
        return (isNak ? "NAK{" : "ACK{") +
                "no=" + ackNo +
                '}';
    }
}
