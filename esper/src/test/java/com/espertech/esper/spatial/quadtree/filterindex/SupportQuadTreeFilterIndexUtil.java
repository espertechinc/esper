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
package com.espertech.esper.spatial.quadtree.filterindex;

import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.core.QuadTree;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class SupportQuadTreeFilterIndexUtil {
    private static final QuadTreeCollector<String, Map<Integer, String>> MAP_COLLECTOR = (event, s, target) -> {
        int num = Integer.parseInt(s.substring(1));
        if (target.containsKey(num)) {
            throw new IllegalStateException();
        }
        target.put(num, s);
    };

    private static final QuadTreeCollector<String, Collection<Object>> COLLECTION_COLLECTOR = (event, s, target) -> target.add(s);

    public final static SupportQuadTreeUtil.Querier FILTERINDEX_QUERIER = (tree, x, y, width, height) -> {
        List<Object> received = new ArrayList<>();
        QuadTreeFilterIndexCollect.collectRange(tree, x, y, width, height, null, received, COLLECTION_COLLECTOR);
        return received.isEmpty() ? null : received;
    };
    public final static SupportQuadTreeUtil.AdderUnique FILTERINDEX_ADDER = (tree, value) -> set(tree, value.getX(), value.getY(), value.getId());
    public final static SupportQuadTreeUtil.Remover FILTERINDEX_REMOVER = (tree, value) -> delete(tree, value.getX(), value.getY());

    protected static void set(QuadTree<Object> quadTree, double x, double y, String value) {
        QuadTreeFilterIndexSet.set(x, y, value, quadTree);
    }

    protected static void delete(QuadTree<Object> tree, double x, double y) {
        QuadTreeFilterIndexDelete.delete(x, y, tree);
    }

    static void assertCollectAll(QuadTree<Object> tree, String expected) {
        BoundingBox bb = tree.getRoot().getBb();
        assertCollect(tree, bb.getMinX(), bb.getMinY(), bb.getMaxX() - bb.getMinX(), bb.getMaxY() - bb.getMinY(), expected);
        assertEquals(expected.length() == 0 ? 0 : expected.split(",").length, QuadTreeFilterIndexCount.count(tree));
        assertEquals(expected.length() == 0, QuadTreeFilterIndexEmpty.isEmpty(tree));
    }

    static void assertCollect(QuadTree<Object> tree, double x, double y, double width, double height, String expected) {
        Map<Integer, String> received = new TreeMap<>();
        QuadTreeFilterIndexCollect.collectRange(tree, x, y, width, height, null, received, MAP_COLLECTOR);
        assertCompare(tree, expected, received);
    }

    private static void assertCompare(QuadTree<Object> tree, String expected, Map<Integer, String> received) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : received.values()) {
            joiner.add(value);
        }
        assertEquals(expected, joiner.toString());
        assertTrue((expected.length() == 0 ? 0 : expected.split(",").length) <= QuadTreeFilterIndexCount.count(tree));
    }

    protected static void compare(double x, double y, String expected, XYPointWValue<String> point) {
        assertEquals(x, point.getX());
        assertEquals(y, point.getY());
        assertEquals(expected, point.getValue());
    }
}
