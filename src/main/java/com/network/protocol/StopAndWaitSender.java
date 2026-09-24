package com.network.protocol;
import com.network.model.Ack;
import com.network.model.Frame;
import com.network.sender.Sender;
import com.network.util.NetworkSimulator;
import java.io.IOException;

public class StopAndWaitSender extends Sender {
    private int Sn = 0;
    private boolean canSend = true;
    private Frame currentFrame;

    public StopAndWaitSender(int localPort, String receiverIp, int receiverPort, NetworkSimulator channel) throws Exception {
        super(localPort, receiverIp, receiverPort, channel);
    }

    @Override
    public void Send(byte[][] dataChunks) throws Exception {
        for (int i = 0; i < dataChunks.length; i++) {
            synchronized (lock) {
                while (!canSend) {
                    lock.wait(100);
                }
                
                log("[Sender-SAW] A packet to send");
                currentFrame = Framing(Sn % 2, dataChunks[i]);
                Channel(currentFrame);
                Timer(Sn % 2);
                
                Sn = Sn + 1;
                canSend = false;
            }
        }
        
        synchronized (lock) {
            while (!canSend) {
                lock.wait(100);
            }
        }
        
        log("[Sender-SAW] All frames sent successfully.");
    }

    @Override
    protected void Recv(Ack ack) {
        log("[Sender-SAW] ArrivalNotification: " + ack);
        synchronized (lock) {
            int ackNo = ack.getAckNo();
            if (ackNo == (Sn % 2)) { 
                for (java.util.Timer t : timers.values()) t.cancel(); timers.clear(); // StopTimer() 
                Timeout();
                // PurgeFrame(Sn - 1)
                canSend = true;
                lock.notifyAll();
            }
        }
    }

    @Override
    protected void handleTimeout(int seqNo) {
        synchronized (lock) {
            if (!canSend) { 
                log("[Sender-SAW] TimeOut, resending frame " + ((Sn - 1) % 2));
                Timer((Sn - 1) % 2);
                try {
                    Channel(currentFrame); 
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}