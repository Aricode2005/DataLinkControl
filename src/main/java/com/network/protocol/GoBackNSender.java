package com.network.protocol;
import com.network.model.Ack;
import com.network.model.Frame;
import com.network.sender.Sender;
import com.network.util.NetworkSimulator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoBackNSender extends Sender {
    private int Sw;
    private int Sf = 0;
    private int Sn = 0;
    private List<Frame> frames = new ArrayList<>();
    private final Object lock = new Object();

    public GoBackNSender(int localPort, String receiverIp, int receiverPort, NetworkSimulator channel, int windowSize) throws Exception {
        super(localPort, receiverIp, receiverPort, channel);
        this.Sw = windowSize;
    }

    @Override
    public void Send(byte[][] dataChunks) throws Exception {
        for (int i = 0; i < dataChunks.length; i++) {
            frames.add(Framing(i % Frame.MAX_SEQ, dataChunks[i])); 
        }
        while (Sf < frames.size()) {
            synchronized (lock) {
                while (Sn < Sf + Sw && Sn < frames.size()) {
                    log("[Sender-GBN] Sending frame " + (Sn % Frame.MAX_SEQ));
                    Frame f = frames.get(Sn);
                    Channel(f);
                    
                    if (Sf == Sn) { // Timer not running concept
                        Timer(Sf % Frame.MAX_SEQ);
                    }
                    
                    Sn = Sn + 1;
                }
                lock.wait(100);
            }
        }
        log("[Sender-GBN] All frames sent successfully.");
    }

    private int getAbsoluteAck(int ackNo, int base) {
        int baseMod = base % Frame.MAX_SEQ;
        int diff = ackNo - baseMod;
        if (diff <= 0) diff += Frame.MAX_SEQ;
        int abs = base + diff;
        if (abs > base + Sw) {
            abs -= Frame.MAX_SEQ;
        }
        return abs;
    }

    @Override
    protected void Recv(Ack ack) {
        log("[Sender-GBN] ACK arrives: " + ack);
        synchronized (lock) {
            int ackNoMod = ack.getAckNo();
            int ackNo = getAbsoluteAck(ackNoMod, Sf);
            
            if (ackNo > Sf && ackNo <= Sn) {
                totalCumulativeAcks++; // TRACKING: Valid Cumulative ACK
                Timeout();
                
                while (Sf < ackNo) {
                    // PurgeFrame(Sf)
                    Sf = Sf + 1;
                }
                
                for (java.util.Timer t : timers.values()) t.cancel(); timers.clear(); // StopTimer() 
                
                // Forouzan bug prevention: restart timer if frames still in flight
                if (Sf < Sn) {
                    Timer(Sf % Frame.MAX_SEQ);
                }
                
                lock.notifyAll();
            }
        }
    }

    @Override
    protected void handleTimeout(int seqNo) {
        synchronized (lock) {
            log("[Sender-GBN] TimeOut, retransmitting window starting from " + (Sf % Frame.MAX_SEQ));
            Timer(Sf % Frame.MAX_SEQ); // StartTimer()
            
            if (Sn - Sf > 1) {
                totalRetransmissions += (Sn - Sf - 1);
            }

            int Temp = Sf;
            while (Temp < Sn) {
                try {
                    Channel(frames.get(Temp)); // SendFrame(Temp)
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Temp = Temp + 1;
            }
        }
    }
}