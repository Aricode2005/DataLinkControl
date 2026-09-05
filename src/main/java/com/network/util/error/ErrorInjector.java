package com.network.util.error;

import java.util.Random;

public class ErrorInjector {
    private ErrorInjectionStrategy strategy;

    public ErrorInjector(ErrorInjectionStrategy initialStrategy) {
        this.strategy = initialStrategy;
    }

    public void setStrategy(ErrorInjectionStrategy newStrategy) {
        this.strategy = newStrategy;
    }

    public ErrorInjectionStrategy getStrategy() {
        return strategy;
    }

    public void execute(byte[] data, int length) {
        if (strategy != null && length > 0) {
            strategy.injectError(data, length);
        }
    }

    

    public static class NoError extends ErrorInjectionStrategy {
        @Override
        public void injectError(byte[] data, int length) {
           
        }
    }

    public static class SingleBitError extends ErrorInjectionStrategy {
        @Override
        public void injectError(byte[] data, int length) {
            int totalBits = length * 8;
            if (totalBits == 0) return;
            int randomBit = random.nextInt(totalBits);
            flipBit(data, totalBits, randomBit);
        }
    }

    public static class TwoIsolatedErrors extends ErrorInjectionStrategy {
        @Override
        public void injectError(byte[] data, int length) {
            int totalBits = length * 8;
            if (totalBits < 2) return;
            int bit1 = random.nextInt(totalBits);
            int bit2;
            do {
                bit2 = random.nextInt(totalBits);
            } while (bit1 == bit2);
            flipBit(data, totalBits, bit1);
            flipBit(data, totalBits, bit2);
        }
    }

    public static class OddNumberOfErrors extends ErrorInjectionStrategy {
        @Override
        public void injectError(byte[] data, int length) {
            int totalBits = length * 8;
            if (totalBits == 0) return;
            int numErrors = ((random.nextInt(3)) * 2) + 3;
            for (int i = 0; i < numErrors; ++i) {
                int randomBit = random.nextInt(totalBits);
                flipBit(data, totalBits, randomBit);
            }
        }
    }

    public static class BurstError extends ErrorInjectionStrategy {
        private int burstLength;

        public BurstError(int burstLength) {
            this.burstLength = burstLength;
        }

        @Override
        public void injectError(byte[] data, int length) {
            int totalBits = length * 8;
            if (burstLength < 2) return;
            int currentBurst = burstLength;
            if (currentBurst > totalBits) currentBurst = totalBits;
            
            int startBit = random.nextInt(totalBits - currentBurst + 1);
            flipBit(data, totalBits, startBit);
            flipBit(data, totalBits, startBit + currentBurst - 1);
            for (int i = 1; i < currentBurst - 1; ++i) {
                if (random.nextInt(2) == 0) {
                    flipBit(data, totalBits, startBit + i);
                }
            }
        }
    }

    public static class CRC32Miss extends ErrorInjectionStrategy {
        @Override
        public void injectError(byte[] data, int length) {
            if (length < 5) return;
            data[0] ^= 0x01;
            data[1] ^= 0x04;
            data[2] ^= (byte)0xC1;
            data[3] ^= 0x1D;
            data[4] ^= (byte)0xB7;
        }
    }
}
