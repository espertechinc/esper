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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.common.internal.epl.spatial.quadtree.core.QuadTreeCollector;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTreeNode;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTreeNodeBranch;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTreeNodeLeaf;

import java.util.Collection;

public class PointRegionQuadTreeFilterIndexCollect {
    public static <L, T> void collectRange(PointRegionQuadTree<Object> quadTree, double x, double y, double width, double height, EventBean eventBean, T target, QuadTreeCollector<L, T> collector, ExprEvaluatorContext ctx) {
        collectRange(quadTree.getRoot(), x, y, width, height, eventBean, target, collector, ctx);
    }

    private static <L, T> void collectRange(PointRegionQuadTreeNode<Object> node, double x, double y, double width, double height, EventBean eventBean, T target, QuadTreeCollector<L, T> collector, ExprEvaluatorContext ctx) {
        if (!node.getBb().intersectsBoxIncludingEnd(x, y, width, height)) {
            return;
        }
        if (node instanceof PointRegionQuadTreeNodeLeaf) {
            PointRegionQuadTreeNodeLeaf<Object> leaf = (PointRegionQuadTreeNodeLeaf<Object>) node;
            collectLeaf(leaf, x, y, width, height, eventBean, target, collector, ctx);
            return;
        }

        PointRegionQuadTreeNodeBranch<Object> branch = (PointRegionQuadTreeNodeBranch<Object>) node;
        collectRange(branch.getNw(), x, y, width, height, eventBean, target, collector, ctx);
        collectRange(branch.getNe(), x, y, width, height, eventBean, target, collector, ctx);
        collectRange(branch.getSw(), x, y, width, height, eventBean, target, collector, ctx);
        collectRange(branch.getSe(), x, y, width, height, eventBean, target, collector, ctx);
    }

    private static <L, T> void collectLeaf(PointRegionQuadTreeNodeLeaf node, double x, double y, double width, double height, EventBean eventBean, T target, QuadTreeCollector<L, T> collector, ExprEvaluatorContext ctx) {
        Object points = node.getPoints();
        if (points == null) {
            return;
        }
        if (points instanceof XYPointWValue) {
            XYPointWValue<L> point = (XYPointWValue<L>) points;
            if (BoundingBox.containsPoint(x, y, width, height, point.getX(), point.getY())) {
                collector.collectInto(eventBean, point.getValue(), target, ctx);
            }
            return;
        }
        Collection<XYPointWValue<L>> collection = (Collection<XYPointWValue<L>>) points;
        for (XYPointWValue<L> point : collection) {
            if (BoundingBox.containsPoint(x, y, width, height, point.getX(), point.getY())) {
                collector.collectInto(eventBean, point.getValue(), target, ctx);
            }
        }
    }
}
