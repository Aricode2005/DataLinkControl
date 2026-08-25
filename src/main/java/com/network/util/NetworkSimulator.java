package com.network.util;

import com.network.util.error.ErrorInjector;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * Simulates a network channel with random delay, packet loss, and error injection.
 */
public class NetworkSimulator {
    private double lossProbability;
    private double errorProbability;
    private int maxDelayMs;
    private Random random;
    private ErrorInjector errorInjector;
    
    // Metrics tracking
    public int totalPacketsTransmitted = 0;
    public int totalPacketsDropped = 0;
    public int totalPacketsCorrupted = 0;
    public long totalDelayIncurred = 0;

    public NetworkSimulator(double lossProbability, double errorProbability, int maxDelayMs, ErrorInjector errorInjector) {
        this.lossProbability = lossProbability;
        this.errorProbability = errorProbability;
        this.maxDelayMs = maxDelayMs;
        this.random = new Random();
        this.errorInjector = errorInjector;
    }

    public void sendWithSimulation(DatagramSocket socket, Object obj, InetAddress address, int port) throws IOException {
        totalPacketsTransmitted++;
        
        // 1. Packet Loss
        if (random.nextDouble() < lossProbability) {
            totalPacketsDropped++;
            return;
        }

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        byte[] data = baos.toByteArray();

        // 2. Error Injection
        boolean corrupt = random.nextDouble() < errorProbability;
        if (corrupt && errorInjector != null) {
            totalPacketsCorrupted++;
            // Skip the standard Java serialization headers (usually ~20-30 bytes)
            // By starting corruption past the header, we avoid StreamCorruptedException
            int headerOffset = 27; 
            if (data.length > headerOffset) {
                byte[] payloadPart = new byte[data.length - headerOffset];
                System.arraycopy(data, headerOffset, payloadPart, 0, payloadPart.length);
                errorInjector.execute(payloadPart, payloadPart.length);
                System.arraycopy(payloadPart, 0, data, headerOffset, payloadPart.length);
            }
        }

        // 3. Delay
        int delay = random.nextInt(maxDelayMs + 1);
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
