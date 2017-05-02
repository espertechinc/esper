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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.spatial.quadtree.core.*;

import java.util.Collection;

public class QuadTreeFilterIndexCollect {
    public static <L, T> void collectRange(QuadTree<Object> quadTree, double x, double y, double width, double height, EventBean eventBean, T target, QuadTreeCollector<L, T> collector) {
        collectRange(quadTree.getRoot(), x, y, width, height, eventBean, target, collector);
    }

    private static <L, T> void collectRange(QuadTreeNode<Object> node, double x, double y, double width, double height, EventBean eventBean, T target, QuadTreeCollector<L, T> collector) {
        if (!node.getBb().intersectsBox(x, y, width, height)) {
            return;
        }
        if (node instanceof QuadTreeNodeLeaf) {
            QuadTreeNodeLeaf<Object> leaf = (QuadTreeNodeLeaf<Object>) node;
            collectLeaf(leaf, x, y, width, height, eventBean, target, collector);
            return;
        }

        QuadTreeNodeBranch<Object> branch = (QuadTreeNodeBranch<Object>) node;
        collectRange(branch.getNw(), x, y, width, height, eventBean, target, collector);
        collectRange(branch.getNe(), x, y, width, height, eventBean, target, collector);
        collectRange(branch.getSw(), x, y, width, height, eventBean, target, collector);
        collectRange(branch.getSe(), x, y, width, height, eventBean, target, collector);
    }

    private static <L, T> void collectLeaf(QuadTreeNodeLeaf node, double x, double y, double width, double height, EventBean eventBean, T target, QuadTreeCollector<L, T> collector) {
        Object points = node.getPoints();
        if (points == null) {
            return;
        }
        if (points instanceof XYPointWValue) {
            XYPointWValue<L> point = (XYPointWValue<L>) points;
            if (BoundingBox.containsPoint(x, y, width, height, point.getX(), point.getY())) {
                collector.collectInto(eventBean, point.getValue(), target);
            }
            return;
        }
        Collection<XYPointWValue<L>> collection = (Collection<XYPointWValue<L>>) points;
        for (XYPointWValue<L> point : collection) {
            if (BoundingBox.containsPoint(x, y, width, height, point.getX(), point.getY())) {
                collector.collectInto(eventBean, point.getValue(), target);
            }
        }
    }
}
