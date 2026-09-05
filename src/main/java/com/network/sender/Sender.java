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

public abstract class Sender {
    protected DatagramSocket socket;
    protected InetAddress receiverAddress;
    protected int receiverPort;
    protected NetworkSimulator channel;
    
    protected int currentTimeoutMs = 2000;
    protected ConcurrentHashMap<Integer, Timer> timers = new ConcurrentHashMap<>();
    protected long rttStart = -1;
    
    public int totalRetransmissions = 0;
    public long totalRTT = 0;
    public int rttSamples = 0;
    public long simulationStartTime = 0;
    public long simulationEndTime = 0;
    public static final long MAX_LIFETIME = 2500;

    public Sender(int localPort, String receiverIp, int receiverPort, NetworkSimulator channel) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.receiverAddress = InetAddress.getByName(receiverIp);
        this.receiverPort = receiverPort;
        this.channel = channel;
    }
    
    protected boolean isTooLate(long timestamp) {
        return (System.currentTimeMillis() - timestamp) > MAX_LIFETIME;
    }

    protected Frame Framing(int seqNo, byte[] payload) {
        byte[] srcMac = {0x00, 0x11, 0x22, 0x33, 0x44, 0x55};
        byte[] destMac = {0x66, 0x77, (byte)0x88, (byte)0x99, (byte)0xAA, (byte)0xBB};
        
        Frame frame = new Frame(srcMac, destMac, seqNo, payload);
        frame.setFcs(CRCUtils.calculateFCS(frame));
        return frame;
    }

    protected void Channel(Object obj) throws IOException {
        if (obj instanceof Frame) {
            ((Frame) obj).setTimestamp(System.currentTimeMillis());
            ((Frame) obj).incrementTransmissions();
        }
        channel.sendWithSimulation(socket, obj, receiverAddress, receiverPort);
    }

    protected void Timer(int seqNo) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTimerExpiration(seqNo);
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

    protected void onTimerExpiration(int seqNo) {
        totalRetransmissions++;
        currentTimeoutMs = Math.min(currentTimeoutMs * 2, 5000); 
        handleTimeout(seqNo);
    }

    protected void Timeout() {
        if (rttStart != -1) {
            long rtt = System.currentTimeMillis() - rttStart;
            totalRTT += rtt;
            rttSamples++;
            currentTimeoutMs = (int) (0.8 * currentTimeoutMs + 0.2 * rtt);
            rttStart = -1;
        }
    }

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
                        if (isTooLate(ack.getTimestamp())) {
                            continue;
                        }
                        if (!CRCUtils.verifyFCS(ack)) {
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

    public abstract void Send(byte[][] dataChunks) throws Exception;
    protected abstract void Recv(Ack ack);
    protected abstract void handleTimeout(int seqNo);
}
