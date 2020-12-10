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
package com.espertech.esper.common.internal.epl.agg.method.rate;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.espertech.esper.common.internal.epl.agg.core.AggregationSerdeUtil.readVersionChecked;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationSerdeUtil.writeVersion;

public class AggregatorRateEverSerde {

    protected static final short SERDE_VERSION = 1;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param output out
     * @param points points
     * @throws IOException io error
     */
    public static void writePoints(DataOutput output, Deque<Long> points) throws IOException {
        writeVersion(SERDE_VERSION, output);
        output.writeInt(points.size());
        for (long value : points) {
            output.writeLong(value);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param input input
     * @return points
     * @throws IOException io error
     */
    public static Deque<Long> readPoints(DataInput input) throws IOException {
        readVersionChecked(SERDE_VERSION, input);
        ArrayDeque<Long> points = new ArrayDeque<>();
        int size = input.readInt();
        for (int i = 0; i < size; i++) {
            points.add(input.readLong());
        }
        return points;
    }
}
