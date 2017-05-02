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
package com.espertech.esper.spatial.quadtree.rowindex;

import com.espertech.esper.spatial.quadtree.core.*;

import java.util.ArrayDeque;
import java.util.Collection;

public class QuadTreeRowIndexQuery {
    public static Collection<Object> queryRange(QuadTree<Object> quadTree, double x, double y, double width, double height) {
        return queryNode(quadTree.getRoot(), x, y, width, height, null);
    }

    private static Collection<Object> queryNode(QuadTreeNode<Object> node, double x, double y, double width, double height, Collection<Object> result) {
        if (!node.getBb().intersectsBox(x, y, width, height)) {
            return result;
        }
        if (node instanceof QuadTreeNodeLeaf) {
            QuadTreeNodeLeaf<Object> leaf = (QuadTreeNodeLeaf<Object>) node;
            return visit(leaf, x, y, width, height, result);
        }

        QuadTreeNodeBranch<Object> branch = (QuadTreeNodeBranch<Object>) node;
        result = queryNode(branch.getNw(), x, y, width, height, result);
        result = queryNode(branch.getNe(), x, y, width, height, result);
        result = queryNode(branch.getSw(), x, y, width, height, result);
        result = queryNode(branch.getSe(), x, y, width, height, result);
        return result;
    }

    private static Collection<Object> visit(QuadTreeNodeLeaf node, double x, double y, double width, double height, Collection<Object> result) {
        Object points = node.getPoints();
        if (points == null) {
            return result;
        }
        if (points instanceof XYPointMultiType) {
            XYPointMultiType point = (XYPointMultiType) points;
            return visit(point, x, y, width, height, result);
        }
        Collection<XYPointMultiType> collection = (Collection<XYPointMultiType>) points;
        for (XYPointMultiType point : collection) {
            result = visit(point, x, y, width, height, result);
        }
        return result;
    }

    private static Collection<Object> visit(XYPointMultiType point, double x, double y, double width, double height, Collection<Object> result) {
        if (!BoundingBox.containsPoint(x, y, width, height, point.getX(), point.getY())) {
            return result;
        }
        if (result == null) {
            result = new ArrayDeque<>(4);
        }
        point.collectInto(result);
        return result;
    }
}
