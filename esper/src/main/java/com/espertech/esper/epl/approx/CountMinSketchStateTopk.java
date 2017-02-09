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

import java.nio.ByteBuffer;
import java.util.*;

public class CountMinSketchStateTopk {

    private final int topkMax;

    // Wherein: Object either is ByteBuffer or Deque<ByteBuffer>
    private final TreeMap<Long, Object> topk;
    private final Map<ByteBuffer, Long> lastFreqForItem;

    public CountMinSketchStateTopk(int topkMax) {
        this.topkMax = topkMax;
        this.lastFreqForItem = new HashMap<ByteBuffer, Long>();
        this.topk = new TreeMap<Long, Object>(Collections.reverseOrder());
    }

    public CountMinSketchStateTopk(int topkMax, TreeMap<Long, Object> topk, Map<ByteBuffer, Long> lastFreqForItem) {
        this.topkMax = topkMax;
        this.topk = topk;
        this.lastFreqForItem = lastFreqForItem;
    }

    public TreeMap<Long, Object> getTopk() {
        return topk;
    }

    public void updateExpectIncreasing(byte[] value, long frequency) {

        boolean filled = lastFreqForItem.size() == topkMax;

        if (!filled) {
            ByteBuffer valueBuffer = ByteBuffer.wrap(value);
            updateInternal(valueBuffer, frequency);
        } else {
            Long lastKey = topk.lastKey();
            if (frequency > lastKey) {
                ByteBuffer valueBuffer = ByteBuffer.wrap(value);
                updateInternal(valueBuffer, frequency);
            }
        }

        trimItems();
    }

    private void updateInternal(ByteBuffer valueBuffer, long frequency) {
        Long beforeUpdateFrequency = lastFreqForItem.put(valueBuffer, frequency);
        if (beforeUpdateFrequency != null) {
            removeItemFromSorted(beforeUpdateFrequency, valueBuffer);
        }
        addItemToSorted(frequency, valueBuffer);
    }

    private void removeItemFromSorted(long frequency, ByteBuffer value) {
        Object existing = topk.get(frequency);
        if (existing != null) {
            if (existing instanceof Deque) {
                Deque<ByteBuffer> deque = (Deque<ByteBuffer>) existing;
                deque.remove(value);
                if (deque.isEmpty()) {
                    topk.remove(frequency);
                }
            } else {
                topk.remove(frequency);
            }
        }
    }

    private void addItemToSorted(long frequency, ByteBuffer value) {
        Object existing = topk.get(frequency);
        if (existing == null) {
            topk.put(frequency, value);
        } else if (existing instanceof Deque) {
            Deque<ByteBuffer> deque = (Deque<ByteBuffer>) existing;
            deque.add(value);
        } else {
            Deque<ByteBuffer> deque = new ArrayDeque<ByteBuffer>(2);
            deque.add((ByteBuffer) existing);
            deque.add(value);
            topk.put(frequency, deque);
        }
    }

    private void trimItems() {
        while (lastFreqForItem.size() > topkMax) {
            Map.Entry<Long, Object> last = topk.lastEntry();
            if (last == null) {
                break;
            }
            if (last.getValue() instanceof Deque) {
                Deque<ByteBuffer> deque = (Deque<ByteBuffer>) last.getValue();
                ByteBuffer valueRemoved = deque.removeLast();
                lastFreqForItem.remove(valueRemoved);
                if (deque.isEmpty()) {
                    topk.remove(last.getKey());
                }
            } else {
                topk.remove(last.getKey());
                lastFreqForItem.remove((ByteBuffer) last.getValue());
            }
        }
    }

    public List<ByteBuffer> getTopKValues() {
        List<ByteBuffer> values = new ArrayList<ByteBuffer>();
        for (Map.Entry<Long, Object> entry : topk.entrySet()) {
            if (entry.getValue() instanceof Deque) {
                Deque<ByteBuffer> set = (Deque<ByteBuffer>) entry.getValue();
                for (ByteBuffer o : set) {
                    values.add(o);
                }
            } else {
                values.add((ByteBuffer) entry.getValue());
            }
        }
        return values;
    }

    public int getTopkMax() {
        return topkMax;
    }
}
