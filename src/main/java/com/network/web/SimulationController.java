package com.network.web;

import com.network.protocol.*;
import com.network.receiver.Receiver;
import com.network.sender.Sender;
import com.network.util.NetworkSimulator;
import com.network.util.error.ErrorInjector;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class SimulationController {

    private final Map<Integer, Receiver> activeReceivers = new ConcurrentHashMap<>();

    @PostMapping("/receiver/start")
    public Map<String, Object> startReceiver(@RequestParam String protocol, @RequestParam(defaultValue="1") int windowSize) throws Exception {
        NetworkSimulator channel = new NetworkSimulator(0.0, 0.0, 10, new ErrorInjector(new ErrorInjector.SingleBitError()));
        Receiver receiver = null;
        
        // Pass 0 to let OS assign an available random port! This allows multiple concurrent users on Railway!
        if (protocol.equals("SAW")) receiver = new StopAndWaitReceiver(0, channel);
        else if (protocol.equals("GBN")) receiver = new GoBackNReceiver(0, channel);
        else if (protocol.equals("SR")) receiver = new SelectiveRepeatReceiver(0, channel, windowSize);
        else throw new IllegalArgumentException("Invalid protocol");
        
        receiver.startListening();
        int sessionCode = receiver.getLocalPort();
        activeReceivers.put(sessionCode, receiver);
        
        Map<String, Object> res = new HashMap<>();
        res.put("sessionCode", sessionCode);
        return res;
    }

    @PostMapping("/sender/start")
    public Map<String, Object> startSender(
            @RequestParam int sessionCode,
            @RequestParam String protocol,
            @RequestParam(defaultValue="1") int windowSize,
            @RequestParam double errorProb,
            @RequestParam double delayProb,
            @RequestBody String payloadString) throws Exception {
            
        NetworkSimulator channel = new NetworkSimulator(errorProb, delayProb, 10, new ErrorInjector(new ErrorInjector.SingleBitError()));
        Sender sender = null;
        
        if (protocol.equals("SAW")) sender = new StopAndWaitSender(0, "127.0.0.1", sessionCode, channel);
        else if (protocol.equals("GBN")) sender = new GoBackNSender(0, "127.0.0.1", sessionCode, channel, windowSize);
        else if (protocol.equals("SR")) sender = new SelectiveRepeatSender(0, "127.0.0.1", sessionCode, channel, windowSize);
        
        sender.startListening();
        
        // Convert string payload to data chunks
        byte[] fileBytes = payloadString.getBytes();
        int chunkSize = 20; 
        int numChunks = (int) Math.ceil((double) fileBytes.length / chunkSize);
        byte[][] data = new byte[numChunks][];
        int totalBytes = 0;
        for (int i = 0; i < numChunks; i++) {
            int start = i * chunkSize;
            int length = Math.min(chunkSize, fileBytes.length - start);
            data[i] = new byte[length];
            System.arraycopy(fileBytes, start, data[i], 0, length);
            totalBytes += length;
        }

        final Sender finalSender = sender;
        final int finalTotalBytes = totalBytes;
        
        // Run simulation in background so UI doesn't hang!
        new Thread(() -> {
            try {
                System.out.println("=========================================");
                System.out.println("Starting Simulation (" + protocol + ")");
                System.out.println("Error: " + errorProb + " | Delay: " + delayProb);
                long start = System.currentTimeMillis();
                finalSender.Send(data);
                long end = System.currentTimeMillis();
                
                System.out.println("\n=== SENDER STATISTICS ===");
                System.out.println("Total Frames to Send: " + data.length);
                System.out.println("Total Frames Sent Corrupted: " + channel.totalPacketsCorrupted);
                System.out.println("Total Frames Delayed: " + channel.totalPacketsDelayed);
                System.out.println("Total Bytes: " + finalTotalBytes);
                System.out.println("Total Retransmissions: " + finalSender.totalRetransmissions);
                if (protocol.equals("GBN")) {
                    System.out.println("Cumulative ACKs Received: " + finalSender.totalCumulativeAcks);
                } else if (protocol.equals("SR")) {
                    System.out.println("NAKs Received: " + finalSender.totalNaksReceived);
                }
                System.out.println("Total Time Required: " + (end - start) + " ms");
                System.out.println("=========================\n");
                finalSender.close();
                
                // Also trigger the receiver's shutdown stats dynamically since Ctrl+C won't happen here
                Receiver rec = activeReceivers.get(sessionCode);
                if (rec != null) {
                    rec.printStats();
                    rec.close();
                    activeReceivers.remove(sessionCode);
                }
                
                System.out.println("SIMULATION_COMPLETE");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
        Map<String, Object> res = new HashMap<>();
        res.put("status", "started");
        return res;
    }
}