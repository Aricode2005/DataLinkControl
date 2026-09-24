package com.network.protocol;

import com.network.model.Frame;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;

import java.io.IOException;

public class SelectiveRepeatReceiver extends Receiver {
    private int Rn = 0;
    private boolean NakSent = false;
    private boolean AckNeeded = false;
    private boolean[] Marked = new boolean[Frame.MAX_SEQ];
    private Frame[] bufferArr = new Frame[Frame.MAX_SEQ];
    private int windowSize;

    public SelectiveRepeatReceiver(int localPort, NetworkSimulator channel, int windowSize) throws Exception {
        super(localPort, channel);
        this.windowSize = windowSize;
        for (int i = 0; i < Frame.MAX_SEQ; i++) {
            Marked[i] = false;
        }
    }

    private int getAbsoluteSeq(int seqNoMod, int base) {
        int baseMod = base % Frame.MAX_SEQ;
        int diff = seqNoMod - baseMod;
        if (diff < 0) diff += Frame.MAX_SEQ;
        int abs = base + diff;
        if (abs >= base + windowSize) {
            abs -= Frame.MAX_SEQ;
        }
        return abs;
    }

    @Override
    protected void Recv(Frame frame) {
        log("[Receiver-SR] Data frame arrives: " + frame.getSeqNo());
        
        if (!Check(frame)) {
            log("[Receiver-SR] Frame corrupted.");
            if (!NakSent) {
                try { Send(Rn % Frame.MAX_SEQ, true); } catch (IOException e) {} // SendNAK(Rn)
                NakSent = true;
            }
            return;
        }

        int seqNo = frame.getSeqNo();
        
        if (seqNo != (Rn % Frame.MAX_SEQ) && !NakSent) {
            log("[Receiver-SR] Out of order. seqNo=" + seqNo + ", Rn=" + (Rn % Frame.MAX_SEQ));
            try { Send(Rn % Frame.MAX_SEQ, true); } catch (IOException e) {} // SendNAK(Rn)
            NakSent = true;
        }

        int absSeqNo = getAbsoluteSeq(seqNo, Rn);
        boolean inWindow = (absSeqNo >= Rn && absSeqNo < Rn + windowSize);
        
        if (inWindow && !Marked[seqNo]) {
            bufferArr[seqNo] = frame;
            Marked[seqNo] = true;
            
            while (Marked[Rn % Frame.MAX_SEQ]) {
                Frame f = bufferArr[Rn % Frame.MAX_SEQ];
                log("[Receiver-SR] Frame delivered: " + new String(f.getPayload()));
                statBytesReceived += f.getPayload().length;
                try { finalDocument.write(f.getPayload()); } catch(Exception e){}
                
                Marked[Rn % Frame.MAX_SEQ] = false;
                bufferArr[Rn % Frame.MAX_SEQ] = null;
                
                Rn = Rn + 1;
                AckNeeded = true;
            }
            
            if (AckNeeded) {
                try { Send(Rn % Frame.MAX_SEQ, false); } catch (IOException e) {} // SendAck(Rn)
                AckNeeded = false;
                NakSent = false;
            }
        }
    }
}