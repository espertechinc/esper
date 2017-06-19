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
import java.util.Iterator;
import java.util.LinkedList;

import static com.espertech.esper.spatial.quadtree.prqdfilterindex.PointRegionQuadTreeFilterIndexCheckBB.checkBB;

public class PointRegionQuadTreeFilterIndexDelete {
    public static void delete(double x, double y, PointRegionQuadTree<Object> tree) {
        PointRegionQuadTreeNode<Object> root = tree.getRoot();
        checkBB(root.getBb(), x, y);
        PointRegionQuadTreeNode<Object> replacement = deleteFromNode(x, y, root, tree);
        tree.setRoot(replacement);
    }

    private static <L> PointRegionQuadTreeNode<Object> deleteFromNode(double x, double y, PointRegionQuadTreeNode<Object> node, PointRegionQuadTree<Object> tree) {

        if (node instanceof PointRegionQuadTreeNodeLeaf) {
            PointRegionQuadTreeNodeLeaf<Object> leaf = (PointRegionQuadTreeNodeLeaf<Object>) node;
            boolean removed = deleteFromPoints(x, y, leaf.getPoints());
            if (removed) {
                leaf.decCount();
                if (leaf.getCount() == 0) {
                    leaf.setPoints(null);
                }
            }
            return leaf;
        }

        PointRegionQuadTreeNodeBranch<Object> branch = (PointRegionQuadTreeNodeBranch<Object>) node;
        QuadrantEnum quadrant = node.getBb().getQuadrant(x, y);
        if (quadrant == QuadrantEnum.NW) {
            branch.setNw(deleteFromNode(x, y, branch.getNw(), tree));
        } else if (quadrant == QuadrantEnum.NE) {
            branch.setNe(deleteFromNode(x, y, branch.getNe(), tree));
        } else if (quadrant == QuadrantEnum.SW) {
            branch.setSw(deleteFromNode(x, y, branch.getSw(), tree));
        } else {
            branch.setSe(deleteFromNode(x, y, branch.getSe(), tree));
        }

        if (!(branch.getNw() instanceof PointRegionQuadTreeNodeLeaf) || !(branch.getNe() instanceof PointRegionQuadTreeNodeLeaf) || !(branch.getSw() instanceof PointRegionQuadTreeNodeLeaf) || !(branch.getSe() instanceof PointRegionQuadTreeNodeLeaf)) {
            return branch;
        }
        PointRegionQuadTreeNodeLeaf<Object> nwLeaf = (PointRegionQuadTreeNodeLeaf<Object>) branch.getNw();
        PointRegionQuadTreeNodeLeaf<Object> neLeaf = (PointRegionQuadTreeNodeLeaf<Object>) branch.getNe();
        PointRegionQuadTreeNodeLeaf<Object> swLeaf = (PointRegionQuadTreeNodeLeaf<Object>) branch.getSw();
        PointRegionQuadTreeNodeLeaf<Object> seLeaf = (PointRegionQuadTreeNodeLeaf<Object>) branch.getSe();
        int total = nwLeaf.getCount() + neLeaf.getCount() + swLeaf.getCount() + seLeaf.getCount();
        if (total >= tree.getLeafCapacity()) {
            return branch;
        }

        Collection<XYPointWValue<L>> collection = new LinkedList<>();
        int count = mergeChildNodes(collection, nwLeaf.getPoints());
        count += mergeChildNodes(collection, neLeaf.getPoints());
        count += mergeChildNodes(collection, swLeaf.getPoints());
        count += mergeChildNodes(collection, seLeaf.getPoints());
        return new PointRegionQuadTreeNodeLeaf<Object>(branch.getBb(), branch.getLevel(), collection, count);
    }

    private static <L> boolean deleteFromPoints(double x, double y, Object points) {
        if (points == null) {
            return false;
        }
        if (!(points instanceof Collection)) {
            XYPointWValue<L> point = (XYPointWValue<L>) points;
            return point.getX() == x && point.getY() == y;
        }
        Collection<XYPointWValue<L>> collection = (Collection<XYPointWValue<L>>) points;
        Iterator<XYPointWValue<L>> it = collection.iterator();
        for (; it.hasNext(); ) {
            XYPointWValue<L> point = it.next();
            if (point.getX() == x && point.getY() == y) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    private static <L> int mergeChildNodes(Collection<XYPointWValue<L>> target, Object points) {
        if (points == null) {
            return 0;
        }
        if (points instanceof XYPointWValue) {
            XYPointWValue<L> p = (XYPointWValue<L>) points;
            target.add(p);
            return 1;
        }
        Collection<XYPointWValue<L>> coll = (Collection<XYPointWValue<L>>) points;
        for (XYPointWValue<L> p : coll) {
            target.add(p);
        }
        return coll.size();
    }
}
