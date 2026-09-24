package com.network.protocol;
import com.network.model.Frame;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;
import java.io.IOException;

public class StopAndWaitReceiver extends Receiver {
    private int Rn = 0;

    public StopAndWaitReceiver(int localPort, NetworkSimulator channel) throws Exception {
        super(localPort, channel);
    }

    @Override
    protected void Recv(Frame frame) {
        log("[Receiver-SAW] Data frame arrives: " + frame.getSeqNo());
        if (!Check(frame)) {
            log("[Receiver-SAW] Frame corrupted, discarding (Sleep).");
            return;
        }
        
        int seqNo = frame.getSeqNo();
        if (seqNo == (Rn % 2)) { 
            log("[Receiver-SAW] Frame accepted: " + new String(frame.getPayload()));
            statBytesReceived += frame.getPayload().length;
            try { finalDocument.write(frame.getPayload()); } catch(Exception e){}
            
            Rn = Rn + 1;
            try {
                Send(Rn % 2, false); 
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            log("[Receiver-SAW] Duplicate frame received. seqNo=" + seqNo + ", expected=" + (Rn % 2));
            try {
                Send(Rn % 2, false); 
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}