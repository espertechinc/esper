package com.espertech.esper.epl.approx;/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */

import com.espertech.esper.util.MurmurHash;
import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TestCountMinSketchStateHashes extends TestCase {

    public void testSimpleFlow() {
        CountMinSketchStateHashes state = CountMinSketchStateHashes.makeState(getDefaultSpec());

        add(state, "hello", 100);
        assertEquals(100, estimateCount(state, "hello"));

        add(state, "text", 1);
        assertEquals(1, estimateCount(state, "text"));

        add(state, "hello", 3);
        assertEquals(103, estimateCount(state, "hello"));
        assertEquals(1, estimateCount(state, "text"));
    }

    public void testSpace() {
        final double eps = 0.001;
        final double confidence = 0.999;

        final int space = 2000;
        final int points = 100000;

        final boolean randomized = true;

        Random random = new Random();
        CountMinSketchSpecHashes spec = new CountMinSketchSpecHashes(eps, confidence, 123456);
        CountMinSketchStateHashes state = CountMinSketchStateHashes.makeState(spec);

        Map<ByteBuffer, Long> sent = new HashMap<ByteBuffer, Long>();
        for (int i = 0; i < points; i++) {
            ByteBuffer bytes;
            if (randomized) {
                bytes = TestCountMinSketchStateTopK.generateBytesRandom(random, space);
            } else {
                bytes = TestCountMinSketchStateTopK.generateBytesModulo(i, space);
            }
            state.add(bytes.array(), 1);

            Long count = sent.get(bytes);
            if (count == null) {
                sent.put(bytes, 1L);
            } else {
                sent.put(bytes, count + 1);
            }

            if (i > 0 && i % 100000 == 0) {
                System.out.println("Completed " + i);
            }
        }

        // compare
        int errors = 0;
        for (Map.Entry<ByteBuffer, Long> entry : sent.entrySet()) {
            long frequency = state.estimateCount(entry.getKey().array());
            if (frequency != entry.getValue()) {
                System.out.println("Expected " + entry.getValue() + " received " + frequency);
                errors++;
            }
        }
        System.out.println("Found " + errors + " errors at space " + space + " sent " + points);
        assertTrue(eps * points > errors);
    }

    public void testPerformanceMurmurHash() {
        final int warmupLoopCount = 1; // 1000000;
        final int measureLoopCount = 1; // 1000000000;

        // init
        String[] texts = new String[]{"joe", "melissa", "townhall", "ballpark", "trial-by-error", "house", "teamwork", "recommendation", "partial", "soccer ball"};
        byte[][] bytes = new byte[texts.length][];
        for (int i = 0; i < texts.length; i++) {
            bytes[i] = texts[i].getBytes();
        }

        // warmup
        for (int i = 0; i < warmupLoopCount; i++) {
            byte[] bytearr = bytes[i % bytes.length];
            int code = MurmurHash.hash(bytearr, 0, bytearr.length, 0);
            if (code == 0) {
                System.out.println("A zero code");
            }
        }

        // run
        // 23.3 for 1G for MurmurHash.hash
        long start = System.nanoTime();
        for (int i = 0; i < measureLoopCount; i++) {
            byte[] bytearr = bytes[i % bytes.length];
            int codeOne = MurmurHash.hash(bytearr, 0, bytearr.length, 0);
            if (codeOne == 0) {
                System.out.println("A zero code");
            }
        }
        long delta = System.nanoTime() - start;
        // Comment me in - System.out.println("Delta " + (delta / 1000000000.0));
    }

    protected static CountMinSketchSpecHashes getDefaultSpec() {
        double epsOfTotalCount = 0.0001;
        double confidence = 0.99;
        int seed = 1234567;
        return new CountMinSketchSpecHashes(epsOfTotalCount, confidence, seed);
    }

    private long estimateCount(CountMinSketchStateHashes state, String item) {
        return state.estimateCount(getBytes(item));
    }

    private void add(CountMinSketchStateHashes state, String item, long count) {
        state.add(getBytes(item), count);
    }

    private static byte[] getBytes(String item) {
        try {
            return item.getBytes("UTF-16");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}

