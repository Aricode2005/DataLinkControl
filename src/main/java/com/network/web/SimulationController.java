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
        
        if (protocol.equals("SAW")) receiver = new StopAndWaitReceiver(0, channel);
        else if (protocol.equals("GBN")) receiver = new GoBackNReceiver(0, channel);
        else if (protocol.equals("SR")) receiver = new SelectiveRepeatReceiver(0, channel, windowSize);
        else throw new IllegalArgumentException("Invalid protocol");
        
        receiver.startListening();
        int sessionCode = receiver.getLocalPort();
        activeReceivers.put(sessionCode, receiver);
        
        receiver.setLogger(new WebSocketLogger(sessionCode, false));
        
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
        
        sender.setLogger(new WebSocketLogger(sessionCode, true));
        sender.startListening();
        
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
        
        new Thread(() -> {
            try {
                WebSocketLogger log = new WebSocketLogger(sessionCode, true);
                log.log("=========================================");
                log.log("Starting Simulation (" + protocol + ")");
                log.log("Error: " + errorProb + " | Delay: " + delayProb);
                
                long start = System.currentTimeMillis();
                finalSender.Send(data);
                long end = System.currentTimeMillis();
                
                long timeRequired = end - start;
                int totalTransmitted = data.length + finalSender.totalRetransmissions;
                double efficiency = ((double) data.length / totalTransmitted) * 100.0;
                double avgRtt = timeRequired / (double) totalTransmitted;
                
                log.log("\n=== SENDER STATISTICS ===");
                log.log("Total Frames to Send: " + data.length);
                log.log("Total Frames Sent Corrupted: " + channel.totalPacketsCorrupted);
                log.log("Total Frames Delayed: " + channel.totalPacketsDelayed);
                log.log("Total Bytes: " + finalTotalBytes);
                log.log("Total Retransmissions: " + finalSender.totalRetransmissions);
                if (protocol.equals("GBN")) {
                    log.log("Cumulative ACKs Received: " + finalSender.totalCumulativeAcks);
                } else if (protocol.equals("SR")) {
                    log.log("NAKs Received: " + finalSender.totalNaksReceived);
                }
                log.log("Total Time Required: " + timeRequired + " ms");
                log.log(String.format("Average RTT: %.2f ms", avgRtt));
                log.log(String.format("Efficiency: %.2f%%", efficiency));
                log.log("=========================\n");
                log.log("SIMULATION_COMPLETE");
                
                finalSender.close();
                
                Receiver rec = activeReceivers.get(sessionCode);
                if (rec != null) {
                    rec.statEndTime = System.currentTimeMillis();
                    WebSocketLogger rLog = new WebSocketLogger(sessionCode, false);
                    rLog.log("\n=== RECEIVER STATISTICS ===");
                    rLog.log("Total Frames Received (inc. duplicates/corrupted): " + rec.statFramesReceived);
                    rLog.log("Total Frames Originally Received Corrupted: " + rec.statFramesCorrupted);
                    rLog.log("Total Frames Received with Delay (Discarded): " + rec.statFramesDelayed);
                    rLog.log("Total Bytes Received/Delivered: " + rec.statBytesReceived);
                    rLog.log("Total Time Required: " + (rec.statEndTime - rec.statStartTime) + " ms");
                    rLog.log("===========================\n");
                    rLog.log("=== ASSEMBLED DOCUMENT ===\n" + new String(rec.finalDocument.toByteArray()) + "\n==========================");
                    rLog.log("SIMULATION_COMPLETE");
                    rec.close();
                    activeReceivers.remove(sessionCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
        Map<String, Object> res = new HashMap<>();
        res.put("status", "started");
        return res;
    }
}