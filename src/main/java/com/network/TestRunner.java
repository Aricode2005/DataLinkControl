package com.network;
import com.network.protocol.*;
import com.network.sender.Sender;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;
import com.network.util.error.ErrorInjector;
import java.io.FileWriter;
import java.io.PrintWriter;

public class TestRunner {
    private static int portCounter = 10000;
    public static void main(String[] args) throws Exception {
        PrintWriter out = new PrintWriter(new FileWriter("comparative_analysis.txt"), true);
        System.out.println("Starting Comparative Analysis... (This will take a few minutes under heavy network delay)");
        
        out.println("=== Data Link Layer Comparative Analysis ===");
        out.println("Sequence Space (m-bits): 4, MAX_SEQ: 16");
        byte[][] data = new byte[30][]; // Reduced from 50 to 30 to speed up 50% loss tests
        for (int i = 0; i < 30; i++) {
            data[i] = new byte[50]; 
        }
        
        System.out.println("\n--- 1. Base Case (0% Error, 0% Delay) ---");
        out.println("\n--- 1. Base Case (0% Error, 0% Delay) ---");
        runTest(out, "Stop-and-Wait", "SAW", 1, 0.0, 0.0, data);
        runTest(out, "Go-Back-N (N=7)", "GBN", 7, 0.0, 0.0, data);
        runTest(out, "Selective Repeat (N=7)", "SR", 7, 0.0, 0.0, data);
        
        System.out.println("\n--- 2. Efficiency vs Error Probability (0% Delay) ---");
        out.println("\n--- 2. Efficiency vs Error Probability (0% Delay) ---");
        for (double p = 0.1; p <= 0.5; p += 0.1) {
            System.out.printf("\n[Error Probability: %.1f]\n", p);
            out.printf("\n[Error Probability: %.1f]\n", p);
            runTest(out, "Stop-and-Wait", "SAW", 1, 0.0, p, data);
            runTest(out, "Go-Back-N (N=7)", "GBN", 7, 0.0, p, data);
            runTest(out, "Selective Repeat (N=7)", "SR", 7, 0.0, p, data);
        }
        
        System.out.println("\n--- 3. Efficiency vs Delay Probability (0% Error) ---");
        out.println("\n--- 3. Efficiency vs Delay Probability (0% Error) ---");
        for (double p = 0.1; p <= 0.5; p += 0.1) {
            System.out.printf("\n[Delay Probability: %.1f]\n", p);
            out.printf("\n[Delay Probability: %.1f]\n", p);
            runTest(out, "Stop-and-Wait", "SAW", 1, p, 0.0, data);
            runTest(out, "Go-Back-N (N=7)", "GBN", 7, p, 0.0, data);
            runTest(out, "Selective Repeat (N=7)", "SR", 7, p, 0.0, data);
        }
        
        out.close();
        System.out.println("\nAnalysis completely finished! All data saved to comparative_analysis.txt");
    }
    
    private static void runTest(PrintWriter out, String name, String type, int windowSize, double delayProb, double errProb, byte[][] data) throws Exception {
        System.out.print("  -> Testing " + name + "... ");
        int rPort = portCounter++;
        NetworkSimulator sChannel = new NetworkSimulator(delayProb, errProb, 10, new ErrorInjector(new ErrorInjector.SingleBitError()));
        NetworkSimulator rChannel = new NetworkSimulator(delayProb, errProb, 10, new ErrorInjector(new ErrorInjector.SingleBitError()));
        
        Receiver receiver = null;
        if (type.equals("SAW")) receiver = new StopAndWaitReceiver(rPort, rChannel);
        else if (type.equals("GBN")) receiver = new GoBackNReceiver(rPort, rChannel);
        else if (type.equals("SR")) receiver = new SelectiveRepeatReceiver(rPort, rChannel, windowSize);
        receiver.startListening();
        
        int sPort = portCounter++;
        Sender senderTemp = null;
        if (type.equals("SAW")) senderTemp = new StopAndWaitSender(sPort, "127.0.0.1", receiver.getLocalPort(), sChannel);
        else if (type.equals("GBN")) senderTemp = new GoBackNSender(sPort, "127.0.0.1", receiver.getLocalPort(), sChannel, windowSize);
        else if (type.equals("SR")) senderTemp = new SelectiveRepeatSender(sPort, "127.0.0.1", receiver.getLocalPort(), sChannel, windowSize);
        final Sender sender = senderTemp;
        sender.startListening();
        
        long start = System.currentTimeMillis();
        Thread senderThread = new Thread(() -> {
            try { sender.Send(data); } catch (Exception e) {}
        });
        senderThread.start();
        senderThread.join();
        Thread.sleep(100); 
        
        long end = System.currentTimeMillis();
        int totalTransmitted = sChannel.totalPacketsTransmitted;
        double efficiency = (double) data.length / totalTransmitted * 100.0;
        long avgRTT = sender.rttSamples > 0 ? sender.totalRTT / sender.rttSamples : 0;
        
        String result = String.format("%-25s | Efficiency: %5.1f%% | Avg RTT: %3d ms | Time: %4d ms", name, efficiency, avgRTT, (end - start));
        out.println(result);
        System.out.println("Done! (" + (end - start) + " ms)");
        
        sender.close();
        receiver.close();
    }
}
