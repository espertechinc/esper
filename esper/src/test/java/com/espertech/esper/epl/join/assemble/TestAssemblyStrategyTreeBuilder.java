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
package com.espertech.esper.epl.join.assemble;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TestAssemblyStrategyTreeBuilder extends TestCase {
    public void testInvalidBuild() {
        // root stream out of bounds
        tryInvalidBuild(3, convert(new int[][]{{1, 2}, {}, {}}), new boolean[]{true, true, true});
        tryInvalidBuild(-1, convert(new int[][]{{1, 2}, {}, {}}), new boolean[]{true, true, true});

        // not matching outer-inner
        tryInvalidBuild(0, convert(new int[][]{{1, 2}, {}, {}}), new boolean[]{true, true});

        // stream relationships not filled
        tryInvalidBuild(0, convert(new int[][]{{1, 2}}), new boolean[]{true, true, true});

        // stream relationships duplicates
        tryInvalidBuild(0, convert(new int[][]{{1, 2}, {1}, {}}), new boolean[]{true, true});
        tryInvalidBuild(0, convert(new int[][]{{1, 2}, {}, {2}}), new boolean[]{true, true, true});

        // stream relationships out of range
        tryInvalidBuild(0, convert(new int[][]{{1, 3}, {}, {}}), new boolean[]{true, true});

        // stream relationships missing stream
        tryInvalidBuild(0, convert(new int[][]{{1}, {}, {}}), new boolean[]{true, true});
    }

    public void testValidBuildSimpleReqOpt() {
        BaseAssemblyNodeFactory nodeFactory = AssemblyStrategyTreeBuilder.build(2, convert(new int[][]{{}, {0}, {1}}), new boolean[]{false, true, true});

        RootRequiredAssemblyNodeFactory child1 = (RootRequiredAssemblyNodeFactory) nodeFactory;
        assertEquals(2, child1.getStreamNum());
        assertEquals(1, child1.getChildNodes().size());
        assertEquals(null, child1.getParentNode());

        BranchOptionalAssemblyNodeFactory child1_1 = (BranchOptionalAssemblyNodeFactory) child1.getChildNodes().get(0);
        assertEquals(1, child1_1.getStreamNum());
        assertEquals(1, child1_1.getChildNodes().size());
        assertEquals(child1, child1_1.getParentNode());

        LeafAssemblyNodeFactory leaf1_2 = (LeafAssemblyNodeFactory) child1_1.getChildNodes().get(0);
        assertEquals(0, leaf1_2.getStreamNum());
        assertEquals(0, leaf1_2.getChildNodes().size());
        assertEquals(child1_1, leaf1_2.getParentNode());
    }

    public void testValidBuildSimpleOptReq() {
        BaseAssemblyNodeFactory nodeFactory = AssemblyStrategyTreeBuilder.build(2, convert(new int[][]{{}, {0}, {1}}), new boolean[]{true, false, true});

        RootOptionalAssemblyNodeFactory child1 = (RootOptionalAssemblyNodeFactory) nodeFactory;
        assertEquals(2, child1.getStreamNum());
        assertEquals(1, child1.getChildNodes().size());
        assertEquals(null, child1.getParentNode());

        BranchRequiredAssemblyNodeFactory child1_1 = (BranchRequiredAssemblyNodeFactory) child1.getChildNodes().get(0);
        assertEquals(1, child1_1.getStreamNum());
        assertEquals(1, child1_1.getChildNodes().size());
        assertEquals(child1, child1_1.getParentNode());

        LeafAssemblyNodeFactory leaf1_2 = (LeafAssemblyNodeFactory) child1_1.getChildNodes().get(0);
        assertEquals(0, leaf1_2.getStreamNum());
        assertEquals(0, leaf1_2.getChildNodes().size());
        assertEquals(child1_1, leaf1_2.getParentNode());
    }

    public void testValidBuildCartesian() {
        BaseAssemblyNodeFactory nodeFactory = AssemblyStrategyTreeBuilder.build(1, convert(new int[][]{{}, {0, 2}, {}}), new boolean[]{false, true, false});

        RootCartProdAssemblyNodeFactory top = (RootCartProdAssemblyNodeFactory) nodeFactory;
        assertEquals(2, top.getChildNodes().size());

        LeafAssemblyNodeFactory leaf1 = (LeafAssemblyNodeFactory) top.getChildNodes().get(0);
        assertEquals(0, leaf1.getStreamNum());
        assertEquals(0, leaf1.getChildNodes().size());
        assertEquals(top, leaf1.getParentNode());

        LeafAssemblyNodeFactory leaf2 = (LeafAssemblyNodeFactory) top.getChildNodes().get(0);
        assertEquals(0, leaf2.getStreamNum());
        assertEquals(0, leaf2.getChildNodes().size());
        assertEquals(top, leaf2.getParentNode());
    }

    private void tryInvalidBuild(int rootStream, Map<Integer, int[]> joinedPerStream, boolean[] isInnerPerStream) {
        try {
            AssemblyStrategyTreeBuilder.build(rootStream, joinedPerStream, isInnerPerStream);
            fail();
        } catch (IllegalArgumentException ex) {
            log.debug(".tryInvalidBuild expected exception=" + ex);
            // expected
        }
    }

    private Map<Integer, int[]> convert(int[][] array) {
        Map<Integer, int[]> result = new HashMap<Integer, int[]>();
        for (int i = 0; i < array.length; i++) {
            result.put(i, array[i]);
        }
        return result;
    }

    private static final Logger log = LoggerFactory.getLogger(TestAssemblyStrategyTreeBuilder.class);
}
