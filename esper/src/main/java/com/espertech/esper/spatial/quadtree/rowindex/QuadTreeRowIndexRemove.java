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
package com.espertech.esper.spatial.quadtree.rowindex;

import com.espertech.esper.spatial.quadtree.core.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class QuadTreeRowIndexRemove {

    /**
     * Remove value.
     *
     * @param x     x
     * @param y     y
     * @param value value to remove
     * @param tree  quadtree
     */
    public static void remove(double x, double y, Object value, QuadTree<Object> tree) {
        QuadTreeNode<Object> root = tree.getRoot();
        QuadTreeNode<Object> replacement = removeFromNode(x, y, value, root, tree);
        tree.setRoot(replacement);
    }

    private static QuadTreeNode<Object> removeFromNode(double x, double y, Object value, QuadTreeNode<Object> node, QuadTree<Object> tree) {

        if (node instanceof QuadTreeNodeLeaf) {
            QuadTreeNodeLeaf<Object> leaf = (QuadTreeNodeLeaf<Object>) node;
            boolean removed = removeFromPoints(x, y, value, leaf.getPoints());
            if (removed) {
                leaf.decCount();
                if (leaf.getCount() == 0) {
                    leaf.setPoints(null);
                }
            }
            return leaf;
        }

        QuadTreeNodeBranch<Object> branch = (QuadTreeNodeBranch<Object>) node;
        QuadrantEnum quadrant = node.getBb().getQuadrant(x, y);
        if (quadrant == QuadrantEnum.NW) {
            branch.setNw(removeFromNode(x, y, value, branch.getNw(), tree));
        } else if (quadrant == QuadrantEnum.NE) {
            branch.setNe(removeFromNode(x, y, value, branch.getNe(), tree));
        } else if (quadrant == QuadrantEnum.SW) {
            branch.setSw(removeFromNode(x, y, value, branch.getSw(), tree));
        } else {
            branch.setSe(removeFromNode(x, y, value, branch.getSe(), tree));
        }

        if (!(branch.getNw() instanceof QuadTreeNodeLeaf) || !(branch.getNe() instanceof QuadTreeNodeLeaf) || !(branch.getSw() instanceof QuadTreeNodeLeaf) || !(branch.getSe() instanceof QuadTreeNodeLeaf)) {
            return branch;
        }
        QuadTreeNodeLeaf<Object> nwLeaf = (QuadTreeNodeLeaf<Object>) branch.getNw();
        QuadTreeNodeLeaf<Object> neLeaf = (QuadTreeNodeLeaf<Object>) branch.getNe();
        QuadTreeNodeLeaf<Object> swLeaf = (QuadTreeNodeLeaf<Object>) branch.getSw();
        QuadTreeNodeLeaf<Object> seLeaf = (QuadTreeNodeLeaf<Object>) branch.getSe();
        int total = nwLeaf.getCount() + neLeaf.getCount() + swLeaf.getCount() + seLeaf.getCount();
        if (total >= tree.getLeafCapacity()) {
            return branch;
        }

        Collection<XYPointMultiType> collection = new LinkedList<>();
        int count = mergeChildNodes(collection, nwLeaf.getPoints());
        count += mergeChildNodes(collection, neLeaf.getPoints());
        count += mergeChildNodes(collection, swLeaf.getPoints());
        count += mergeChildNodes(collection, seLeaf.getPoints());
        return new QuadTreeNodeLeaf<>(branch.getBb(), branch.getLevel(), collection, count);
    }

    private static boolean removeFromPoints(double x, double y, Object value, Object points) {
        if (points == null) {
            return false;
        }
        if (!(points instanceof Collection)) {
            XYPointMultiType point = (XYPointMultiType) points;
            if (point.getX() == x && point.getY() == y) {
                boolean removed = point.remove(value);
                if (removed) {
                    return true;
                }
            }
            return false;
        }
        Collection<XYPointMultiType> collection = (Collection<XYPointMultiType>) points;
        Iterator<XYPointMultiType> it = collection.iterator();
        for (; it.hasNext(); ) {
            XYPointMultiType point = it.next();
            if (point.getX() == x && point.getY() == y) {
                boolean removed = point.remove(value);
                if (removed) {
                    if (point.isEmpty()) {
                        it.remove();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static int mergeChildNodes(Collection<XYPointMultiType> target, Object points) {
        if (points == null) {
            return 0;
        }
        if (points instanceof XYPointMultiType) {
            XYPointMultiType p = (XYPointMultiType) points;
            target.add(p);
            return p.count();
        }
        Collection<XYPointMultiType> coll = (Collection<XYPointMultiType>) points;
        int total = 0;
        for (XYPointMultiType p : coll) {
            target.add(p);
            total += p.count();
        }
        return total;
    }
}
