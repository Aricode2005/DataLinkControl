package com.network.protocol;

import com.network.model.Frame;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;

import java.io.IOException;

public class GoBackNReceiver extends Receiver {
    private int expectedSeqNo = 0;

    public GoBackNReceiver(int localPort, NetworkSimulator channel) throws Exception {
        super(localPort, channel);
    }

    @Override
    protected void Recv(Frame frame) {
        System.out.println("[Receiver-GBN] Received frame " + frame.getSeqNo());
        if (!Check(frame)) {
            System.out.println("[Receiver-GBN] Frame corrupted, discarding.");
            return;
        }

        if (frame.getSeqNo() == expectedSeqNo) {
            System.out.println("[Receiver-GBN] Frame accepted: " + new String(frame.getPayload()));
            expectedSeqNo++;
        } else {
            System.out.println("[Receiver-GBN] Out of order frame, expected " + expectedSeqNo + ", got " + frame.getSeqNo());
        }

        try {
            // GBN sends cumulative ACK for the next expected frame
            Send(expectedSeqNo, false); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
