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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.core.QuadTreeCollector;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNode;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;

import java.util.Collection;

public class MXCIFQuadTreeFilterIndexCollect {
    public static <L, T> void collectRange(MXCIFQuadTree<Object> quadTree, double x, double y, double width, double height, EventBean eventBean, T target, QuadTreeCollector<L, T> collector) {
        collectRange(quadTree.getRoot(), x, y, width, height, eventBean, target, collector);
    }

    private static <L, T> void collectRange(MXCIFQuadTreeNode<Object> node, double x, double y, double width, double height, EventBean eventBean, T target, QuadTreeCollector<L, T> collector) {
        if (node instanceof MXCIFQuadTreeNodeLeaf) {
            MXCIFQuadTreeNodeLeaf<Object> leaf = (MXCIFQuadTreeNodeLeaf<Object>) node;
            collectNode(leaf, x, y, width, height, eventBean, target, collector);
            return;
        }

        MXCIFQuadTreeNodeBranch<Object> branch = (MXCIFQuadTreeNodeBranch<Object>) node;
        collectNode(branch, x, y, width, height, eventBean, target, collector);
        collectRange(branch.getNw(), x, y, width, height, eventBean, target, collector);
        collectRange(branch.getNe(), x, y, width, height, eventBean, target, collector);
        collectRange(branch.getSw(), x, y, width, height, eventBean, target, collector);
        collectRange(branch.getSe(), x, y, width, height, eventBean, target, collector);
    }

    private static <L, T> void collectNode(MXCIFQuadTreeNode node, double x, double y, double width, double height, EventBean eventBean, T target, QuadTreeCollector<L, T> collector) {
        Object rectangles = node.getData();
        if (rectangles == null) {
            return;
        }
        if (rectangles instanceof XYWHRectangleWValue) {
            XYWHRectangleWValue<L> rectangle = (XYWHRectangleWValue<L>) rectangles;
            if (BoundingBox.intersectsBoxIncludingEnd(x, y, x + width, y + height, rectangle.getX(), rectangle.getY(), rectangle.getW(), rectangle.getH())) {
                collector.collectInto(eventBean, rectangle.getValue(), target);
            }
            return;
        }
        Collection<XYWHRectangleWValue<L>> collection = (Collection<XYWHRectangleWValue<L>>) rectangles;
        for (XYWHRectangleWValue<L> rectangle : collection) {
            if (BoundingBox.intersectsBoxIncludingEnd(x, y, x + width, y + height, rectangle.getX(), rectangle.getY(), rectangle.getW(), rectangle.getH())) {
                collector.collectInto(eventBean, rectangle.getValue(), target);
            }
        }
    }
}
