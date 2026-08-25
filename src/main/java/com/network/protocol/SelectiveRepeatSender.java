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
            frames.add(Framing(i, dataChunks[i]));
        }

        while (base < frames.size()) {
            synchronized (lock) {
                while (nextSeqNum < base + windowSize && nextSeqNum < frames.size()) {
                    System.out.println("[Sender-SR] Sending frame " + nextSeqNum);
                    Frame f = frames.get(nextSeqNum);
                    Channel(f);
                    Timer(nextSeqNum);
                    nextSeqNum++;
                }
                lock.wait(100); // Polling mechanism with short wait to check window sliding
            }
        }
        System.out.println("[Sender-SR] All frames sent successfully.");
    }

    @Override
    protected void Recv(Ack ack) {
        System.out.println("[Sender-SR] Received " + ack);
        synchronized (lock) {
            int ackNo = ack.getAckNo();
            if (ack.isNak()) {
                System.out.println("[Sender-SR] Received NAK for " + ackNo + ", retransmitting.");
                try {
                    Channel(frames.get(ackNo));
                    stopTimer(ackNo);
                    Timer(ackNo);
                } catch (Exception e) {}
            } else {
                if (ackNo >= 0 && ackNo < acked.length && !acked[ackNo]) {
                    acked[ackNo] = true;
                    stopTimer(ackNo);
                    updateRTT();
                    
                    // Slide window
                    while (base < acked.length && acked[base]) {
                        base++;
                    }
                    lock.notifyAll();
                }
            }
        }
    }

    @Override
    protected void handleTimeout(int seqNo) {
        synchronized (lock) {
            if (!acked[seqNo]) {
                System.out.println("[Sender-SR] Timeout for frame " + seqNo + ", retransmitting selectively.");
                try {
                    Channel(frames.get(seqNo));
                    Timer(seqNo); // reset timer
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
