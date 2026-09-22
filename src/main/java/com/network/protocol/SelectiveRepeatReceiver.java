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
        int seqNoMod = frame.getSeqNo();
        log("[Receiver-SR] Received frame " + seqNoMod);
        if (!Check(frame)) {
            log("[Receiver-SR] Frame corrupted, sending NAK.");
            try {
                Send(seqNoMod, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        int absSeqNo = getAbsoluteSeq(seqNoMod, base);
        if (absSeqNo >= base && absSeqNo < base + windowSize) {
            log("[Receiver-SR] Frame " + (absSeqNo % Frame.MAX_SEQ) + " buffered.");
            buffer.put(absSeqNo, frame);
            try {
                Send(seqNoMod, false); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (buffer.containsKey(base)) {
                Frame f = buffer.remove(base);
                log("[Receiver-SR] Frame " + (base % Frame.MAX_SEQ) + " accepted/delivered: " + new String(f.getPayload()));
                statBytesReceived += f.getPayload().length;
                try { finalDocument.write(f.getPayload()); } catch(Exception e){}
                base++;
            }
        } else if (absSeqNo >= base - windowSize && absSeqNo < base) {
            log("[Receiver-SR] Received duplicate frame " + (absSeqNo % Frame.MAX_SEQ) + ", re-ACKing.");
            try {
                Send(seqNoMod, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
