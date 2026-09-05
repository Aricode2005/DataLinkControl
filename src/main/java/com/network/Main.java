package com.network;
import com.network.model.Frame;
import com.network.protocol.*;
import com.network.sender.Sender;
import com.network.receiver.Receiver;
import com.network.util.NetworkSimulator;
import com.network.util.error.ErrorInjector;
import java.io.File;
import java.nio.file.Files;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        if (args.length > 0) {
            String role = args[0].toLowerCase();
            String protocol = args.length > 1 ? args[1].toUpperCase() : "SAW";
            int windowSize = 1;
            if (!protocol.equals("SAW")) {
                System.out.print("Enter window size N: ");
                windowSize = scanner.nextInt();
                if (protocol.equals("GBN") && windowSize > Frame.MAX_SEQ - 1) {
                    System.out.println("Warning: Window size exceeds 2^m - 1. Failures may occur.");
                }
                if (protocol.equals("SR") && windowSize > Frame.MAX_SEQ / 2) {
                    System.out.println("Warning: Window size exceeds 2^(m-1). Failures may occur.");
                }
            }
            runStandalone(role, protocol, windowSize);
            return;
        }
        System.out.println("Usage:");
        System.out.println("  Terminal 1: java -cp target com.network.Main receiver [SAW|GBN|SR]");
        System.out.println("  Terminal 2: java -cp target com.network.Main sender [SAW|GBN|SR]");
    }

    private static void runStandalone(String role, String protocol, int windowSize) throws Exception {
        int senderPort = 9000;
        int receiverPort = 9001;
        String ip = "127.0.0.1";
        NetworkSimulator channel = new NetworkSimulator(0.2, 0.1, 10, new ErrorInjector(new ErrorInjector.SingleBitError()));
        
        if (role.equals("receiver")) {
            Receiver receiver = null;
            if (protocol.equals("SAW")) receiver = new StopAndWaitReceiver(receiverPort, channel);
            else if (protocol.equals("GBN")) receiver = new GoBackNReceiver(receiverPort, channel);
            else if (protocol.equals("SR")) receiver = new SelectiveRepeatReceiver(receiverPort, channel, windowSize);
            else return;
            
            final Receiver finalRecv = receiver;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                finalRecv.printStats();
            }));

            receiver.startListening();
            System.out.println("Receiver is running. Press Ctrl+C to exit and view statistics.");
            while (true) {
                Thread.sleep(1000);
            }
        } else if (role.equals("sender")) {
            Sender sender = null;
            if (protocol.equals("SAW")) sender = new StopAndWaitSender(senderPort, ip, receiverPort, channel);
            else if (protocol.equals("GBN")) sender = new GoBackNSender(senderPort, ip, receiverPort, channel, windowSize);
            else if (protocol.equals("SR")) sender = new SelectiveRepeatSender(senderPort, ip, receiverPort, channel, windowSize);
            else return;
            
            sender.startListening();
            
            File inputFile = new File("input.txt");
            byte[][] data;
            int totalBytes = 0;
            if (inputFile.exists()) {
                byte[] fileBytes = Files.readAllBytes(inputFile.toPath());
                int chunkSize = 20; 
                int numChunks = (int) Math.ceil((double) fileBytes.length / chunkSize);
                data = new byte[numChunks][];
                for (int i = 0; i < numChunks; i++) {
                    int start = i * chunkSize;
                    int length = Math.min(chunkSize, fileBytes.length - start);
                    data[i] = new byte[length];
                    System.arraycopy(fileBytes, start, data[i], 0, length);
                    totalBytes += length;
                }
                System.out.println("Loaded " + numChunks + " chunks from input.txt");
            } else {
                data = new byte[20][];
                for (int i = 0; i < 20; i++) {
                    data[i] = ("Data " + i).getBytes();
                    totalBytes += data[i].length;
                }
                System.out.println("input.txt not found. Using default dummy data.");
            }
            
            System.out.println("Press Enter to send data...");
            new Scanner(System.in).nextLine();
            
            long start = System.currentTimeMillis();
            sender.Send(data);
            long end = System.currentTimeMillis();
            
            System.out.println("\n=== SENDER STATISTICS ===");
            System.out.println("Total Frames to Send: " + data.length);
            System.out.println("Total Frames Sent Corrupted: " + channel.totalPacketsCorrupted);
            System.out.println("Total Frames Delayed: " + channel.totalPacketsDelayed);
            System.out.println("Total Bytes: " + totalBytes);
            System.out.println("Total Retransmissions: " + sender.totalRetransmissions);
            System.out.println("Total Time Required: " + (end - start) + " ms");
            System.out.println("=========================\n");
            
            System.out.println("Finished. Press Enter to exit...");
            new Scanner(System.in).nextLine();
            sender.close();
            System.exit(0);
        }
    }
}
