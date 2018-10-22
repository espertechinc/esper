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
package com.espertech.esper.example.cycledetect;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionState;
import com.espertech.esper.common.internal.collection.RefCountedSetAtomicInteger;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.Collection;
import java.util.Set;

public class CycleDetectorAggregationState implements AggregationMultiFunctionState {

    private final CycleDetectorAggregationStateFactory factory;
    private final RefCountedSetAtomicInteger<Object> vertexReferenceCount = new RefCountedSetAtomicInteger<>();
    private DefaultDirectedGraph<Object, EventBean> graph;
    private CycleDetector<Object, EventBean> cycleDetector;

    public CycleDetectorAggregationState(CycleDetectorAggregationStateFactory factory) {
        this.factory = factory;
        graph = new DefaultDirectedGraph<>(EventBean.class);
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        Object from = factory.getFrom().evaluate(eventsPerStream, true, exprEvaluatorContext);
        Object to = factory.getTo().evaluate(eventsPerStream, true, exprEvaluatorContext);

        if (vertexReferenceCount.add(from)) {
            graph.addVertex(from);
        }
        if (vertexReferenceCount.add(to)) {
            graph.addVertex(to);
        }

        EventBean event = eventsPerStream[0];
        graph.addEdge(from, to, event);
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        graph.removeEdge(eventsPerStream[0]);

        Object from = factory.getFrom().evaluate(eventsPerStream, true, exprEvaluatorContext);
        Object to = factory.getTo().evaluate(eventsPerStream, true, exprEvaluatorContext);

        if (vertexReferenceCount.remove(from)) {
            graph.removeVertex(from);
        }
        if (vertexReferenceCount.remove(to)) {
            graph.removeVertex(to);
        }
    }

    public void clear() {
        graph = new DefaultDirectedGraph<>(EventBean.class);
    }

    public int size() {
        return 0;
    }

    public boolean hasCycle() {
        long start = System.currentTimeMillis();
        cycleDetector = new CycleDetector<>(graph);
        boolean detected = cycleDetector.detectCycles();
        long delta = System.currentTimeMillis() - start;
        // System.out.println("Cycle " + (detected ? "" : " not") + " detected in " + delta + " msec");
        return detected;
    }

    public Collection<Object> getCycle() {
        long start = System.currentTimeMillis();
        if (cycleDetector == null) {
            cycleDetector = new CycleDetector<>(graph);
        }
        Set<Object> cycles = cycleDetector.findCycles();
        long delta = System.currentTimeMillis() - start;
        // System.out.println("Cycle output took " + delta + " msec");

        for (Object vertex : cycles) {
            vertexReferenceCount.removeAll(vertex);
            graph.removeVertex(vertex);
        }
        return cycles;
    }
}
