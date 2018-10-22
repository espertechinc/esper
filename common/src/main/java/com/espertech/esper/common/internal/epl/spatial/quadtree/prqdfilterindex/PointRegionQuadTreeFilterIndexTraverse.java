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
package com.espertech.esper.common.internal.epl.spatial.quadtree.prqdfilterindex;

import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTreeNode;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTreeNodeBranch;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTreeNodeLeaf;
import com.espertech.esper.common.internal.epl.spatial.quadtree.prqdrowindex.XYPointMultiType;

import java.util.Collection;
import java.util.function.Consumer;

public class PointRegionQuadTreeFilterIndexTraverse {
    public static void traverse(PointRegionQuadTree<Object> quadtree, Consumer<Object> consumer) {
        traverse(quadtree.getRoot(), consumer);
    }

    public static void traverse(PointRegionQuadTreeNode<Object> node, Consumer<Object> consumer) {
        if (node instanceof PointRegionQuadTreeNodeLeaf) {
            PointRegionQuadTreeNodeLeaf<Object> leaf = (PointRegionQuadTreeNodeLeaf<Object>) node;
            traverseData(leaf.getPoints(), consumer);
            return;
        }

        PointRegionQuadTreeNodeBranch<Object> branch = (PointRegionQuadTreeNodeBranch<Object>) node;
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
        if (data instanceof XYPointWValue) {
            consumer.accept(((XYPointWValue) data).getValue());
        } else if (data instanceof XYPointMultiType) {
            XYPointMultiType multiType = (XYPointMultiType) data;
            if (multiType.getMultityped() instanceof Collection) {
                Collection collection = (Collection) multiType.getMultityped();
                for (Object datapoint : collection) {
                    visit(datapoint, consumer);
                }
            }
        }
    }
}
