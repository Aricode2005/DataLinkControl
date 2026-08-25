package com.network.receiver;

import com.network.model.Ack;
import com.network.model.Frame;
import com.network.util.CRCUtils;
import com.network.util.NetworkSimulator;

import java.io.*;
import java.net.*;

/**
 * Abstract Receiver class defining the required methods.
 */
public abstract class Receiver {
    protected DatagramSocket socket;
    protected InetAddress senderAddress;
    protected int senderPort;
    protected NetworkSimulator channel;

    public Receiver(int localPort, NetworkSimulator channel) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.channel = channel;
    }

    /**
     * Checks if there is any error in data using FCS.
     */
    protected boolean Check(Frame frame) {
        return CRCUtils.verifyFCS(frame);
    }

    /**
     * Prepares an acknowledgement frame and sends it.
     */
    protected void Send(int ackNo, boolean isNak) throws IOException {
        Ack ack = new Ack(ackNo, isNak);
        ack.setFcs(CRCUtils.calculateFCS(ack));
        System.out.println("[Receiver] Sending " + ack);
        channel.sendWithSimulation(socket, ack, senderAddress, senderPort);
    }

    /**
     * Listening loop for incoming frames.
     */
    public void startListening() {
        new Thread(() -> {
            byte[] buf = new byte[2048];
            while (!socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    
                    // Capture sender address on first receive
                    if (senderAddress == null) {
                        senderAddress = packet.getAddress();
                        senderPort = packet.getPort();
                    }

                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object obj = ois.readObject();
                    
                    if (obj instanceof Frame) {
                        Recv((Frame) obj);
                    }
                } catch (Exception e) {
                    if (!socket.isClosed()) e.printStackTrace();
                }
            }
        }).start();
    }
    
    public void close() {
        socket.close();
    }

    // Abstract method to handle specific reception logic
    protected abstract void Recv(Frame frame);
}
