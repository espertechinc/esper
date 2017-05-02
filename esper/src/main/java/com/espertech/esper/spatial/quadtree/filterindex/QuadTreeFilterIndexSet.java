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

import com.espertech.esper.spatial.quadtree.core.*;

import java.util.Collection;
import java.util.LinkedList;

import static com.espertech.esper.spatial.quadtree.filterindex.QuadTreeFilterIndexCheckBB.checkBB;

public class QuadTreeFilterIndexSet {
    public static <L> void set(double x, double y, L value, QuadTree<Object> tree) {
        QuadTreeNode<Object> root = tree.getRoot();
        checkBB(root.getBb(), x, y);
        QuadTreeNode<Object> replacement = setOnNode(x, y, value, root, tree);
        tree.setRoot(replacement);
    }

    private static <L> QuadTreeNode<Object> setOnNode(double x, double y, L value, QuadTreeNode<Object> node, QuadTree<Object> tree) {
        if (node instanceof QuadTreeNodeLeaf) {
            QuadTreeNodeLeaf<Object> leaf = (QuadTreeNodeLeaf<Object>) node;
            int count = setOnLeaf(leaf, x, y, value);
            leaf.incCount(count);

            if (leaf.getCount() <= tree.getLeafCapacity() || node.getLevel() >= tree.getMaxTreeHeight()) {
                return leaf;
            }
            node = subdivide(leaf, tree);
        }

        QuadTreeNodeBranch<Object> branch = (QuadTreeNodeBranch) node;
        addToBranch(branch, x, y, value, tree);
        return node;
    }

    private static void addToBranch(QuadTreeNodeBranch<Object> branch, double x, double y, Object value, QuadTree<Object> tree) {
        QuadrantEnum quadrant = branch.getBb().getQuadrant(x, y);
        if (quadrant == QuadrantEnum.NW) {
            branch.setNw(setOnNode(x, y, value, branch.getNw(), tree));
        } else if (quadrant == QuadrantEnum.NE) {
            branch.setNe(setOnNode(x, y, value, branch.getNe(), tree));
        } else if (quadrant == QuadrantEnum.SW) {
            branch.setSw(setOnNode(x, y, value, branch.getSw(), tree));
        } else {
            branch.setSe(setOnNode(x, y, value, branch.getSe(), tree));
        }
    }

    private static <L> QuadTreeNode<Object> subdivide(QuadTreeNodeLeaf<Object> leaf, QuadTree<Object> tree) {
        double w = (leaf.getBb().getMaxX() - leaf.getBb().getMinX()) / 2d;
        double h = (leaf.getBb().getMaxY() - leaf.getBb().getMinY()) / 2d;
        double minx = leaf.getBb().getMinX();
        double miny = leaf.getBb().getMinY();

        BoundingBox bbNW = new BoundingBox(minx, miny, minx + w, miny + h);
        BoundingBox bbNE = new BoundingBox(minx + w, miny, leaf.getBb().getMaxX(), miny + h);
        BoundingBox bbSW = new BoundingBox(minx, miny + h, minx + w, leaf.getBb().getMaxY());
        BoundingBox bbSE = new BoundingBox(minx + w, miny + h, leaf.getBb().getMaxX(), leaf.getBb().getMaxY());
        QuadTreeNode<Object> nw = new QuadTreeNodeLeaf<>(bbNW, leaf.getLevel() + 1, null, 0);
        QuadTreeNode<Object> ne = new QuadTreeNodeLeaf<>(bbNE, leaf.getLevel() + 1, null, 0);
        QuadTreeNode<Object> sw = new QuadTreeNodeLeaf<>(bbSW, leaf.getLevel() + 1, null, 0);
        QuadTreeNode<Object> se = new QuadTreeNodeLeaf<>(bbSE, leaf.getLevel() + 1, null, 0);
        QuadTreeNodeBranch<Object> branch = new QuadTreeNodeBranch<>(leaf.getBb(), leaf.getLevel(), nw, ne, sw, se);

        Object points = leaf.getPoints();
        if (points instanceof XYPointWValue) {
            XYPointWValue point = (XYPointWValue<L>) points;
            subdividePoint(point, branch, tree);
        } else {
            Collection<XYPointWValue<L>> collection = (Collection<XYPointWValue<L>>) points;
            for (XYPointWValue<L> point : collection) {
                subdividePoint(point, branch, tree);
            }
        }
        return branch;
    }

    private static <L> void subdividePoint(XYPointWValue<L> point, QuadTreeNodeBranch<Object> branch, QuadTree<Object> tree) {
        double x = point.getX();
        double y = point.getY();
        QuadrantEnum quadrant = branch.getBb().getQuadrant(x, y);
        if (quadrant == QuadrantEnum.NW) {
            branch.setNw(setOnNode(x, y, point, branch.getNw(), tree));
        } else if (quadrant == QuadrantEnum.NE) {
            branch.setNe(setOnNode(x, y, point, branch.getNe(), tree));
        } else if (quadrant == QuadrantEnum.SW) {
            branch.setSw(setOnNode(x, y, point, branch.getSw(), tree));
        } else {
            branch.setSe(setOnNode(x, y, point, branch.getSe(), tree));
        }
    }

    private static <L> int setOnLeaf(QuadTreeNodeLeaf<Object> leaf, double x, double y, L value) {
        Object currentValue = leaf.getPoints();

        if (value instanceof XYPointWValue) {
            XYPointWValue<L> point = (XYPointWValue<L>) value;
            if (point.getX() != x && point.getY() != y) {
                throw new IllegalStateException();
            }
            if (currentValue == null) {
                leaf.setPoints(point);
                return 1;
            }
            if (currentValue instanceof XYPointWValue) {
                XYPointWValue<L> other = (XYPointWValue<L>) currentValue;
                if (other.getX() == x && other.getY() == y) {
                    other.setValue(value);
                    return 0; // replaced
                }
                Collection<XYPointWValue<L>> collection = new LinkedList<>();
                collection.add(other);
                collection.add(point);
                leaf.setPoints(collection);
                return 1;
            }
            Collection<XYPointWValue<L>> collection = (Collection<XYPointWValue<L>>) currentValue;
            for (XYPointWValue<L> other : collection) {
                if (other.getX() == x && other.getY() == y) {
                    other.setValue(value);
                    return 0;
                }
            }
            collection.add(point);
            return 1;
        }

        if (currentValue == null) {
            XYPointWValue<L> point = new XYPointWValue<>(x, y, value);
            leaf.setPoints(point);
            return 1;
        }
        if (currentValue instanceof XYPointWValue) {
            XYPointWValue<L> other = (XYPointWValue<L>) currentValue;
            if (other.getX() == x && other.getY() == y) {
                other.setValue(value);
                return 0;
            }
            Collection<XYPointWValue<L>> collection = new LinkedList<>();
            collection.add(other);
            collection.add(new XYPointWValue<>(x, y, value));
            leaf.setPoints(collection);
            return 1;
        }
        Collection<XYPointWValue<L>> collection = (Collection<XYPointWValue<L>>) currentValue;
        for (XYPointWValue<L> other : collection) {
            if (other.getX() == x && other.getY() == y) {
                other.setValue(value);
                return 0;
            }
        }
        collection.add(new XYPointWValue<>(x, y, value));
        return 1;
    }
}
