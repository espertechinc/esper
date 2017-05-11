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

import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNode;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;

import java.util.ArrayDeque;
import java.util.Collection;

public class MXCIFQuadTreeRowIndexQuery {
    public static Collection<Object> queryRange(MXCIFQuadTree<Object> quadTree, double x, double y, double width, double height) {
        return queryNode(quadTree.getRoot(), x, y, width, height, null);
    }

    private static Collection<Object> queryNode(MXCIFQuadTreeNode<Object> node, double x, double y, double width, double height, Collection<Object> result) {
        if (node instanceof MXCIFQuadTreeNodeLeaf) {
            MXCIFQuadTreeNodeLeaf<Object> leaf = (MXCIFQuadTreeNodeLeaf<Object>) node;
            return visit(leaf, x, y, width, height, result);
        }

        MXCIFQuadTreeNodeBranch<Object> branch = (MXCIFQuadTreeNodeBranch<Object>) node;
        result = visit(branch, x, y, width, height, result);
        result = queryNode(branch.getNw(), x, y, width, height, result);
        result = queryNode(branch.getNe(), x, y, width, height, result);
        result = queryNode(branch.getSw(), x, y, width, height, result);
        result = queryNode(branch.getSe(), x, y, width, height, result);
        return result;
    }

    private static Collection<Object> visit(MXCIFQuadTreeNode<Object> node, double x, double y, double width, double height, Collection<Object> result) {
        Object data = node.getData();
        if (data == null) {
            return result;
        }
        if (data instanceof XYWHRectangleMultiType) {
            XYWHRectangleMultiType point = (XYWHRectangleMultiType) data;
            return visit(point, x, y, width, height, result);
        }
        Collection<XYWHRectangleMultiType> collection = (Collection<XYWHRectangleMultiType>) data;
        for (XYWHRectangleMultiType rectangle : collection) {
            result = visit(rectangle, x, y, width, height, result);
        }
        return result;
    }

    private static Collection<Object> visit(XYWHRectangleMultiType rectangle, double x, double y, double width, double height, Collection<Object> result) {
        if (!BoundingBox.intersectsBoxIncludingEnd(x, y, x + width, y + height, rectangle.getX(), rectangle.getY(), rectangle.getW(), rectangle.getH())) {
            return result;
        }
        if (result == null) {
            result = new ArrayDeque<>(4);
        }
        rectangle.collectInto(result);
        return result;
    }
}
