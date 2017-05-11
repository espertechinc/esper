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

import com.espertech.esper.filter.FilterHandleSetNode;
import com.espertech.esper.filter.FilterParamIndexBase;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeNode;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeNodeLeaf;

import java.util.Collection;

public class PointRegionQuadTreeFilterIndexCount {
    public static int count(PointRegionQuadTree<Object> quadTree) {
        return count(quadTree.getRoot());
    }

    private static int count(PointRegionQuadTreeNode<Object> node) {
        if (node instanceof PointRegionQuadTreeNodeLeaf) {
            PointRegionQuadTreeNodeLeaf<Object> leaf = (PointRegionQuadTreeNodeLeaf<Object>) node;
            return countLeaf(leaf);
        }
        PointRegionQuadTreeNodeBranch<Object> branch = (PointRegionQuadTreeNodeBranch<Object>) node;
        return count(branch.getNw()) + count(branch.getNe()) + count(branch.getSw()) + count(branch.getSe());
    }

    private static int countLeaf(PointRegionQuadTreeNodeLeaf<Object> leaf) {
        if (leaf.getPoints() == null) {
            return 0;
        }
        if (leaf.getPoints() instanceof XYPointWValue) {
            return countCallbacks(leaf.getPoints());
        }
        Collection<XYPointWValue> coll = (Collection<XYPointWValue>) leaf.getPoints();
        int count = 0;
        for (XYPointWValue p : coll) {
            count += countCallbacks(p.getValue());
        }
        return count;
    }

    private static int countCallbacks(Object points) {
        if (points instanceof FilterHandleSetNode) {
            return ((FilterHandleSetNode) points).getFilterCallbackCount();
        }
        if (points instanceof FilterParamIndexBase) {
            return ((FilterParamIndexBase) points).sizeExpensive();
        }
        return 1;
    }
}
