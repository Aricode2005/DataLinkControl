package com.network;
import com.network.model.Frame;
import com.network.protocol.*;
import com.network.sender.Sender;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;
import com.network.util.error.ErrorInjector;
import java.io.FileWriter;
import java.io.PrintWriter;

public class WindowSizeAnalyzer {
    private static int portCounter = 20000;
    public static void main(String[] args) throws Exception {
        PrintWriter csv = new PrintWriter(new FileWriter("window_size_metrics.csv"));
        csv.println("Protocol,WindowSize,Efficiency(%),AvgRTT(ms)");
        byte[][] data = new byte[30][];
        for (int i = 0; i < 30; i++) data[i] = new byte[10];
        int[] gbnSizes = {2, 4, 8, 12, 15};
        for (int n : gbnSizes) {
            runAndLog(csv, "GBN", n, data);
        }
        int[] srSizes = {2, 4, 6, 8};
        for (int n : srSizes) {
            runAndLog(csv, "SR", n, data);
        }
        csv.close();
        System.out.println("Window size metrics logged to window_size_metrics.csv");
        runFailureCase();
    }
    private static void runAndLog(PrintWriter csv, String protocol, int windowSize, byte[][] data) throws Exception {
        int rPort = portCounter++;
        NetworkSimulator sChannel = new NetworkSimulator(0.1, 0.0, 10, new ErrorInjector(new ErrorInjector.NoError()));
        NetworkSimulator rChannel = new NetworkSimulator(0.0, 0.0, 10, new ErrorInjector(new ErrorInjector.NoError()));
        Receiver receiver = null;
        if (protocol.equals("GBN")) receiver = new GoBackNReceiver(rPort, rChannel);
        else receiver = new SelectiveRepeatReceiver(rPort, rChannel, windowSize);
        receiver.startListening();
        int sPort = portCounter++;
        Sender senderTemp = null;
        if (protocol.equals("GBN")) senderTemp = new GoBackNSender(sPort, "127.0.0.1", receiver.getLocalPort(), sChannel, windowSize);
        else senderTemp = new SelectiveRepeatSender(sPort, "127.0.0.1", receiver.getLocalPort(), sChannel, windowSize);
        final Sender sender = senderTemp;
        sender.startListening();
        Thread senderThread = new Thread(() -> {
            try { sender.Send(data); } catch (Exception e) {}
        });
        senderThread.start();
        senderThread.join();
        Thread.sleep(100);
        int totalTransmitted = sChannel.totalPacketsTransmitted;
        double efficiency = (double) data.length / totalTransmitted * 100.0;
        long avgRTT = sender.rttSamples > 0 ? sender.totalRTT / sender.rttSamples : 0;
        csv.printf("%s,%d,%.1f,%d\n", protocol, windowSize, efficiency, avgRTT);
        sender.close();
        receiver.close();
    }
    private static void runFailureCase() throws Exception {
        PrintWriter out = new PrintWriter(new FileWriter("window_size_failures.txt"));
        out.println("=== Special Failure Cases ===");
        out.println("Max Seq (M_BITS=4) = 16");
        out.println("\n1. GBN Failure Case (N = 16)");
        out.println("If N = 16 (which is >= MAX_SEQ), the window perfectly overlaps the sequence space.");
        out.println("If a frame is severely delayed, the sender times out and re-sends the window.");
        out.println("The receiver eventually advances its expected sequence number and wraps around.");
        out.println("When the original delayed frame finally arrives, it has the exact sequence number the receiver is now expecting, causing Erroneous Acceptance.");
        out.println("\n2. SR Failure Case (N = 9)");
        out.println("If N = 9 (which is > MAX_SEQ / 2), the sender's window and receiver's window can overlap over the wrap-around boundary.");
        out.println("A delayed frame from the previous cycle can be erroneously accepted into the receiver's buffer for the new cycle because the sequence numbers are ambiguous.");
        out.println("\n(To see this in action, run Main.java interactively and enter these invalid window sizes when prompted. The network simulator's delayProbability will eventually trigger the glitch.)");
        out.close();
        System.out.println("Failure case descriptions logged to window_size_failures.txt");
    }
}
