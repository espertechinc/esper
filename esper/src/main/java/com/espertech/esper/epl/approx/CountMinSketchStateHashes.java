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

import com.espertech.esper.util.MurmurHash;

import java.util.Random;

/**
 * <p>
 * Count-min sketch (or CM sketch) is a probabilistic sub-linear space streaming algorithm
 * (source: Wikipedia, see http://en.wikipedia.org/wiki/Count%E2%80%93min_sketch)
 * </p>
 * <p>
 * Count-min sketch computes an approximate frequency and thereby top-k or heavy-hitters.
 * </p>
 * <p>
 * Paper:
 * Graham Cormode and S. Muthukrishnan. An improved data stream summary:
 * The Count-Min sketch and its applications. 2004. 10.1016/j.jalgor.2003.12.001
 * http://dl.acm.org/citation.cfm?id=1073718
 * </p>
 */
public class CountMinSketchStateHashes {

    private int depth;
    private int width;
    private long[][] table;
    private long[] hash;
    private long total;

    public static CountMinSketchStateHashes makeState(CountMinSketchSpecHashes spec) {
        int width = (int) Math.ceil(2 / spec.getEpsOfTotalCount());
        int depth = (int) Math.ceil(-Math.log(1 - spec.getConfidence()) / Math.log(2));
        long[][] table = new long[depth][width];
        long[] hash = new long[depth];
        Random r = new Random(spec.getSeed());
        for (int i = 0; i < depth; ++i) {
            hash[i] = r.nextInt(Integer.MAX_VALUE);
        }
        return new CountMinSketchStateHashes(depth, width, table, hash, 0);
    }

    public CountMinSketchStateHashes(int depth, int width, long[][] table, long[] hash, long total) {
        this.depth = depth;
        this.width = width;
        this.table = table;
        this.hash = hash;
        this.total = total;
    }

    public long[][] getTable() {
        return table;
    }

    public long[] getHash() {
        return hash;
    }

    public int getDepth() {
        return depth;
    }

    public int getWidth() {
        return width;
    }

    public void incTotal(long count) {
        total += count;
    }

    public long getTotal() {
        return total;
    }

    public long estimateCount(byte[] item) {
        long res = Long.MAX_VALUE;
        int[] buckets = getHashBuckets(item, depth, width);
        for (int i = 0; i < depth; ++i) {
            res = Math.min(res, table[i][buckets[i]]);
        }
        return res;
    }

    public void add(byte[] item, long count) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative increments not implemented");
        }
        int[] buckets = getHashBuckets(item, depth, width);
        for (int i = 0; i < depth; ++i) {
            table[i][buckets[i]] += count;
        }
        total += count;
    }

    private int[] getHashBuckets(byte[] b, int hashCount, int max) {
        int[] result = new int[hashCount];
        int hash1 = MurmurHash.hash(b, 0, b.length, 0);
        int hash2 = MurmurHash.hash(b, 0, b.length, hash1);
        for (int i = 0; i < hashCount; i++) {
            result[i] = Math.abs((hash1 + i * hash2) % max);
        }
        return result;
    }
}
