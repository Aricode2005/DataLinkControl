package com.network;

import com.network.protocol.*;
import com.network.sender.Sender;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Data Link Layer Flow Control Simulation ===\n");
        
        int senderPort = 8000;
        int receiverPort = 8001;
        String ip = "127.0.0.1";
        
        // Data to send
        byte[][] data = {
            "Packet 0".getBytes(),
            "Packet 1".getBytes(),
            "Packet 2".getBytes(),
            "Packet 3".getBytes(),
            "Packet 4".getBytes()
        };

        // 1. Stop and Wait
        System.out.println("--- 1. Stop and Wait ---");
        runSimulation(
            new StopAndWaitSender(senderPort, ip, receiverPort, new NetworkSimulator(0.2, 0.2, 100)),
            new StopAndWaitReceiver(receiverPort, new NetworkSimulator(0.1, 0.1, 100)),
            data
        );
        
        Thread.sleep(2000);

        // 2. Go-Back-N ARQ
        System.out.println("\n--- 2. Go-Back-N ARQ (Window Size: 3) ---");
        runSimulation(
            new GoBackNSender(senderPort + 2, ip, receiverPort + 2, new NetworkSimulator(0.2, 0.2, 100), 3),
            new GoBackNReceiver(receiverPort + 2, new NetworkSimulator(0.1, 0.1, 100)),
            data
        );
        
        Thread.sleep(2000);
        
        // 3. Selective Repeat ARQ
        System.out.println("\n--- 3. Selective Repeat ARQ (Window Size: 3) ---");
        runSimulation(
            new SelectiveRepeatSender(senderPort + 4, ip, receiverPort + 4, new NetworkSimulator(0.2, 0.2, 100), 3),
            new SelectiveRepeatReceiver(receiverPort + 4, new NetworkSimulator(0.1, 0.1, 100), 3),
            data
        );
    }

    private static void runSimulation(Sender sender, Receiver receiver, byte[][] data) throws Exception {
        receiver.startListening();
        sender.startListening();

        Thread senderThread = new Thread(() -> {
            try {
                sender.Send(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        senderThread.start();
        senderThread.join(); // Wait for sender to finish
        
        // give a little time for final acks to arrive
        Thread.sleep(1000); 
        
        sender.close();
        receiver.close();
    }
}
