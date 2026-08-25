package com.network.protocol;

import com.network.model.Ack;
import com.network.model.Frame;
import com.network.sender.Sender;
import com.network.util.NetworkSimulator;

import java.io.IOException;

public class StopAndWaitSender extends Sender {
    private boolean waitingForAck = false;
    private Frame currentFrame;
    private int expectedAck = 1;
    
    private final Object lock = new Object();

    public StopAndWaitSender(int localPort, String receiverIp, int receiverPort, NetworkSimulator channel) throws Exception {
        super(localPort, receiverIp, receiverPort, channel);
    }

    @Override
    public void Send(byte[][] dataChunks) throws Exception {
        int seqNo = 0;
        for (byte[] data : dataChunks) {
            seqNo = (seqNo + 1) % 2; // SeqNo alternates 0 and 1
            currentFrame = Framing(seqNo, data);
            
            sendCurrentFrame();
            
            synchronized (lock) {
                while (waitingForAck) {
                    lock.wait(); // Wait until ACK is received
                }
            }
        }
        System.out.println("[Sender-SAW] All frames sent successfully.");
    }
    
    private void sendCurrentFrame() throws IOException {
        System.out.println("[Sender-SAW] Sending frame " + currentFrame.getSeqNo());
        waitingForAck = true;
        expectedAck = (currentFrame.getSeqNo() + 1) % 2;
        Timer(currentFrame.getSeqNo());
        Channel(currentFrame);
    }

    @Override
    protected void Recv(Ack ack) {
        System.out.println("[Sender-SAW] Received " + ack);
        if (ack.getAckNo() == expectedAck) {
            updateRTT();
            stopTimer(currentFrame.getSeqNo());
            waitingForAck = false;
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    @Override
    protected void handleTimeout(int seqNo) {
        if (waitingForAck) {
            System.out.println("[Sender-SAW] Retransmitting frame " + currentFrame.getSeqNo());
            try {
                sendCurrentFrame();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
