package com.network.model;

import java.io.Serializable;

/**
 * Represents an Acknowledgement frame.
 */
public class Ack implements Serializable {
    private static final long serialVersionUID = 1L;

    private int ackNo;
    private boolean isNak; // For Selective Repeat
    private long fcs;

    public Ack(int ackNo, boolean isNak) {
        this.ackNo = ackNo;
        this.isNak = isNak;
    }

    public int getAckNo() { return ackNo; }
    public boolean isNak() { return isNak; }
    public long getFcs() { return fcs; }

    public void setFcs(long fcs) { this.fcs = fcs; }

    public void corruptData() {
        // change ackNo to simulate corruption
        this.ackNo = -1;
    }

    @Override
    public String toString() {
        return (isNak ? "NAK{" : "ACK{") +
                "no=" + ackNo +
                '}';
    }
}
