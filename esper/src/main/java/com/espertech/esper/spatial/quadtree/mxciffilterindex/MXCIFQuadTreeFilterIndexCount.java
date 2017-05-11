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

import com.espertech.esper.filter.FilterHandleSetNode;
import com.espertech.esper.filter.FilterParamIndexBase;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNode;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;

import java.util.Collection;

public class MXCIFQuadTreeFilterIndexCount {
    public static int count(MXCIFQuadTree<Object> quadTree) {
        return count(quadTree.getRoot());
    }

    private static int count(MXCIFQuadTreeNode<Object> node) {
        if (node instanceof MXCIFQuadTreeNodeLeaf) {
            MXCIFQuadTreeNodeLeaf<Object> leaf = (MXCIFQuadTreeNodeLeaf<Object>) node;
            return countData(leaf.getData());
        }
        MXCIFQuadTreeNodeBranch<Object> branch = (MXCIFQuadTreeNodeBranch<Object>) node;
        return count(branch.getNw()) + count(branch.getNe()) + count(branch.getSw()) + count(branch.getSe()) + countData(branch.getData());
    }

    private static int countData(Object data) {
        if (data == null) {
            return 0;
        }
        if (data instanceof XYWHRectangleWValue) {
            return countCallbacks(data);
        }
        Collection<XYWHRectangleWValue> coll = (Collection<XYWHRectangleWValue>) data;
        int count = 0;
        for (XYWHRectangleWValue p : coll) {
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
