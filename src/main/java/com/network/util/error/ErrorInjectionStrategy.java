package com.network.util.error;

import java.util.Random;

public abstract class ErrorInjectionStrategy {
    protected Random random = new Random();

    public abstract void injectError(byte[] data, int length);

    protected void flipBit(byte[] data, int totalBits, int bitIndex) {
        if (bitIndex >= totalBits) return;
        int byteIndex = bitIndex / 8;
        int bitOffset = bitIndex % 8;
        data[byteIndex] ^= (1 << bitOffset);
    }
}
