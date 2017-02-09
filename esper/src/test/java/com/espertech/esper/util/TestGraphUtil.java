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
package com.espertech.esper.util;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;

import java.util.*;

public class TestGraphUtil extends TestCase {
    public void testMerge() {
        Map<String, Object> mapOne = makeMap(new Object[][]{
                {"base1", 1},
                {"base3", makeMap(new Object[][]{{"n1", 9}})},
                {"base4", null},
        });

        Map<String, Object> mapTwo = makeMap(new Object[][]{
                {"base1", null},
                {"base2", 5},
                {"base5", null},
                {"base3", makeMap(new Object[][]{{"n1", 7}, {"n2", 10}})}
        });

        Map<String, Object> merged = GraphUtil.mergeNestableMap(mapOne, mapTwo);
        assertEquals(1, merged.get("base1"));
        assertEquals(5, merged.get("base2"));
        assertEquals(null, merged.get("base4"));
        assertEquals(null, merged.get("base5"));
        assertEquals(5, merged.size());
        Map<String, Object> nested = (Map<String, Object>) merged.get("base3");
        assertEquals(2, nested.size());
        assertEquals(9, nested.get("n1"));
        assertEquals(10, nested.get("n2"));
    }

    public void testSimpleTopDownOrder() throws Exception {
        Map<String, Set<String>> graph = new LinkedHashMap<String, Set<String>>();
        assertEquals(0, GraphUtil.getTopDownOrder(graph).size());

        add(graph, "1_1", "1");
        EPAssertionUtil.assertEqualsExactOrder(GraphUtil.getTopDownOrder(graph).toArray(), "1,1_1".split(","));

        add(graph, "1_1_1", "1_1");
        EPAssertionUtil.assertEqualsExactOrder(GraphUtil.getTopDownOrder(graph).toArray(), "1,1_1,1_1_1".split(","));

        add(graph, "0_1", "0");
        EPAssertionUtil.assertEqualsExactOrder(GraphUtil.getTopDownOrder(graph).toArray(), "0,0_1,1,1_1,1_1_1".split(","));

        add(graph, "1_2", "1");
        EPAssertionUtil.assertEqualsExactOrder(GraphUtil.getTopDownOrder(graph).toArray(), "0,0_1,1,1_1,1_1_1,1_2".split(","));

        add(graph, "1_1_2", "1_1");
        EPAssertionUtil.assertEqualsExactOrder(GraphUtil.getTopDownOrder(graph).toArray(), "0,0_1,1,1_1,1_1_1,1_1_2,1_2".split(","));

        add(graph, "1_2_1", "1_2");
        EPAssertionUtil.assertEqualsExactOrder(GraphUtil.getTopDownOrder(graph).toArray(), "0,0_1,1,1_1,1_1_1,1_1_2,1_2,1_2_1".split(","));

        add(graph, "0", "R");
        EPAssertionUtil.assertEqualsExactOrder(GraphUtil.getTopDownOrder(graph).toArray(), "1,1_1,1_1_1,1_1_2,1_2,1_2_1,R,0,0_1,".split(","));

        add(graph, "1", "R");
        EPAssertionUtil.assertEqualsExactOrder(GraphUtil.getTopDownOrder(graph).toArray(), "R,0,0_1,1,1_1,1_1_1,1_1_2,1_2,1_2_1".split(","));
    }

    public void testAcyclicTopDownOrder() throws Exception {
        Map<String, Set<String>> graph = new LinkedHashMap<String, Set<String>>();

        add(graph, "1_1", "R2");
        add(graph, "A", "R1");
        add(graph, "A", "R2");
        EPAssertionUtil.assertEqualsExactOrder("R1,R2,1_1,A".split(","), GraphUtil.getTopDownOrder(graph).toArray());

        add(graph, "R1", "R2");
        EPAssertionUtil.assertEqualsExactOrder("R2,1_1,R1,A".split(","), GraphUtil.getTopDownOrder(graph).toArray());

        add(graph, "1_1", "A");
        EPAssertionUtil.assertEqualsExactOrder("R2,R1,A,1_1".split(","), GraphUtil.getTopDownOrder(graph).toArray());

        add(graph, "0", "1_1");
        EPAssertionUtil.assertEqualsExactOrder("R2,R1,A,1_1,0".split(","), GraphUtil.getTopDownOrder(graph).toArray());

        add(graph, "R1", "0");
        tryInvalid(graph, "Circular dependency detected between [1_1, A, R1, 0]");
    }

    public void testInvalidTopDownOder() throws Exception {
        Map<String, Set<String>> graph = new LinkedHashMap<String, Set<String>>();
        add(graph, "1_1", "1");
        add(graph, "1", "1_1");
        tryInvalid(graph, "Circular dependency detected between [1_1, 1]");

        graph = new LinkedHashMap<String, Set<String>>();
        add(graph, "1", "2");
        add(graph, "2", "3");
        add(graph, "3", "1");
        tryInvalid(graph, "Circular dependency detected between [1, 2, 3]");
    }

    private void tryInvalid(Map<String, Set<String>> graph, String msg) {
        try {
            GraphUtil.getTopDownOrder(graph);
            fail();
        } catch (GraphCircularDependencyException ex) {
            // expected
            assertEquals(msg, ex.getMessage());
        }

    }

    private void add(Map<String, Set<String>> graph, String child, String parent) {
        Set<String> parents = graph.get(child);
        if (parents == null) {
            parents = new HashSet<String>();
            graph.put(child, parents);
        }
        parents.add(parent);
    }

    private Map<String, Object> makeMap(Object[][] entries) {
        Map result = new HashMap<String, Object>();
        if (entries == null) {
            return result;
        }
        for (int i = 0; i < entries.length; i++) {
            result.put(entries[i][0], entries[i][1]);
        }
        return result;
    }
}
