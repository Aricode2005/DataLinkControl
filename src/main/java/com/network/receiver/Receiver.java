package com.network.receiver;
import com.network.model.Ack;
import com.network.model.Frame;
import com.network.util.CRCUtils;
import com.network.util.NetworkSimulator;
import java.io.*;
import java.net.*;

public abstract class Receiver {
    protected DatagramSocket socket;
    protected InetAddress senderAddress;
    protected int senderPort;
    protected NetworkSimulator channel;
    
    public static final long MAX_LIFETIME = 2500;

    public int statTotalFramesReceived = 0;
    public int statFramesCorrupted = 0;
    public int statFramesTooLate = 0;
    public int statBytesReceived = 0;
    public long statStartTime = 0;
    public long statEndTime = 0;

    public Receiver(int localPort, NetworkSimulator channel) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.channel = channel;
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    protected boolean Check(Frame frame) {
        boolean valid = CRCUtils.verifyFCS(frame);
        if (!valid) statFramesCorrupted++;
        return valid;
    }
    
    protected boolean isTooLate(long timestamp) {
        boolean late = (System.currentTimeMillis() - timestamp) > MAX_LIFETIME;
        if (late) statFramesTooLate++;
        return late;
    }

    protected void Send(int ackNo, boolean isNak) throws IOException {
        Ack ack = new Ack(ackNo, isNak);
        ack.setFcs(CRCUtils.calculateFCS(ack));
        ack.setTimestamp(System.currentTimeMillis());
        channel.sendWithSimulation(socket, ack, senderAddress, senderPort);
    }

    public void startListening() {
        new Thread(() -> {
            byte[] buf = new byte[2048];
            while (!socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    
                    if (statStartTime == 0) statStartTime = System.currentTimeMillis();
                    statEndTime = System.currentTimeMillis();

                    if (senderAddress == null) {
                        senderAddress = packet.getAddress();
                        senderPort = packet.getPort();
                    }

                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object obj = ois.readObject();
                    
                    if (obj instanceof Frame) {
                        statTotalFramesReceived++;
                        Frame f = (Frame) obj;
                        if (isTooLate(f.getTimestamp())) {
                            System.out.println("[Receiver] Frame " + f.getSeqNo() + " arrived too late, discarded.");
                            continue;
                        }
                        Recv(f);
                    }
                } catch (Exception e) {
                    if (!socket.isClosed()) e.printStackTrace();
                }
            }
        }).start();
    }
    
    public void printStats() {
        System.out.println("\n=== RECEIVER STATISTICS ===");
        System.out.println("Total Frames Received (inc. duplicates/corrupted): " + statTotalFramesReceived);
        System.out.println("Total Frames Originally Received Corrupted: " + statFramesCorrupted);
        System.out.println("Total Frames Received with Delay (Discarded): " + statFramesTooLate);
        System.out.println("Total Bytes Received/Delivered: " + statBytesReceived);
        System.out.println("Total Time Required: " + (statEndTime - statStartTime) + " ms");
        System.out.println("===========================\n");
    }

    public void close() {
        socket.close();
    }

    protected abstract void Recv(Frame frame);
}
