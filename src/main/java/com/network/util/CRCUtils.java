package com.network.util;

import com.network.model.Frame;
import com.network.model.Ack;
import java.util.zip.CRC32;


public class CRCUtils {

    public static long calculateFCS(Frame frame) {
        CRC32 crc = new CRC32();
        crc.update(frame.getSourceAddress());
        crc.update(frame.getDestinationAddress());
        crc.update(frame.getLength());
        crc.update(frame.getSeqNo());
        if (frame.getPayload() != null) {
            crc.update(frame.getPayload());
        }
        return crc.getValue();
    }

    public static boolean verifyFCS(Frame frame) {
        return calculateFCS(frame) == frame.getFcs();
    }

    public static long calculateFCS(Ack ack) {
        CRC32 crc = new CRC32();
        crc.update(ack.getAckNo());
        crc.update(ack.isNak() ? 1 : 0);
        return crc.getValue();
    }

  
    public static boolean verifyFCS(Ack ack) {
        return calculateFCS(ack) == ack.getFcs();
    }
}
