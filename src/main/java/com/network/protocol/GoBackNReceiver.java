package com.network.protocol;
import com.network.model.Frame;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;
import java.io.IOException;

public class GoBackNReceiver extends Receiver {
    private int Rn = 0;

    public GoBackNReceiver(int localPort, NetworkSimulator channel) throws Exception {
        super(localPort, channel);
    }

    @Override
    protected void Recv(Frame frame) {
        log("[Receiver-GBN] Data frame arrives: " + frame.getSeqNo());
        
        if (!Check(frame)) {
            log("[Receiver-GBN] Frame corrupted, discarding (Sleep).");
            return;
        }
        
        int seqNo = frame.getSeqNo();
        if (seqNo == (Rn % Frame.MAX_SEQ)) {
            log("[Receiver-GBN] Frame accepted: " + new String(frame.getPayload()));
            statBytesReceived += frame.getPayload().length;
            try { finalDocument.write(frame.getPayload()); } catch(Exception e){}
            
            Rn = Rn + 1;
            try {
                Send(Rn % Frame.MAX_SEQ, false); 
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            log("[Receiver-GBN] Out of order frame, ignoring (Sleep). expected: " + (Rn % Frame.MAX_SEQ) + ", got: " + seqNo);
        }
    }
}