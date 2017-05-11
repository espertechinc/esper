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
package com.espertech.esper.spatial.quadtree.mxcifrowindex;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.SupportRectangleWithId;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.spatial.quadtree.prqdrowindex.PointRegionQuadTreeRowIndexRemove;
import com.espertech.esper.spatial.quadtree.prqdrowindex.XYPointMultiType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

public class SupportMXCIFQuadTreeRowIndexUtil {

    public final static SupportQuadTreeUtil.AdderUnique<MXCIFQuadTree<Object>> MXCIF_RI_ADDERUNIQUE = (tree, value) -> SupportMXCIFQuadTreeRowIndexUtil.addUnique(tree, value.getX(), value.getY(), value.getW(), value.getH(), value.getId());
    public final static SupportQuadTreeUtil.Remover<MXCIFQuadTree<Object>> MXCIF_RI_REMOVER = (tree, value) -> remove(tree, value.getX(), value.getY(), value.getW(), value.getH(), value.getId());
    public final static SupportQuadTreeUtil.Querier<MXCIFQuadTree<Object>> MXCIF_RI_QUERIER = SupportMXCIFQuadTreeRowIndexUtil::queryWLog;
    public final static SupportQuadTreeUtil.AdderNonUnique<MXCIFQuadTree<Object>> MXCIF_RI_ADDERNONUNIQUE = (tree, value) -> SupportMXCIFQuadTreeRowIndexUtil.addNonUnique(tree, value.getX(), value.getY(), value.getW(), value.getH(), value.getId());

    protected static Collection<Object> queryWLog(MXCIFQuadTree<Object> quadTree, double x, double y, double width, double height) {
        Collection<Object> values = MXCIFQuadTreeRowIndexQuery.queryRange(quadTree, x, y, width, height);
        // Comment-me-in: System.out.println("// query(tree, " + x + ", " + y + ", " + width + ", " + height + "); --> " + values);
        return values;
    }

    protected static void remove(MXCIFQuadTree<Object> quadTree, double x, double y, double width, double height, String value) {
        // Comment-me-in: System.out.println("remove(tree, " + x + ", " + y + ", " + width + ", " + height + ", \"" + value + "\");");
        MXCIFQuadTreeRowIndexRemove.remove(x, y, width, height, value, quadTree);
    }

    protected static boolean addNonUnique(MXCIFQuadTree<Object> quadTree, double x, double y, double width, double height, String value) {
        // Comment-me-in: System.out.println("addNonUnique(tree, " + x + ", " + y + ", " + width + ", " + height + ", \"" + value + "\");");
        return MXCIFQuadTreeRowIndexAdd.add(x, y, width, height, value, quadTree, false, "indexNameDummy");
    }

    public static void addUnique(MXCIFQuadTree<Object> tree, double x, double y, double width, double height, String value) {
        // Comment-me-in: System.out.println("addUnique(tree, " + x + ", " + y + ", " + width + ", " + height + ", \"" + value + "\");");
        MXCIFQuadTreeRowIndexAdd.add(x, y, width, height, value, tree, true, "indexNameHere");
    }

    protected static void assertFound(MXCIFQuadTree<Object> quadTree, double x, double y, double width, double height, String p1) {
        Object[] expected = p1.length() == 0 ? null : p1.split(",");
        assertFound(quadTree, x, y, width, height, expected);
    }

    protected static void assertFound(MXCIFQuadTree<Object> quadTree, double x, double y, double width, double height, Object[] ids) {
        Collection<Object> values = MXCIFQuadTreeRowIndexQuery.queryRange(quadTree, x, y, width, height);
        if (ids == null || ids.length == 0) {
            assertTrue(values == null);
        } else {
            if (values == null) {
                fail("Nothing returned, expected " + Arrays.asList(ids));
            }
            EPAssertionUtil.assertEqualsAnyOrder(ids, values.toArray());
        }
    }

    protected static void compare(double x, double y, double width, double height, String expected, XYWHRectangleMultiType rectangle) {
        assertEquals(x, rectangle.getX());
        assertEquals(y, rectangle.getY());
        assertEquals(width, rectangle.getW());
        assertEquals(height, rectangle.getH());
        assertEquals(expected, rectangle.getMultityped().toString());
    }
}
