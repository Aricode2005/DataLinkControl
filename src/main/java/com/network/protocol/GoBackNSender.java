package com.network.protocol;
import com.network.model.Ack;
import com.network.model.Frame;
import com.network.sender.Sender;
import com.network.util.NetworkSimulator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoBackNSender extends Sender {
    private int windowSize;
    private int base = 0;
    private int nextSeqNum = 0;
    private List<Frame> frames = new ArrayList<>();
    private final Object lock = new Object();

    public GoBackNSender(int localPort, String receiverIp, int receiverPort, NetworkSimulator channel, int windowSize) throws Exception {
        super(localPort, receiverIp, receiverPort, channel);
        this.windowSize = windowSize;
    }

    @Override
    public void Send(byte[][] dataChunks) throws Exception {
        for (int i = 0; i < dataChunks.length; i++) {
            frames.add(Framing(i % Frame.MAX_SEQ, dataChunks[i])); 
        }
        while (base < frames.size()) {
            synchronized (lock) {
                while (nextSeqNum < base + windowSize && nextSeqNum < frames.size()) {
                    System.out.println("[Sender-GBN] Sending frame " + (nextSeqNum % Frame.MAX_SEQ));
                    Frame f = frames.get(nextSeqNum);
                    Channel(f);
                    if (base == nextSeqNum) {
                        Timer(base % Frame.MAX_SEQ);
                    }
                    nextSeqNum++;
                }
                lock.wait(currentTimeoutMs);
            }
        }
        System.out.println("[Sender-GBN] All frames sent successfully.");
    }

    private int getAbsoluteAck(int ackNo, int base) {
        int baseMod = base % Frame.MAX_SEQ;
        int diff = ackNo - baseMod;
        if (diff <= 0) diff += Frame.MAX_SEQ;
        int abs = base + diff;
        if (abs > base + windowSize) {
            abs -= Frame.MAX_SEQ;
        }
        return abs;
    }

    @Override
    protected void Recv(Ack ack) {
        System.out.println("[Sender-GBN] Received " + ack);
        synchronized (lock) {
            int ackNo = ack.getAckNo();
            int absAckNo = getAbsoluteAck(ackNo, base);
            if (absAckNo > base && absAckNo <= nextSeqNum) {
                Timeout();
                stopTimer(base % Frame.MAX_SEQ); 
                base = absAckNo;
                if (base < nextSeqNum) {
                    Timer(base % Frame.MAX_SEQ);
                }
                lock.notifyAll();
            }
        }
    }

    @Override
    protected void handleTimeout(int seqNo) {
        synchronized (lock) {
            if (seqNo == base % Frame.MAX_SEQ) {
                System.out.println("[Sender-GBN] Timeout for window base " + (base % Frame.MAX_SEQ) + ", retransmitting window.");
                nextSeqNum = base;
                lock.notifyAll();
            }
        }
    }
}
