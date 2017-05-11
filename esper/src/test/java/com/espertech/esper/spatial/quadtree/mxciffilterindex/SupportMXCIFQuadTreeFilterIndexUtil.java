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
package com.espertech.esper.spatial.quadtree.mxciffilterindex;

import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.core.QuadTreeCollector;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcifrowindex.MXCIFQuadTreeRowIndexAdd;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class SupportMXCIFQuadTreeFilterIndexUtil {

    private static final QuadTreeCollector<String, Collection<Object>> COLLECTION_COLLECTOR = (event, s, target) -> target.add(s);

    private static final QuadTreeCollector<String, Map<Integer, String>> MAP_COLLECTOR = (event, s, target) -> {
        int num = Integer.parseInt(s.substring(1));
        if (target.containsKey(num)) {
            throw new IllegalStateException();
        }
        target.put(num, s);
    };

    public final static SupportQuadTreeUtil.Querier<MXCIFQuadTree<Object>> MXCIF_FI_QUERIER = (tree, x, y, width, height) -> {
        List<Object> received = new ArrayList<>();
        MXCIFQuadTreeFilterIndexCollect.collectRange(tree, x, y, width, height, null, received, COLLECTION_COLLECTOR);
        // Comment-me-in: System.out.println("// query(tree, " + x + ", " + y + ", " + width + ", " + height + "); --> " + received);
        return received.isEmpty() ? null : received;
    };
    public final static SupportQuadTreeUtil.AdderUnique<MXCIFQuadTree<Object>> MXCIF_FI_ADDER = (tree, value) -> set(value.getX(), value.getY(), value.getW(), value.getH(), value.getId(), tree);
    public final static SupportQuadTreeUtil.Remover<MXCIFQuadTree<Object>> MXCIF_FI_REMOVER = (tree, value) -> MXCIFQuadTreeFilterIndexDelete.delete(value.getX(), value.getY(), value.getW(), value.getH(), tree);

    public static void set(double x, double y, double width, double height, String value, MXCIFQuadTree<Object> tree) {
        // Comment-me-in: System.out.println("set(" + x + ", " + y + ", " + width + ", " + height + ", \"" + value + "\", tree);");
        MXCIFQuadTreeFilterIndexSet.set(x, y, width, height, value, tree);
    }

    static void assertCollectAll(MXCIFQuadTree<Object> tree, String expected) {
        BoundingBox bb = tree.getRoot().getBb();
        assertCollect(tree, bb.getMinX(), bb.getMinY(), bb.getMaxX() - bb.getMinX(), bb.getMaxY() - bb.getMinY(), expected);
        assertEquals(expected.length() == 0 ? 0 : expected.split(",").length, MXCIFQuadTreeFilterIndexCount.count(tree));
        assertEquals(expected.length() == 0, MXCIFQuadTreeFilterIndexEmpty.isEmpty(tree));
    }

    static void assertCollect(MXCIFQuadTree<Object> tree, double x, double y, double width, double height, String expected) {
        Map<Integer, String> received = new TreeMap<>();
        MXCIFQuadTreeFilterIndexCollect.collectRange(tree, x, y, width, height, null, received, MAP_COLLECTOR);
        assertCompare(tree, expected, received);
    }

    private static void assertCompare(MXCIFQuadTree<Object> tree, String expected, Map<Integer, String> received) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : received.values()) {
            joiner.add(value);
        }
        assertEquals(expected, joiner.toString());
        assertTrue((expected.length() == 0 ? 0 : expected.split(",").length) <= MXCIFQuadTreeFilterIndexCount.count(tree));
    }

    protected static void compare(double x, double y, double width, double height, String expected, XYWHRectangleWValue<String> rectangle) {
        assertEquals(x, rectangle.getX());
        assertEquals(y, rectangle.getY());
        assertEquals(width, rectangle.getW());
        assertEquals(height, rectangle.getH());
        assertEquals(expected, rectangle.getValue());
    }
}
