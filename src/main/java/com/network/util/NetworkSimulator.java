package com.network.util;
import com.network.util.error.ErrorInjector;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class NetworkSimulator {
    private double delayProbability;
    private double errorProbability;
    private int maxDelayMs;
    private Random random;
    private ErrorInjector errorInjector;
    
    public int totalPacketsTransmitted = 0;
    public int totalPacketsCorrupted = 0;
    public int totalPacketsDelayed = 0;
    public long totalDelayIncurred = 0;

    public NetworkSimulator(double delayProbability, double errorProbability, int maxDelayMs, ErrorInjector errorInjector) {
        this.delayProbability = delayProbability;
        this.errorProbability = errorProbability;
        this.maxDelayMs = maxDelayMs;
        this.random = new Random();
        this.errorInjector = errorInjector;
    }

    public void sendWithSimulation(DatagramSocket socket, Object obj, InetAddress address, int port) throws IOException {
        totalPacketsTransmitted++;
        
        boolean isRetransmission = false;
        if (obj instanceof com.network.model.Frame) {
            if (((com.network.model.Frame) obj).getTransmissions() > 1) {
                isRetransmission = true;
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        byte[] data = baos.toByteArray();

        boolean corrupt = false;
        if (!isRetransmission) {
            corrupt = random.nextDouble() < errorProbability;
        }

        if (corrupt && errorInjector != null && !(errorInjector.getStrategy() instanceof ErrorInjector.NoError)) {
            totalPacketsCorrupted++;
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object clonedObj = ois.readObject();
                
                if (clonedObj instanceof com.network.model.Frame) {
                    com.network.model.Frame f = (com.network.model.Frame) clonedObj;
                    byte[] payload = f.getPayload();
                    if (payload != null && payload.length > 0) {
                        errorInjector.execute(payload, payload.length);
                    }
                } else if (clonedObj instanceof com.network.model.Ack) {
                    com.network.model.Ack a = (com.network.model.Ack) clonedObj;
                    a.setFcs(a.getFcs() ^ 1);
                }
                
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
                oos2.writeObject(clonedObj);
                oos2.flush();
                data = baos2.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int delay = 0;
        if (!isRetransmission && random.nextDouble() < delayProbability) {
            totalPacketsDelayed++;
            delay = maxDelayMs + random.nextInt(2000);
        } else {
            if (maxDelayMs > 0) {
                delay = random.nextInt(maxDelayMs / 2 + 1);
            }
        }

        if (delay > 0) {
            try {
                Thread.sleep(delay);
                totalDelayIncurred += delay;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }
}
