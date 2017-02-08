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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.util.*;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.nio.ByteBuffer;
import java.util.Collection;

public class CountMinSketchAggState implements AggregationState {

    protected final CountMinSketchState state;
    private final CountMinSketchAgent agent;

    private final CountMinSketchAgentContextAdd add;
    private final CountMinSketchAgentContextEstimate estimate;
    private final CountMinSketchAgentContextFromBytes fromBytes;

    public CountMinSketchAggState(CountMinSketchState state, CountMinSketchAgent agent) {
        this.state = state;
        this.agent = agent;
        add = new CountMinSketchAgentContextAdd(state);
        estimate = new CountMinSketchAgentContextEstimate(state);
        fromBytes = new CountMinSketchAgentContextFromBytes(state);
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException("values are added through the add method");
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public void add(Object value) {
        add.setValue(value);
        agent.add(add);
    }

    public Long frequency(Object value) {
        estimate.setValue(value);
        return agent.estimate(estimate);
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public CountMinSketchTopK[] getFromBytes() {
        Collection<ByteBuffer> bytes = state.getTopKValues();
        if (bytes.isEmpty()) {
            return new CountMinSketchTopK[0];
        }
        CountMinSketchTopK[] arr = new CountMinSketchTopK[bytes.size()];
        int index = 0;
        for (ByteBuffer buf : bytes) {
            Long frequency = state.frequency(buf.array());
            fromBytes.setBytes(buf.array());
            Object value = agent.fromBytes(fromBytes);
            if (frequency == null) {
                continue;
            }
            arr[index++] = new CountMinSketchTopK(frequency, value);
        }
        return arr;
    }
}
