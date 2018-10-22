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
package com.espertech.esper.common.internal.epl.spatial.quadtree.mxciffilterindex;

import com.espertech.esper.common.internal.epl.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.common.internal.epl.spatial.quadtree.mxcif.MXCIFQuadTreeNode;
import com.espertech.esper.common.internal.epl.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.common.internal.epl.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;
import com.espertech.esper.common.internal.epl.spatial.quadtree.mxcifrowindex.XYWHRectangleMultiType;

import java.util.Collection;
import java.util.function.Consumer;

public class MXCIFQuadTreeFilterIndexTraverse {
    public static void traverse(MXCIFQuadTree<Object> quadtree, Consumer<Object> consumer) {
        traverse(quadtree.getRoot(), consumer);
    }

    public static void traverse(MXCIFQuadTreeNode<Object> node, Consumer<Object> consumer) {
        if (node instanceof MXCIFQuadTreeNodeLeaf) {
            MXCIFQuadTreeNodeLeaf<Object> leaf = (MXCIFQuadTreeNodeLeaf<Object>) node;
            traverseData(leaf.getData(), consumer);
            return;
        }

        MXCIFQuadTreeNodeBranch<Object> branch = (MXCIFQuadTreeNodeBranch<Object>) node;
        traverseData(branch.getData(), consumer);
        traverse(branch.getNw(), consumer);
        traverse(branch.getNe(), consumer);
        traverse(branch.getSw(), consumer);
        traverse(branch.getSe(), consumer);
    }

    private static void traverseData(Object data, Consumer<Object> consumer) {
        if (data == null) {
            return;
        }
        if (!(data instanceof Collection)) {
            visit(data, consumer);
            return;
        }
        Collection collection = (Collection) data;
        for (Object datapoint : collection) {
            visit(datapoint, consumer);
        }
    }

    private static void visit(Object data, Consumer<Object> consumer) {
        if (data instanceof XYWHRectangleWValue) {
            consumer.accept(((XYWHRectangleWValue) data).getValue());
        } else if (data instanceof XYWHRectangleMultiType) {
            XYWHRectangleMultiType multiType = (XYWHRectangleMultiType) data;
            if (multiType.getMultityped() instanceof Collection) {
                Collection collection = (Collection) multiType.getMultityped();
                for (Object datapoint : collection) {
                    visit(datapoint, consumer);
                }
            }
        }
    }
}
