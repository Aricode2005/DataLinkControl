package com.network.protocol;

import com.network.model.Frame;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;

import java.io.IOException;

public class StopAndWaitReceiver extends Receiver {
    private int expectedSeqNo = 0;

    public StopAndWaitReceiver(int localPort, NetworkSimulator channel) throws Exception {
        super(localPort, channel);
    }

    @Override
    protected void Recv(Frame frame) {
        System.out.println("[Receiver-SAW] Received frame " + frame.getSeqNo());
        if (!Check(frame)) {
            System.out.println("[Receiver-SAW] Frame corrupted, discarding.");
            return; // discard
        }

        if (frame.getSeqNo() == expectedSeqNo) {
            System.out.println("[Receiver-SAW] Frame accepted: " + new String(frame.getPayload()));
            expectedSeqNo = (expectedSeqNo + 1) % 2;
        } else {
            System.out.println("[Receiver-SAW] Duplicate frame received.");
        }

        try {
            Send(expectedSeqNo, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
