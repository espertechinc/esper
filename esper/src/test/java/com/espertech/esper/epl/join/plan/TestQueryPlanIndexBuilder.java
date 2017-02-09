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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestQueryPlanIndexBuilder extends TestCase {
    private QueryGraph queryGraph;
    private EventType[] types;

    public void setUp() {
        types = new EventType[]{
                SupportEventTypeFactory.createMapType(createType("p00,p01")),
                SupportEventTypeFactory.createMapType(createType("p10")),
                SupportEventTypeFactory.createMapType(createType("p20,p21")),
                SupportEventTypeFactory.createMapType(createType("p30,p31")),
                SupportEventTypeFactory.createMapType(createType("p40,p41,p42")),
        };

        queryGraph = new QueryGraph(5, null, false);
        queryGraph.addStrictEquals(0, "p00", make(0, "p00"), 1, "p10", make(1, "p10"));
        queryGraph.addStrictEquals(0, "p01", make(0, "p01"), 2, "p20", make(2, "p20"));
        queryGraph.addStrictEquals(4, "p40", make(4, "p40"), 3, "p30", make(3, "p30"));
        queryGraph.addStrictEquals(4, "p41", make(4, "p41"), 3, "p31", make(3, "p31"));
        queryGraph.addStrictEquals(4, "p42", make(4, "p42"), 2, "p21", make(2, "p21"));
    }

    public void testBuildIndexSpec() {
        QueryPlanIndex[] indexes = QueryPlanIndexBuilder.buildIndexSpec(queryGraph, types, new String[queryGraph.getNumStreams()][][]);

        String[][] expected = new String[][]{{"p00"}, {"p01"}};
        EPAssertionUtil.assertEqualsExactOrder(expected, indexes[0].getIndexProps());

        expected = new String[][]{{"p10"}};
        EPAssertionUtil.assertEqualsExactOrder(expected, indexes[1].getIndexProps());

        expected = new String[][]{{"p20"}, {"p21"}};
        EPAssertionUtil.assertEqualsExactOrder(expected, indexes[2].getIndexProps());

        expected = new String[][]{{"p30", "p31"}};
        EPAssertionUtil.assertEqualsExactOrder(expected, indexes[3].getIndexProps());

        expected = new String[][]{{"p42"}, {"p40", "p41"}};
        EPAssertionUtil.assertEqualsExactOrder(expected, indexes[4].getIndexProps());

        // Test no index, should have a single entry with a zero-length property name array
        queryGraph = new QueryGraph(3, null, false);
        indexes = QueryPlanIndexBuilder.buildIndexSpec(queryGraph, types, new String[queryGraph.getNumStreams()][][]);
        assertEquals(1, indexes[1].getIndexProps().length);
    }

    public void testIndexAlreadyExists() {
        queryGraph = new QueryGraph(5, null, false);
        queryGraph.addStrictEquals(0, "p00", make(0, "p00"), 1, "p10", make(1, "p10"));
        queryGraph.addStrictEquals(0, "p00", make(0, "p00"), 2, "p20", make(2, "p20"));

        QueryPlanIndex[] indexes = QueryPlanIndexBuilder.buildIndexSpec(queryGraph, types, new String[queryGraph.getNumStreams()][][]);

        String[][] expected = new String[][]{{"p00"}};
        EPAssertionUtil.assertEqualsExactOrder(expected, indexes[0].getIndexProps());
    }

    private Map<String, Object> createType(String propCSV) {
        String[] props = propCSV.split(",");
        Map<String, Object> type = new HashMap<String, Object>();
        for (int i = 0; i < props.length; i++) {
            type.put(props[i], String.class);
        }
        return type;
    }

    private ExprIdentNode make(int stream, String p) {
        return new ExprIdentNodeImpl(types[stream], p, stream);
    }
}
