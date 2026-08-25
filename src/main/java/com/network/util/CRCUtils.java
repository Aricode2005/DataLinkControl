package com.network.util;

import com.network.model.Frame;
import com.network.model.Ack;
import java.util.zip.CRC32;

/**
 * Utility class to calculate and verify CRC (Frame Check Sequence).
 */
public class CRCUtils {

    /**
     * Calculates FCS for a Frame.
     */
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

    /**
     * Verifies if a Frame has a valid FCS.
     */
    public static boolean verifyFCS(Frame frame) {
        return calculateFCS(frame) == frame.getFcs();
    }

    /**
     * Calculates FCS for an Ack.
     */
    public static long calculateFCS(Ack ack) {
        CRC32 crc = new CRC32();
        crc.update(ack.getAckNo());
        crc.update(ack.isNak() ? 1 : 0);
        return crc.getValue();
    }

    /**
     * Verifies if an Ack has a valid FCS.
     */
    public static boolean verifyFCS(Ack ack) {
        return calculateFCS(ack) == ack.getFcs();
    }
}
