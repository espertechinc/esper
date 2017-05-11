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

import com.espertech.esper.spatial.quadtree.core.*;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeNode;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeNodeLeaf;

import java.util.Collection;

import static com.espertech.esper.spatial.quadtree.prqdfilterindex.PointRegionQuadTreeFilterIndexCheckBB.checkBB;

public class PointRegionQuadTreeFilterIndexGet {
    public static <L> L get(double x, double y, PointRegionQuadTree<Object> tree) {
        checkBB(tree.getRoot().getBb(), x, y);
        return get(x, y, tree.getRoot());
    }

    private static <L> L get(double x, double y, PointRegionQuadTreeNode<Object> node) {
        if (node instanceof PointRegionQuadTreeNodeLeaf) {
            PointRegionQuadTreeNodeLeaf<Object> leaf = (PointRegionQuadTreeNodeLeaf<Object>) node;
            if (leaf.getPoints() == null) {
                return null;
            }
            if (leaf.getPoints() instanceof XYPointWValue) {
                XYPointWValue<L> value = (XYPointWValue<L>) leaf.getPoints();
                if (value.getX() == x && value.getY() == y) {
                    return value.getValue();
                }
                return null;
            }
            Collection<XYPointWValue<L>> collection = (Collection<XYPointWValue<L>>) leaf.getPoints();
            for (XYPointWValue<L> point : collection) {
                if (point.getX() == x && point.getY() == y) {
                    return point.getValue();
                }
            }
            return null;
        }

        PointRegionQuadTreeNodeBranch<Object> branch = (PointRegionQuadTreeNodeBranch<Object>) node;
        QuadrantEnum q = node.getBb().getQuadrant(x, y);
        if (q == QuadrantEnum.NW) {
            return get(x, y, branch.getNw());
        } else if (q == QuadrantEnum.NE) {
            return get(x, y, branch.getNe());
        } else if (q == QuadrantEnum.SW) {
            return get(x, y, branch.getSw());
        } else {
            return get(x, y, branch.getSe());
        }
    }
}
