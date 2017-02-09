/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.epl.approx;

import com.espertech.esper.client.util.CountMinSketchAgentStringUTF16;
import com.espertech.esper.collection.Pair;
import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.*;

public class TestCountMinSketchStateTopK extends TestCase {

    public void testTopK() {
        final int space = 10000;
        final int points = 100000;
        final int topkMax = 100;

        Random random = new Random();
        CountMinSketchStateTopk topk = new CountMinSketchStateTopk(topkMax);
        Map<ByteBuffer, Long> sent = new HashMap<ByteBuffer, Long>();
        for (int i = 0; i < points; i++) {
            // for simple population: ByteBuffer bytes = generateBytesModulo(i, space);
            ByteBuffer bytes = generateBytesRandom(random, space);
            Long count = sent.get(bytes);
            if (count == null) {
                sent.put(bytes, 1L);
                topk.updateExpectIncreasing(bytes.array(), 1);
            } else {
                sent.put(bytes, count + 1);
                topk.updateExpectIncreasing(bytes.array(), count + 1);
            }

            if (i > 0 && i % 100000 == 0) {
                System.out.println("Completed " + i);
            }
        }

        // compare
        List<ByteBuffer> top = topk.getTopKValues();

        // assert filled
        if (sent.size() < topkMax) {
            assertEquals(sent.size(), top.size());
        } else {
            assertEquals(topkMax, top.size());
        }

        // assert no duplicate values
        Set<ByteBuffer> set = new HashSet<ByteBuffer>();
        for (ByteBuffer topBytes : top) {
            assertTrue(set.add(topBytes));
        }

        // assert order descending
        Long lastFreq = null;
        for (ByteBuffer topBytes : top) {
            long freq = sent.get(topBytes);
            if (lastFreq != null) {
                assertTrue(freq <= lastFreq);
            }
            lastFreq = freq;
        }
    }

    public void testFlow() {
        // top-k for 3
        CountMinSketchSpec spec = new CountMinSketchSpec(TestCountMinSketchStateHashes.getDefaultSpec(), 3, new CountMinSketchAgentStringUTF16());
        CountMinSketchState state = CountMinSketchState.makeState(spec);

        updateAssert(state, "a", "a=1");
        updateAssert(state, "b", "a=1,b=1");
        updateAssert(state, "a", "a=2,b=1");
        updateAssert(state, "c", "a=2,b=1,c=1");
        updateAssert(state, "d", "a=2,b=1,c=1");
        updateAssert(state, "c", "a=2,b=1,c=2");
        updateAssert(state, "a", "a=3,b=1,c=2");
        updateAssert(state, "d", "a=3,d=2,c=2");
        updateAssert(state, "e", "a=3,d=2,c=2");
        updateAssert(state, "e", "a=3,d=2,c=2");
        updateAssert(state, "e", "a=3,e=3,c=2");
        updateAssert(state, "d", "a=3,e=3,d=3");
        updateAssert(state, "c", "a=3,e=3,d=3");
        updateAssert(state, "c", "a=3,e=3,c=4");
    }

    private void updateAssert(CountMinSketchState state, String value, String expected) {
        state.add(value.getBytes(), 1);
        Collection<ByteBuffer> topkValues = state.getTopKValues();
        List<Pair<Long, Object>> topkList = new ArrayList<Pair<Long, Object>>();
        for (ByteBuffer topkValue : topkValues) {
            long frequency = state.frequency(topkValue.array());
            String text = new String(topkValue.array());
            topkList.add(new Pair<Long, Object>(frequency, text));
        }
        assertList(expected, topkList);
    }

    private void assertList(String pairText, List<Pair<Long, Object>> asList) {
        String[] pairs = pairText.split(",");
        assertEquals("received " + asList.toString(), pairs.length, asList.size());
        for (String pair : pairs) {
            String[] pairArr = pair.split("=");
            Pair<Long, Object> pairExpected = new Pair<Long, Object>(Long.parseLong(pairArr[1]), pairArr[0]);
            boolean found = asList.remove(pairExpected);
            assertTrue("failed to find " + pairExpected + " among remaining " + asList.toString(), found);
        }
    }

    protected static ByteBuffer generateBytesRandom(Random random, int space) {
        int val = random.nextInt(space);
        byte[] bytes = Integer.toString(val).getBytes();
        return ByteBuffer.wrap(bytes);
    }

    protected static ByteBuffer generateBytesModulo(int num, int space) {
        String value = Integer.toString(num % space);
        return ByteBuffer.wrap(value.getBytes());
    }
}
