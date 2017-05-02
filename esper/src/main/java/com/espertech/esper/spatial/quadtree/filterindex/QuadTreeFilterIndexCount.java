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
package com.espertech.esper.spatial.quadtree.filterindex;

import com.espertech.esper.filter.FilterHandleSetNode;
import com.espertech.esper.filter.FilterParamIndexBase;
import com.espertech.esper.spatial.quadtree.core.QuadTree;
import com.espertech.esper.spatial.quadtree.core.QuadTreeNode;
import com.espertech.esper.spatial.quadtree.core.QuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.core.QuadTreeNodeLeaf;

import java.util.Collection;

public class QuadTreeFilterIndexCount {
    public static int count(QuadTree<Object> quadTree) {
        return count(quadTree.getRoot());
    }

    private static int count(QuadTreeNode<Object> node) {
        if (node instanceof QuadTreeNodeLeaf) {
            QuadTreeNodeLeaf<Object> leaf = (QuadTreeNodeLeaf<Object>) node;
            return countLeaf(leaf);
        }
        QuadTreeNodeBranch<Object> branch = (QuadTreeNodeBranch<Object>) node;
        return count(branch.getNw()) + count(branch.getNe()) + count(branch.getSw()) + count(branch.getSe());
    }

    private static int countLeaf(QuadTreeNodeLeaf<Object> leaf) {
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
