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
package com.espertech.esper.spatial.quadtree.user;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.spatial.quadtree.core.QuadTree;
import com.espertech.esper.spatial.quadtree.core.QuadTreeNode;
import com.espertech.esper.spatial.quadtree.core.QuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.core.QuadTreeNodeLeaf;

import java.util.Arrays;
import java.util.Collection;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

public class SupportQuadTreeUtil {

    protected static void remove(QuadTree<Object> quadTree, double x, double y, String value) {
        QuadTreeToolRemove.remove(x, y, value, quadTree);
    }

    protected static boolean addNonUnique(QuadTree<Object> quadTree, double x, double y, String value) {
        return QuadTreeToolAdd.add(x, y, value, quadTree, false, "indexNameDummy");
    }

    protected static boolean addUnique(QuadTree<Object> quadTree, double x, double y, String value) {
        return QuadTreeToolAdd.add(x, y, value, quadTree, true, "indexNameDummy");
    }

    protected static void assertFound(QuadTree<Object> quadTree, double x, double y, double width, double height, String p1) {
        Object[] expected = p1.length() == 0 ? null : p1.split(",");
        assertFound(quadTree, x, y, width, height, expected);
    }

    protected static void assertFound(QuadTree<Object> quadTree, double x, double y, double width, double height, Object[] ids) {
        Collection<Object> values = QuadTreeToolQuery.queryRange(quadTree, x, y, width, height);
        if (ids == null || ids.length == 0) {
            assertTrue(values == null);
        } else {
            if (values == null) {
                fail("Nothing returned, expected " + Arrays.asList(ids));
            }
            EPAssertionUtil.assertEqualsAnyOrder(ids, values.toArray());
        }
    }

    protected static QuadTreeNodeLeaf<Object> navigateLeaf(QuadTree<Object> tree, String directions) {
        return (QuadTreeNodeLeaf<Object>) navigate(tree, directions);
    }

    protected static QuadTreeNodeLeaf<Object> navigateLeaf(QuadTreeNode<Object> node, String directions) {
        return (QuadTreeNodeLeaf<Object>) navigate(node, directions);
    }

    protected static QuadTreeNodeBranch<Object> navigateBranch(QuadTree<Object> tree, String directions) {
        return (QuadTreeNodeBranch<Object>) navigate(tree, directions);
    }

    protected static QuadTreeNode<Object> navigate(QuadTree<Object> tree, String directions) {
        return navigate(tree.getRoot(), directions);
    }

    protected static QuadTreeNode<Object> navigate(QuadTreeNode<Object> current, String directions) {
        if (directions.isEmpty()) {
            return current;
        }
        String[] split = directions.split(",");
        for (int i = 0; i < split.length; i++) {
            QuadTreeNodeBranch<Object> branch = (QuadTreeNodeBranch<Object>) current;
            if (split[i].equals("nw")) {
                current = branch.getNw();
            }
            else if (split[i].equals("ne")) {
                current = branch.getNe();
            }
            else if (split[i].equals("sw")) {
                current = branch.getSw();
            }
            else if (split[i].equals("se")) {
                current = branch.getSe();
            }
            else {
                throw new IllegalArgumentException("Invalid direction " + split[i]);
            }
        }
        return current;
    }

    protected static void compare(double x, double y, String expected, XYPointMultiType point) {
        assertEquals(x, point.getX());
        assertEquals(y, point.getY());
        assertEquals(expected, point.getMultityped().toString());
    }
}
