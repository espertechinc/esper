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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class TestCycleDetect extends TestCase {
    public void testAlert() throws Exception {
        CycleDetectMain main = new CycleDetectMain();

        EPRuntime runtime = main.getRuntime();
        SupportUpdateListener listener = new SupportUpdateListener();
        runtime.getDeploymentService().getStatement("CycleDetect", "CycleDetector").addListener(listener);

        runtime.getEventService().sendEventBean(new TransactionEvent("A", "B", 100), "TransactionEvent");
        runtime.getEventService().sendEventBean(new TransactionEvent("D", "A", 50), "TransactionEvent");
        runtime.getEventService().sendEventBean(new TransactionEvent("B", "C", 100), "TransactionEvent");
        runtime.getEventService().sendEventBean(new TransactionEvent("G", "H", 60), "TransactionEvent");
        assertFalse(listener.isInvoked());

        runtime.getEventService().sendEventBean(new TransactionEvent("C", "D", 30), "TransactionEvent");
        Collection vertices = (Collection) listener.assertOneGetNewAndReset().get("out");
        EPAssertionUtil.assertEqualsAnyOrder(vertices.toArray(), new Object[]{"A", "B", "C", "D"});

        runtime.destroy();
    }

    public void testCycleDetection() {
        DefaultDirectedGraph<String, Object> g = new DefaultDirectedGraph<String, Object>(Object.class);

        // Add vertices, e.g. equations.
        g.addVertex("A");
        g.addVertex("B");
        g.addEdge("A", "B");

        g.addVertex("D");
        g.addVertex("A");
        g.addEdge("D", "A");

        g.addVertex("B");
        g.addVertex("C");
        g.addEdge("B", "C");

        g.addVertex("G");
        g.addVertex("H");
        g.addEdge("G", "H");

        g.addVertex("C");
        g.addVertex("D");
        g.addEdge("C", "D");

        System.out.println(g.toString());

        // Are there cycles in the dependencies.
        CycleDetector<String, Object> cycleDetector = new CycleDetector<String, Object>(g);
        // Cycle(s) detected.
        if (cycleDetector.detectCycles()) {

            System.out.println("Cycles detected.");

            // Get all vertices involved in cycles.
            Set<String> cycleVertices = cycleDetector.findCycles();

            // Loop through vertices trying to find disjoint cycles.
            while (!cycleVertices.isEmpty()) {
                System.out.println("Cycle:");

                // Get a vertex involved in a cycle.
                Iterator<String> iterator = cycleVertices.iterator();
                String cycle = iterator.next();

                // Get all vertices involved with this vertex.
                Set<String> subCycle = cycleDetector.findCyclesContainingVertex(cycle);
                for (String sub : subCycle) {
                    System.out.println("   " + sub);
                    // Remove vertex so that this cycle is not encountered
                    // again.
                    cycleVertices.remove(sub);
                }
            }
        }
        else {
            // No cycles.  Just output properly ordered vertices.
            TopologicalOrderIterator<String, Object> orderIterator = new TopologicalOrderIterator<String, Object>(g);
            System.out.println("\nOrdering:");
            while (orderIterator.hasNext()) {
                String v = orderIterator.next();
                System.out.println(v);
            }
        }
    }
}
