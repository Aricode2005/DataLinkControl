package com.network.util;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * Simulates a network channel with random delay, packet loss, and bit errors.
 */
public class NetworkSimulator {
    private double lossProbability;
    private double errorProbability;
    private int maxDelayMs;
    private Random random;

    public NetworkSimulator(double lossProbability, double errorProbability, int maxDelayMs) {
        this.lossProbability = lossProbability;
        this.errorProbability = errorProbability;
        this.maxDelayMs = maxDelayMs;
        this.random = new Random();
    }

    public void sendWithSimulation(DatagramSocket socket, Object obj, InetAddress address, int port) throws IOException {
        // 1. Packet Loss
        if (random.nextDouble() < lossProbability) {
            System.out.println("[Channel] Packet dropped.");
            return;
        }

        // 2. Bit Error (Corruption)
        boolean corrupt = random.nextDouble() < errorProbability;

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        byte[] data = baos.toByteArray();

        if (corrupt) {
            System.out.println("[Channel] Packet corrupted.");
            // Flip a bit in the byte array to corrupt the payload
            if (data.length > 20) { // skip java serialization headers usually
                data[data.length - 1] = (byte) ~data[data.length - 1]; 
            }
        }

        // 3. Delay
        int delay = random.nextInt(maxDelayMs + 1);
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }
}
