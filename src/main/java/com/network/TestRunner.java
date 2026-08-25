package com.network;

import com.network.protocol.*;
import com.network.sender.Sender;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;
import com.network.util.error.ErrorInjector;
import com.network.util.error.ErrorInjectionStrategy;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TestRunner {
    private static int portCounter = 9000;
    
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Comprehensive Testing...");
        
        try (PrintWriter writer = new PrintWriter(new FileWriter("simulation_results.txt"))) {
            writer.println("=================================================");
            writer.println("      DATA LINK FLOW CONTROL TEST REPORT         ");
            writer.println("=================================================");
            
            byte[][] data = generateData(10); // 10 frames
            
            // --- Case 1 & 2: Compare time and efficiency WITHOUT error ---
            writer.println("\n--- Case 1 & 2: NO ERROR / LOST FRAME ---");
            writer.println("Comparing efficiency and time without any errors.");
            
            runTest(writer, "Stop and Wait", "None", 0.0, 0.0, data, 1);
            runTest(writer, "Go-Back-N", "None", 0.0, 0.0, data, 1);
            runTest(writer, "Selective Repeat", "None", 0.0, 0.0, data, 1);

            // --- Case 3: Compare efficiency for different probabilities (0.1 - 0.5) ---
            writer.println("\n--- Case 3: VARYING ERROR PROBABILITY (0.1 to 0.5) ---");
            writer.println("Using SingleBitError Strategy");
            
            double[] probabilities = {0.1, 0.3, 0.5};
            for (double p : probabilities) {
                writer.println("\n>> Testing with Error Probability = " + p);
                
                runTest(writer, "Stop and Wait", "SingleBitError", p, p, data, 2);
                runTest(writer, "Go-Back-N", "SingleBitError", p, p, data, 2);
                runTest(writer, "Selective Repeat", "SingleBitError", p, p, data, 2);
            }
            
            writer.println("\n=================================================");
            writer.println("                  TEST COMPLETE                  ");
            writer.println("=================================================");
        }
        System.out.println("Testing complete. Results saved to simulation_results.txt");
    }

    private static void runTest(PrintWriter writer, String protocolName, String errorStrategy, double lossP, double errP, byte[][] data, int caseNo) throws Exception {
        int senderPort = portCounter++;
        int receiverPort = portCounter++;
        
        ErrorInjectionStrategy strategy;
        switch (errorStrategy) {
            case "SingleBitError": strategy = new ErrorInjector.SingleBitError(); break;
            case "BurstError": strategy = new ErrorInjector.BurstError(4); break;
            default: strategy = new ErrorInjector.NoError(); break;
        }

        ErrorInjector injector = new ErrorInjector(strategy);
        NetworkSimulator senderChannel = new NetworkSimulator(lossP, errP, 10, injector);
        NetworkSimulator receiverChannel = new NetworkSimulator(lossP, errP, 10, new ErrorInjector(new ErrorInjector.NoError())); // usually acks just get dropped/delayed

        Sender sender = null;
        Receiver receiver = null;

        if (protocolName.equals("Stop and Wait")) {
            sender = new StopAndWaitSender(senderPort, "127.0.0.1", receiverPort, senderChannel);
            receiver = new StopAndWaitReceiver(receiverPort, receiverChannel);
        } else if (protocolName.equals("Go-Back-N")) {
            sender = new GoBackNSender(senderPort, "127.0.0.1", receiverPort, senderChannel, 4);
            receiver = new GoBackNReceiver(receiverPort, receiverChannel);
        } else if (protocolName.equals("Selective Repeat")) {
            sender = new SelectiveRepeatSender(senderPort, "127.0.0.1", receiverPort, senderChannel, 4);
            receiver = new SelectiveRepeatReceiver(receiverPort, receiverChannel, 4);
        }

        receiver.startListening();
        sender.startListening();

        long startTime = System.currentTimeMillis();
        Sender finalSender = sender;
        Thread senderThread = new Thread(() -> {
            try {
                finalSender.Send(data);
            } catch (Exception e) {}
        });
        
        senderThread.start();
        senderThread.join();
        
        // Wait for last acks
        Thread.sleep(1000);
        long totalTime = System.currentTimeMillis() - startTime - 1000;
        
        sender.close();
        receiver.close();

        // Calculate metrics
        int usefulFrames = data.length;
        int totalTx = senderChannel.totalPacketsTransmitted;
        double efficiency = totalTx == 0 ? 0 : (double) usefulFrames / totalTx * 100;
        long avgRtt = sender.rttSamples == 0 ? 0 : sender.totalRTT / sender.rttSamples;
        
        writer.printf("Protocol: %-18s | Time Taken: %4d ms | Avg RTT: %3d ms | Eff: %5.2f%% | Retransmissions: %d\n", 
            protocolName, totalTime, avgRtt, efficiency, sender.totalRetransmissions);
        writer.flush();
    }
    
    private static byte[][] generateData(int frames) {
        byte[][] data = new byte[frames][];
        for (int i = 0; i < frames; i++) {
            data[i] = ("Payload Data " + i).getBytes();
        }
        return data;
    }
}
