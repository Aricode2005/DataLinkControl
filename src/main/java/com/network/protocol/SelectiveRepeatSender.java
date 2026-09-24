package com.network.protocol;
import com.network.model.Ack;
import com.network.model.Frame;
import com.network.sender.Sender;
import com.network.util.NetworkSimulator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SelectiveRepeatSender extends Sender {
    private int windowSize;
    private int base = 0;
    private int nextSeqNum = 0;
    private List<Frame> frames = new ArrayList<>();
    private boolean[] acked;
    private final Object lock = new Object();

    public SelectiveRepeatSender(int localPort, String receiverIp, int receiverPort, NetworkSimulator channel, int windowSize) throws Exception {
        super(localPort, receiverIp, receiverPort, channel);
        this.windowSize = windowSize;
    }

    @Override
    public void Send(byte[][] dataChunks) throws Exception {
        acked = new boolean[dataChunks.length];
        for (int i = 0; i < dataChunks.length; i++) {
            frames.add(Framing(i % Frame.MAX_SEQ, dataChunks[i]));
        }
        while (base < frames.size()) {
            synchronized (lock) {
                while (nextSeqNum < base + windowSize && nextSeqNum < frames.size()) {
                    log("[Sender-SR] Sending frame " + (nextSeqNum % Frame.MAX_SEQ));
                    Frame f = frames.get(nextSeqNum);
                    Channel(f);
                    Timer(nextSeqNum);
                    nextSeqNum++;
                }
                lock.wait(100);
            }
        }
        log("[Sender-SR] All frames sent successfully.");
    }

    private int getAbsoluteSeq(int seqNoMod, int base) {
        int baseMod = base % Frame.MAX_SEQ;
        int diff = seqNoMod - baseMod;
        if (diff <= 0) diff += Frame.MAX_SEQ; // Cumulative ACKs wrap forward like GBN
        int abs = base + diff;
        if (abs > base + windowSize) {
            abs -= Frame.MAX_SEQ;
        }
        return abs;
    }

    @Override
    protected void Recv(Ack ack) {
        log("[Sender-SR] Received " + ack);
        synchronized (lock) {
            int ackNoMod = ack.getAckNo();
            int absAckNo = getAbsoluteSeq(ackNoMod, base);
            
            // Cumulative logic for both ACK and NAK (means everything before absAckNo was successfully received)
            if (absAckNo > base && absAckNo <= nextSeqNum) {
                Timeout();
                while (base < absAckNo) {
                    acked[base] = true;
                    stopTimer(base);
                    base++;
                }
                lock.notifyAll();
            }

            if (ack.isNak()) {
                totalNaksReceived++; // TRACKING: NAK received
                // Retransmit the explicitly requested frame
                if (absAckNo >= base && absAckNo < nextSeqNum && !acked[absAckNo]) {
                    log("[Sender-SR] Received NAK for " + (absAckNo % Frame.MAX_SEQ) + ", retransmitting.");
                    try {
                        Channel(frames.get(absAckNo));
                        stopTimer(absAckNo);
                        Timer(absAckNo);
                    } catch (Exception e) {}
                }
            }
        }
    }

    @Override
    protected void handleTimeout(int seqNoAbs) {
        synchronized (lock) {
            if (seqNoAbs < acked.length && !acked[seqNoAbs]) {
                log("[Sender-SR] Timeout for frame " + (seqNoAbs % Frame.MAX_SEQ) + ", retransmitting selectively.");
                try {
                    Channel(frames.get(seqNoAbs));
                    Timer(seqNoAbs); 
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}