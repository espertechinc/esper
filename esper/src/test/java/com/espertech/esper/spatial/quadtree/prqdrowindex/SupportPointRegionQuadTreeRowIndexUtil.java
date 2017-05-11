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
package com.espertech.esper.spatial.quadtree.prqdrowindex;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;

import java.util.Arrays;
import java.util.Collection;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

public class SupportPointRegionQuadTreeRowIndexUtil {

    public final static SupportQuadTreeUtil.Querier<PointRegionQuadTree<Object>> POINTREGION_RI_QUERIER = (tree, x, y, width, height) -> PointRegionQuadTreeRowIndexQuery.queryRange(tree, x, y, width, height);
    public final static SupportQuadTreeUtil.AdderUnique<PointRegionQuadTree<Object>> POINTREGION_RI_ADDERUNIQUE = (tree, value) -> addUnique(tree, value.getX(), value.getY(), value.getId());
    public final static SupportQuadTreeUtil.Remover<PointRegionQuadTree<Object>> POINTREGION_RI_REMOVER = (tree, value) -> remove(tree, value.getX(), value.getY(), value.getId());
    public final static SupportQuadTreeUtil.AdderNonUnique<PointRegionQuadTree<Object>> POINTREGION_RI_ADDERNONUNIQUE = (tree, value) -> addNonUnique(tree, value.getX(), value.getY(), value.getId());

    protected static void remove(PointRegionQuadTree<Object> quadTree, double x, double y, String value) {
        PointRegionQuadTreeRowIndexRemove.remove(x, y, value, quadTree);
    }

    protected static boolean addNonUnique(PointRegionQuadTree<Object> quadTree, double x, double y, String value) {
        return PointRegionQuadTreeRowIndexAdd.add(x, y, value, quadTree, false, "indexNameDummy");
    }

    protected static boolean addUnique(PointRegionQuadTree<Object> quadTree, double x, double y, String value) {
        return PointRegionQuadTreeRowIndexAdd.add(x, y, value, quadTree, true, "indexNameDummy");
    }

    protected static void assertFound(PointRegionQuadTree<Object> quadTree, double x, double y, double width, double height, String p1) {
        Object[] expected = p1.length() == 0 ? null : p1.split(",");
        assertFound(quadTree, x, y, width, height, expected);
    }

    protected static void assertFound(PointRegionQuadTree<Object> quadTree, double x, double y, double width, double height, Object[] ids) {
        Collection<Object> values = PointRegionQuadTreeRowIndexQuery.queryRange(quadTree, x, y, width, height);
        if (ids == null || ids.length == 0) {
            assertTrue(values == null);
        } else {
            if (values == null) {
                fail("Nothing returned, expected " + Arrays.asList(ids));
            }
            EPAssertionUtil.assertEqualsAnyOrder(ids, values.toArray());
        }
    }

    protected static void compare(double x, double y, String expected, XYPointMultiType point) {
        assertEquals(x, point.getX());
        assertEquals(y, point.getY());
        assertEquals(expected, point.getMultityped().toString());
    }
}
