package com.espertech.esper.example.cycledetect;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
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

        EPServiceProvider engine = main.getEngine();
        SupportUpdateListener listener = new SupportUpdateListener();
        engine.getEPAdministrator().getStatement("CycleDetector").addListener(listener);

        engine.getEPRuntime().sendEvent(new TransactionEvent("A", "B", 100));
        engine.getEPRuntime().sendEvent(new TransactionEvent("D", "A", 50));
        engine.getEPRuntime().sendEvent(new TransactionEvent("B", "C", 100));
        engine.getEPRuntime().sendEvent(new TransactionEvent("G", "H", 60));
        assertFalse(listener.isInvoked());

        engine.getEPRuntime().sendEvent(new TransactionEvent("C", "D", 30));
        Collection vertices = (Collection) listener.assertOneGetNewAndReset().get("out");
        EPAssertionUtil.assertEqualsAnyOrder(vertices.toArray(), new Object[]{"A", "B", "C", "D"});

        engine.destroy();
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

        // No cycles.  Just output properly ordered vertices.
        else {
            TopologicalOrderIterator<String, Object> orderIterator = new TopologicalOrderIterator<String, Object>(g);
            System.out.println("\nOrdering:");
            while (orderIterator.hasNext()) {
                String v = orderIterator.next();
                System.out.println(v);
            }
        }
    }
}
