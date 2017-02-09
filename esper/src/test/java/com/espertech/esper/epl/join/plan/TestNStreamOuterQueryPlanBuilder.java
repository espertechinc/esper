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

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.collection.InterchangeablePair;
import com.espertech.esper.epl.spec.OuterJoinDesc;
import com.espertech.esper.supportunit.epl.SupportOuterJoinDescFactory;
import com.espertech.esper.type.OuterJoinType;
import com.espertech.esper.util.DependencyGraph;
import junit.framework.TestCase;

import java.util.*;

public class TestNStreamOuterQueryPlanBuilder extends TestCase {
    public void testGraphOuterJoins() throws Exception {
        OuterJoinDesc[] descList = new OuterJoinDesc[2];
        descList[0] = SupportOuterJoinDescFactory.makeDesc("intPrimitive", "s0", "intBoxed", "s1", OuterJoinType.RIGHT);
        descList[1] = SupportOuterJoinDescFactory.makeDesc("simpleProperty", "s2", "theString", "s1", OuterJoinType.FULL);

        OuterInnerDirectionalGraph graph = NStreamOuterQueryPlanBuilder.graphOuterJoins(3, descList);

        // assert the inner and outer streams for each stream
        assertInners(new int[][]{null, {0, 2}, {1}}, graph);
        assertOuters(new int[][]{{1}, {2}, {1}}, graph);

        descList[0] = SupportOuterJoinDescFactory.makeDesc("intPrimitive", "s1", "intBoxed", "s0", OuterJoinType.LEFT);
        descList[1] = SupportOuterJoinDescFactory.makeDesc("simpleProperty", "s2", "theString", "s1", OuterJoinType.RIGHT);

        graph = NStreamOuterQueryPlanBuilder.graphOuterJoins(3, descList);

        // assert the inner and outer streams for each stream
        assertInners(new int[][]{{1}, null, {1}}, graph);
        assertOuters(new int[][]{null, {0, 2}, null}, graph);

        try {
            NStreamOuterQueryPlanBuilder.graphOuterJoins(3, new OuterJoinDesc[0]);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testRecursiveBuild() throws Exception {
        int streamNum = 2;
        QueryGraph queryGraph = new QueryGraph(6, null, false);
        OuterInnerDirectionalGraph outerInnerGraph = new OuterInnerDirectionalGraph(6);
        Set<Integer> completedStreams = new HashSet<Integer>();
        LinkedHashMap<Integer, int[]> substreamsPerStream = new LinkedHashMap<Integer, int[]>();
        boolean[] requiredPerStream = new boolean[6];

        /**
         * 2    <--   3
         *                  <-- 4
         *                  --> 5
         *      -->   1
         *                  --> 0
         *
         */
        outerInnerGraph.add(3, 2).add(2, 1).add(4, 3).add(1, 0).add(3, 5);
        queryGraph.addStrictEquals(2, "", null, 3, "", null);
        queryGraph.addStrictEquals(3, "", null, 4, "", null);
        queryGraph.addStrictEquals(3, "", null, 5, "", null);
        queryGraph.addStrictEquals(2, "", null, 1, "", null);
        queryGraph.addStrictEquals(1, "", null, 0, "", null);

        Set<InterchangeablePair<Integer, Integer>> innerJoins = new HashSet<InterchangeablePair<Integer, Integer>>();
        InnerJoinGraph innerJoinGraph = new InnerJoinGraph(6, innerJoins);
        Stack<Integer> streamStack = new Stack<Integer>();

        NStreamOuterQueryPlanBuilder.recursiveBuild(streamNum, streamStack, queryGraph, outerInnerGraph, innerJoinGraph, completedStreams,
                substreamsPerStream, requiredPerStream, new DependencyGraph(6, false));

        assertEquals(6, substreamsPerStream.size());
        EPAssertionUtil.assertEqualsExactOrder(substreamsPerStream.get(2), new int[]{3, 1});
        EPAssertionUtil.assertEqualsExactOrder(substreamsPerStream.get(3), new int[]{4, 5});
        EPAssertionUtil.assertEqualsExactOrder(substreamsPerStream.get(1), new int[]{0});
        EPAssertionUtil.assertEqualsExactOrder(substreamsPerStream.get(4), new int[]{});
        EPAssertionUtil.assertEqualsExactOrder(substreamsPerStream.get(5), new int[]{});
        EPAssertionUtil.assertEqualsExactOrder(substreamsPerStream.get(0), new int[]{});

        NStreamOuterQueryPlanBuilder.verifyJoinedPerStream(2, substreamsPerStream);
        EPAssertionUtil.assertEqualsExactOrder(requiredPerStream, new boolean[]{false, false, false, true, true, false}
        );

    }

    public void testVerifyJoinedPerStream() {
        // stream relationships not filled
        tryVerifyJoinedPerStream(convert(new int[][]{{1, 2}}));

        // stream relationships duplicates
        tryVerifyJoinedPerStream(convert(new int[][]{{1, 2}, {1}, {}}));
        tryVerifyJoinedPerStream(convert(new int[][]{{1, 2}, {}, {2}}));

        // stream relationships out of range
        tryVerifyJoinedPerStream(convert(new int[][]{{1, 3}, {}, {}}));

        // stream relationships missing stream
        tryVerifyJoinedPerStream(convert(new int[][]{{1}, {}, {}}));
    }

    private void tryVerifyJoinedPerStream(Map<Integer, int[]> map) {
        try {
            NStreamOuterQueryPlanBuilder.verifyJoinedPerStream(0, map);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    private void assertInners(int[][] innersPerStream, OuterInnerDirectionalGraph graph) {
        for (int i = 0; i < innersPerStream.length; i++) {
            EPAssertionUtil.assertEqualsAnyOrder(innersPerStream[i], graph.getInner(i));
        }
    }

    private void assertOuters(int[][] outersPerStream, OuterInnerDirectionalGraph graph) {
        for (int i = 0; i < outersPerStream.length; i++) {
            EPAssertionUtil.assertEqualsAnyOrder(outersPerStream[i], graph.getOuter(i));
        }
    }

    private Map<Integer, int[]> convert(int[][] array) {
        Map<Integer, int[]> result = new HashMap<Integer, int[]>();
        for (int i = 0; i < array.length; i++) {
            result.put(i, array[i]);
        }
        return result;
    }

}
