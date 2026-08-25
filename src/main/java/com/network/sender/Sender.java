package com.network.sender;

import com.network.model.Ack;
import com.network.model.Frame;
import com.network.util.CRCUtils;
import com.network.util.NetworkSimulator;

import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract Sender class defining the required methods.
 */
public abstract class Sender {
    protected DatagramSocket socket;
    protected InetAddress receiverAddress;
    protected int receiverPort;
    protected NetworkSimulator channel;
    
    protected int currentTimeoutMs = 2000;
    protected ConcurrentHashMap<Integer, Timer> timers = new ConcurrentHashMap<>();
    protected long rttStart = -1;
    
    // Metrics
    public int totalRetransmissions = 0;
    public long totalRTT = 0;
    public int rttSamples = 0;
    public long simulationStartTime = 0;
    public long simulationEndTime = 0;

    public Sender(int localPort, String receiverIp, int receiverPort, NetworkSimulator channel) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.receiverAddress = InetAddress.getByName(receiverIp);
        this.receiverPort = receiverPort;
        this.channel = channel;
    }

    /**
     * Prepares the frame following the structure.
     */
    protected Frame Framing(int seqNo, byte[] payload) {
        byte[] srcMac = {0x00, 0x11, 0x22, 0x33, 0x44, 0x55};
        byte[] destMac = {0x66, 0x77, (byte)0x88, (byte)0x99, (byte)0xAA, (byte)0xBB};
        
        Frame frame = new Frame(srcMac, destMac, seqNo, payload);
        frame.setFcs(CRCUtils.calculateFCS(frame));
        return frame;
    }

    /**
     * Introduces random delay / error.
     */
    protected void Channel(Object obj) throws IOException {
        channel.sendWithSimulation(socket, obj, receiverAddress, receiverPort);
    }

    /**
     * Start timer for a specific frame sequence number.
     */
    protected void Timer(int seqNo) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Timeout(seqNo);
            }
        }, currentTimeoutMs);
        timers.put(seqNo, timer);
        if (rttStart == -1) rttStart = System.currentTimeMillis();
    }
    
    protected void stopTimer(int seqNo) {
        Timer timer = timers.remove(seqNo);
        if (timer != null) {
            timer.cancel();
        }
    }

    /**
     * Called on timeout to recompute timeout or just handle retransmission.
     */
    protected void Timeout(int seqNo) {
        totalRetransmissions++;
        currentTimeoutMs = Math.min(currentTimeoutMs * 2, 5000); 
        handleTimeout(seqNo);
    }

    protected void updateRTT() {
        if (rttStart != -1) {
            long rtt = System.currentTimeMillis() - rttStart;
            totalRTT += rtt;
            rttSamples++;
            currentTimeoutMs = (int) (0.8 * currentTimeoutMs + 0.2 * rtt); // smoothed RTT
            rttStart = -1;
        }
    }

    /**
     * Listening loop for incoming ACKs.
     */
    public void startListening() {
        new Thread(() -> {
            byte[] buf = new byte[1024];
            while (!socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    
                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object obj = ois.readObject();
                    
                    if (obj instanceof Ack) {
                        Ack ack = (Ack) obj;
                        if (!CRCUtils.verifyFCS(ack)) {
                            System.out.println("[Sender] Received corrupted ACK.");
                            continue;
                        }
                        Recv(ack);
                    }
                } catch (Exception e) {
                    if (!socket.isClosed()) e.printStackTrace();
                }
            }
        }).start();
    }

    public void close() {
        socket.close();
        timers.values().forEach(Timer::cancel);
    }

    // Abstract methods to be implemented by specific Flow Control strategies
    public abstract void Send(byte[][] dataChunks) throws Exception;
    protected abstract void Recv(Ack ack);
    protected abstract void handleTimeout(int seqNo);
}
