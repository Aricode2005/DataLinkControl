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
            frames.add(Framing(i, dataChunks[i])); // In real GBN seq number wraps, but for sim we can just use index
        }
        
        while (base < frames.size()) {
            synchronized (lock) {
                while (nextSeqNum < base + windowSize && nextSeqNum < frames.size()) {
                    System.out.println("[Sender-GBN] Sending frame " + nextSeqNum);
                    Frame f = frames.get(nextSeqNum);
                    Channel(f);
                    if (base == nextSeqNum) {
                        Timer(base);
                    }
                    nextSeqNum++;
                }
                
                lock.wait(currentTimeoutMs); // wait for ACKs or timeout
            }
        }
        System.out.println("[Sender-GBN] All frames sent successfully.");
    }

    @Override
    protected void Recv(Ack ack) {
        System.out.println("[Sender-GBN] Received " + ack);
        synchronized (lock) {
            int ackNo = ack.getAckNo();
            if (ackNo > base) {
                updateRTT();
                base = ackNo;
                stopTimer(base); // although base has moved, we might need to restart timer if base < nextSeqNum
                if (base < nextSeqNum) {
                    Timer(base);
                }
                lock.notifyAll(); // wake up sending thread
            }
        }
    }

    @Override
    protected void handleTimeout(int seqNo) {
        synchronized (lock) {
            System.out.println("[Sender-GBN] Timeout for frame " + base + ", retransmitting window.");
            nextSeqNum = base;
            lock.notifyAll(); // Wake up to retransmit
        }
    }
}
