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
        int seqNo = frame.getSeqNo();
        if (seqNo == (expectedSeqNo % Frame.MAX_SEQ)) {
            System.out.println("[Receiver-GBN] Frame accepted: " + new String(frame.getPayload()));
            statBytesReceived += frame.getPayload().length;
            expectedSeqNo++;
        } else {
            System.out.println("[Receiver-GBN] Out of order frame, expected " + (expectedSeqNo % Frame.MAX_SEQ) + ", got " + seqNo);
        }
        try {
            Send(expectedSeqNo % Frame.MAX_SEQ, false); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
