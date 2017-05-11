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

import com.espertech.esper.spatial.quadtree.core.QuadrantAppliesEnum;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNode;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;

import java.util.Collection;

import static com.espertech.esper.spatial.quadtree.mxcifrowindex.MXCIFQuadTreeFilterIndexCheckBB.checkBB;

public class MXCIFQuadTreeFilterIndexGet {
    public static <L> L get(double x, double y, double width, double height, MXCIFQuadTree<Object> tree) {
        checkBB(tree.getRoot().getBb(), x, y, width, height);
        return get(x, y, width, height, tree.getRoot());
    }

    private static <L> L get(double x, double y, double width, double height, MXCIFQuadTreeNode<Object> node) {
        if (node instanceof MXCIFQuadTreeNodeLeaf) {
            MXCIFQuadTreeNodeLeaf<Object> leaf = (MXCIFQuadTreeNodeLeaf<Object>) node;
            return getFromData(x, y, width, height, leaf.getData());
        }

        MXCIFQuadTreeNodeBranch<Object> branch = (MXCIFQuadTreeNodeBranch<Object>) node;
        QuadrantAppliesEnum q = node.getBb().getQuadrantApplies(x, y, width, height);
        if (q == QuadrantAppliesEnum.NW) {
            return get(x, y, width, height, branch.getNw());
        } else if (q == QuadrantAppliesEnum.NE) {
            return get(x, y, width, height, branch.getNe());
        } else if (q == QuadrantAppliesEnum.SW) {
            return get(x, y, width, height, branch.getSw());
        } else if (q == QuadrantAppliesEnum.SE) {
            return get(x, y, width, height, branch.getSe());
        } else if (q == QuadrantAppliesEnum.SOME) {
            return getFromData(x, y, width, height, branch.getData());
        } else {
            throw new IllegalStateException("Not applicable to any quadrant");
        }
    }

    private static <L> L getFromData(double x, double y, double width, double height, Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof XYWHRectangleWValue) {
            XYWHRectangleWValue<L> value = (XYWHRectangleWValue<L>) data;
            if (value.coordinateEquals(x, y, width, height)) {
                return value.getValue();
            }
            return null;
        }
        Collection<XYWHRectangleWValue<L>> collection = (Collection<XYWHRectangleWValue<L>>) data;
        for (XYWHRectangleWValue<L> rectangle : collection) {
            if (rectangle.coordinateEquals(x, y, width, height)) {
                return rectangle.getValue();
            }
        }
        return null;
    }
}
