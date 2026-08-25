package com.network.protocol;

import com.network.model.Frame;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SelectiveRepeatReceiver extends Receiver {
    private int windowSize;
    private int base = 0;
    private Map<Integer, Frame> buffer = new HashMap<>();

    public SelectiveRepeatReceiver(int localPort, NetworkSimulator channel, int windowSize) throws Exception {
        super(localPort, channel);
        this.windowSize = windowSize;
    }

    @Override
    protected void Recv(Frame frame) {
        int seqNo = frame.getSeqNo();
        System.out.println("[Receiver-SR] Received frame " + seqNo);

        if (!Check(frame)) {
            System.out.println("[Receiver-SR] Frame corrupted, sending NAK.");
            try {
                Send(seqNo, true); // Send NAK
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (seqNo >= base && seqNo < base + windowSize) {
            System.out.println("[Receiver-SR] Frame " + seqNo + " buffered.");
            buffer.put(seqNo, frame);
            
            try {
                Send(seqNo, false); // Independent ACK
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Deliver frames and slide window
            while (buffer.containsKey(base)) {
                Frame f = buffer.remove(base);
                System.out.println("[Receiver-SR] Frame " + base + " accepted/delivered: " + new String(f.getPayload()));
                base++;
            }
        } else if (seqNo >= base - windowSize && seqNo < base) {
            // Already ACKed but ACK might have been lost
            System.out.println("[Receiver-SR] Received duplicate frame " + seqNo + ", re-ACKing.");
            try {
                Send(seqNo, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
