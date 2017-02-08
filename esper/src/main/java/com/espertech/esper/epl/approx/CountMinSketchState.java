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
import java.util.Collection;
import java.util.Collections;

public class CountMinSketchState {

    private CountMinSketchStateHashes hashes;
    private CountMinSketchStateTopk topk;

    public static CountMinSketchState makeState(CountMinSketchSpec spec) {
        CountMinSketchStateHashes hashes = CountMinSketchStateHashes.makeState(spec.getHashesSpec());
        CountMinSketchStateTopk topk = null;
        if (spec.getTopkSpec() != null && spec.getTopkSpec() > 0) {
            topk = new CountMinSketchStateTopk(spec.getTopkSpec());
        }
        return new CountMinSketchState(hashes, topk);
    }

    public CountMinSketchState(CountMinSketchStateHashes hashes, CountMinSketchStateTopk topk) {
        this.hashes = hashes;
        this.topk = topk;
    }

    public void add(byte[] bytes, int count) {
        hashes.add(bytes, count);
        if (topk != null) {
            long frequency = hashes.estimateCount(bytes);
            topk.updateExpectIncreasing(bytes, frequency);
        }
    }

    public long frequency(byte[] bytes) {
        return hashes.estimateCount(bytes);
    }

    public Collection<ByteBuffer> getTopKValues() {
        if (topk == null) {
            return Collections.emptyList();
        }
        return topk.getTopKValues();
    }

    public CountMinSketchStateHashes getHashes() {
        return hashes;
    }

    public CountMinSketchStateTopk getTopk() {
        return topk;
    }

    public void setHashes(CountMinSketchStateHashes hashes) {
        this.hashes = hashes;
    }

    public void setTopk(CountMinSketchStateTopk topk) {
        this.topk = topk;
    }
}

