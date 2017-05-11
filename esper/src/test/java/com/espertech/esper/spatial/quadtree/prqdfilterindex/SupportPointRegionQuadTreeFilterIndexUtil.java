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
package com.espertech.esper.spatial.quadtree.prqdfilterindex;

import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.core.QuadTreeCollector;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class SupportPointRegionQuadTreeFilterIndexUtil {
    private static final QuadTreeCollector<String, Map<Integer, String>> MAP_COLLECTOR = (event, s, target) -> {
        int num = Integer.parseInt(s.substring(1));
        if (target.containsKey(num)) {
            throw new IllegalStateException();
        }
        target.put(num, s);
    };

    private static final QuadTreeCollector<String, Collection<Object>> COLLECTION_COLLECTOR = (event, s, target) -> target.add(s);

    public final static SupportQuadTreeUtil.Querier<PointRegionQuadTree<Object>> POINTREGION_FI_QUERIER = (tree, x, y, width, height) -> {
        List<Object> received = new ArrayList<>();
        PointRegionQuadTreeFilterIndexCollect.collectRange(tree, x, y, width, height, null, received, COLLECTION_COLLECTOR);
        return received.isEmpty() ? null : received;
    };
    public final static SupportQuadTreeUtil.AdderUnique<PointRegionQuadTree<Object>> POINTREGION_FI_ADDERUNIQUE = (tree, value) -> set(tree, value.getX(), value.getY(), value.getId());
    public final static SupportQuadTreeUtil.Remover<PointRegionQuadTree<Object>> POINTREGION_FI_REMOVER = (tree, value) -> delete(tree, value.getX(), value.getY());

    protected static void set(PointRegionQuadTree<Object> quadTree, double x, double y, String value) {
        PointRegionQuadTreeFilterIndexSet.set(x, y, value, quadTree);
    }

    protected static void delete(PointRegionQuadTree<Object> tree, double x, double y) {
        PointRegionQuadTreeFilterIndexDelete.delete(x, y, tree);
    }

    static void assertCollectAll(PointRegionQuadTree<Object> tree, String expected) {
        BoundingBox bb = tree.getRoot().getBb();
        assertCollect(tree, bb.getMinX(), bb.getMinY(), bb.getMaxX() - bb.getMinX(), bb.getMaxY() - bb.getMinY(), expected);
        assertEquals(expected.length() == 0 ? 0 : expected.split(",").length, PointRegionQuadTreeFilterIndexCount.count(tree));
        assertEquals(expected.length() == 0, PointRegionQuadTreeFilterIndexEmpty.isEmpty(tree));
    }

    static void assertCollect(PointRegionQuadTree<Object> tree, double x, double y, double width, double height, String expected) {
        Map<Integer, String> received = new TreeMap<>();
        PointRegionQuadTreeFilterIndexCollect.collectRange(tree, x, y, width, height, null, received, MAP_COLLECTOR);
        assertCompare(tree, expected, received);
    }

    private static void assertCompare(PointRegionQuadTree<Object> tree, String expected, Map<Integer, String> received) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : received.values()) {
            joiner.add(value);
        }
        assertEquals(expected, joiner.toString());
        assertTrue((expected.length() == 0 ? 0 : expected.split(",").length) <= PointRegionQuadTreeFilterIndexCount.count(tree));
    }

    protected static void compare(double x, double y, String expected, XYPointWValue<String> point) {
        assertEquals(x, point.getX());
        assertEquals(y, point.getY());
        assertEquals(expected, point.getValue());
    }
}
