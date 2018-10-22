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
package com.espertech.esper.common.internal.epl.agg.access.countminsketch;

import com.espertech.esper.common.internal.epl.approx.countminsketch.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class AggregationStateSerdeCountMinSketch {

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param output out
     * @param state  state
     * @throws IOException when there is a write exception
     */
    public static void writeCountMinSketch(DataOutput output, CountMinSketchAggState state) throws IOException {
        writeState(output, state.getState());
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param input in
     * @param spec  spec
     * @return state
     * @throws IOException when there is a read exception
     */
    public static CountMinSketchAggState readCountMinSketch(DataInput input, CountMinSketchSpec spec) throws IOException {
        CountMinSketchAggState state = spec.makeAggState();
        readState(input, state.getState());
        return state;
    }

    private static void writeState(DataOutput output, CountMinSketchState state) throws IOException {
        CountMinSketchStateHashes hashes = state.getHashes();
        output.writeInt(hashes.getDepth());
        output.writeInt(hashes.getWidth());

        long[][] table = hashes.getTable();
        output.writeInt(table.length);
        for (long[] row : table) {
            output.writeInt(row.length);
            for (long col : row) {
                output.writeLong(col);
            }
        }

        long[] hash = hashes.getHash();
        output.writeInt(hash.length);
        for (long aHash : hash) {
            output.writeLong(aHash);
        }

        output.writeLong(hashes.getTotal());

        CountMinSketchStateTopk topk = state.getTopk();
        output.writeBoolean(topk != null);
        if (topk != null) {
            output.writeInt(topk.getTopkMax());
            TreeMap<Long, Object> topMap = topk.getTopk();
            output.writeInt(topMap.size());
            for (Map.Entry<Long, Object> entry : topMap.entrySet()) {
                output.writeLong(entry.getKey());
                if (entry.getValue() instanceof ByteBuffer) {
                    output.writeInt(1);
                    writeBytes(output, (ByteBuffer) entry.getValue());
                } else {
                    Deque<ByteBuffer> q = (Deque<ByteBuffer>) entry.getValue();
                    output.writeInt(q.size());
                    for (ByteBuffer buf : q) {
                        writeBytes(output, buf);
                    }
                }
            }
        }
    }

    private static void readState(DataInput input, CountMinSketchState state) throws IOException {
        int depth = input.readInt();
        int width = input.readInt();

        int rowsTable = input.readInt();
        long[][] table = new long[rowsTable][];
        for (int i = 0; i < rowsTable; i++) {
            int colsRows = input.readInt();
            table[i] = new long[colsRows];
            for (int j = 0; j < colsRows; j++) {
                table[i][j] = input.readLong();
            }
        }

        int rowsHash = input.readInt();
        long[] hash = new long[rowsHash];
        for (int i = 0; i < rowsTable; i++) {
            hash[i] = input.readLong();
        }

        long total = input.readLong();
        state.setHashes(new CountMinSketchStateHashes(depth, width, table, hash, total));

        boolean hasTopk = input.readBoolean();
        state.setTopk(null);
        if (hasTopk) {
            int topkMax = input.readInt();

            TreeMap<Long, Object> topMap = new TreeMap<Long, Object>(Collections.reverseOrder());
            Map<ByteBuffer, Long> refMap = new HashMap<ByteBuffer, Long>();
            int numRows = input.readInt();
            for (int i = 0; i < numRows; i++) {
                long freq = input.readLong();
                int numEntries = input.readInt();
                if (numEntries == 1) {
                    ByteBuffer buf = readBytes(input);
                    topMap.put(freq, buf);
                    refMap.put(buf, freq);
                } else {
                    Deque<ByteBuffer> q = new ArrayDeque<ByteBuffer>(numEntries);
                    for (int j = 0; j < numEntries; j++) {
                        ByteBuffer buf = readBytes(input);
                        q.add(buf);
                        refMap.put(buf, freq);
                    }
                    topMap.put(freq, q);
                }
            }
            state.setTopk(new CountMinSketchStateTopk(topkMax, topMap, refMap));
        }
    }

    private static void writeBytes(DataOutput output, ByteBuffer value) throws IOException {
        byte[] bytes = value.array();
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static ByteBuffer readBytes(DataInput input) throws IOException {
        int byteSize = input.readInt();
        byte[] bytes = new byte[byteSize];
        input.readFully(bytes);
        return ByteBuffer.wrap(bytes);
    }
}
