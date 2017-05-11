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

import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.core.QuadrantAppliesEnum;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNode;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;

import java.util.Collection;
import java.util.LinkedList;

import static com.espertech.esper.spatial.quadtree.mxcifrowindex.MXCIFQuadTreeFilterIndexCheckBB.checkBB;

public class MXCIFQuadTreeFilterIndexSet {
    public static <L> void set(double x, double y, double width, double height, L value, MXCIFQuadTree<Object> tree) {
        MXCIFQuadTreeNode<Object> root = tree.getRoot();
        checkBB(root.getBb(), x, y, width, height);
        MXCIFQuadTreeNode<Object> replacement = setOnNode(x, y, width, height, value, root, tree);
        tree.setRoot(replacement);
    }

    private static <L> MXCIFQuadTreeNode<Object> setOnNode(double x, double y, double width, double height, L value, MXCIFQuadTreeNode<Object> node, MXCIFQuadTree<Object> tree) {
        if (node instanceof MXCIFQuadTreeNodeLeaf) {
            MXCIFQuadTreeNodeLeaf<Object> leaf = (MXCIFQuadTreeNodeLeaf<Object>) node;
            int count = setOnNode(leaf, x, y, width, height, value);
            leaf.incCount(count);

            if (leaf.getCount() <= tree.getLeafCapacity() || node.getLevel() >= tree.getMaxTreeHeight()) {
                return leaf;
            }
            node = subdivide(leaf, tree);
        }

        MXCIFQuadTreeNodeBranch<Object> branch = (MXCIFQuadTreeNodeBranch) node;
        addToBranch(branch, x, y, width, height, value, tree);
        return node;
    }

    private static void addToBranch(MXCIFQuadTreeNodeBranch<Object> branch, double x, double y, double width, double height, Object value, MXCIFQuadTree<Object> tree) {
        QuadrantAppliesEnum quadrant = branch.getBb().getQuadrantApplies(x, y, width, height);
        if (quadrant == QuadrantAppliesEnum.NW) {
            branch.setNw(setOnNode(x, y, width, height, value, branch.getNw(), tree));
        } else if (quadrant == QuadrantAppliesEnum.NE) {
            branch.setNe(setOnNode(x, y, width, height, value, branch.getNe(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SW) {
            branch.setSw(setOnNode(x, y, width, height, value, branch.getSw(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SE) {
            branch.setSe(setOnNode(x, y, width, height, value, branch.getSe(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SOME) {
            int count = setOnNode(branch, x, y, width, height, value);
            branch.incCount(count);
        } else {
            throw new IllegalStateException("Quandrant not applies to any");
        }
    }

    private static <L> MXCIFQuadTreeNode<Object> subdivide(MXCIFQuadTreeNodeLeaf<Object> leaf, MXCIFQuadTree<Object> tree) {
        double w = (leaf.getBb().getMaxX() - leaf.getBb().getMinX()) / 2d;
        double h = (leaf.getBb().getMaxY() - leaf.getBb().getMinY()) / 2d;
        double minx = leaf.getBb().getMinX();
        double miny = leaf.getBb().getMinY();

        BoundingBox bbNW = new BoundingBox(minx, miny, minx + w, miny + h);
        BoundingBox bbNE = new BoundingBox(minx + w, miny, leaf.getBb().getMaxX(), miny + h);
        BoundingBox bbSW = new BoundingBox(minx, miny + h, minx + w, leaf.getBb().getMaxY());
        BoundingBox bbSE = new BoundingBox(minx + w, miny + h, leaf.getBb().getMaxX(), leaf.getBb().getMaxY());
        MXCIFQuadTreeNode<Object> nw = new MXCIFQuadTreeNodeLeaf<>(bbNW, leaf.getLevel() + 1, null, 0);
        MXCIFQuadTreeNode<Object> ne = new MXCIFQuadTreeNodeLeaf<>(bbNE, leaf.getLevel() + 1, null, 0);
        MXCIFQuadTreeNode<Object> sw = new MXCIFQuadTreeNodeLeaf<>(bbSW, leaf.getLevel() + 1, null, 0);
        MXCIFQuadTreeNode<Object> se = new MXCIFQuadTreeNodeLeaf<>(bbSE, leaf.getLevel() + 1, null, 0);
        MXCIFQuadTreeNodeBranch<Object> branch = new MXCIFQuadTreeNodeBranch<>(leaf.getBb(), leaf.getLevel(), null, 0, nw, ne, sw, se);

        Object rectangles = leaf.getData();
        if (rectangles instanceof XYWHRectangleWValue) {
            XYWHRectangleWValue rectangle = (XYWHRectangleWValue<L>) rectangles;
            subdivide(rectangle, branch, tree);
        } else {
            Collection<XYWHRectangleWValue<L>> collection = (Collection<XYWHRectangleWValue<L>>) rectangles;
            for (XYWHRectangleWValue<L> rectangle : collection) {
                subdivide(rectangle, branch, tree);
            }
        }
        return branch;
    }

    private static <L> void subdivide(XYWHRectangleWValue<L> rectangle, MXCIFQuadTreeNodeBranch<Object> branch, MXCIFQuadTree<Object> tree) {
        double x = rectangle.getX();
        double y = rectangle.getY();
        double w = rectangle.getW();
        double h = rectangle.getH();
        QuadrantAppliesEnum quadrant = branch.getBb().getQuadrantApplies(x, y, w, h);
        if (quadrant == QuadrantAppliesEnum.NW) {
            branch.setNw(setOnNode(x, y, w, h, rectangle, branch.getNw(), tree));
        } else if (quadrant == QuadrantAppliesEnum.NE) {
            branch.setNe(setOnNode(x, y, w, h, rectangle, branch.getNe(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SW) {
            branch.setSw(setOnNode(x, y, w, h, rectangle, branch.getSw(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SE) {
            branch.setSe(setOnNode(x, y, w, h, rectangle, branch.getSe(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SOME) {
            int numAdded = setOnNode(branch, x, y, w, h, rectangle);
            branch.incCount(numAdded);
        } else {
            throw new IllegalStateException("No intersection");
        }
    }

    private static <L> int setOnNode(MXCIFQuadTreeNode<Object> node, double x, double y, double width, double height, L value) {
        Object currentValue = node.getData();

        if (value instanceof XYWHRectangleWValue) {
            XYWHRectangleWValue<L> rectangle = (XYWHRectangleWValue<L>) value;
            if (!rectangle.coordinateEquals(x, y, width, height)) {
                throw new IllegalStateException();
            }
            if (currentValue == null) {
                node.setData(rectangle);
                return 1;
            }
            if (currentValue instanceof XYWHRectangleWValue) {
                XYWHRectangleWValue<L> other = (XYWHRectangleWValue<L>) currentValue;
                if (other.coordinateEquals(x, y, width, height)) {
                    other.setValue(value);
                    return 0; // replaced
                }
                Collection<XYWHRectangleWValue<L>> collection = new LinkedList<>();
                collection.add(other);
                collection.add(rectangle);
                node.setData(collection);
                return 1;
            }
            Collection<XYWHRectangleWValue<L>> collection = (Collection<XYWHRectangleWValue<L>>) currentValue;
            for (XYWHRectangleWValue<L> other : collection) {
                if (other.coordinateEquals(x, y, width, height)) {
                    other.setValue(value);
                    return 0;
                }
            }
            collection.add(rectangle);
            return 1;
        }

        if (currentValue == null) {
            XYWHRectangleWValue<L> point = new XYWHRectangleWValue<>(x, y, width, height, value);
            node.setData(point);
            return 1;
        }
        if (currentValue instanceof XYWHRectangleWValue) {
            XYWHRectangleWValue<L> other = (XYWHRectangleWValue<L>) currentValue;
            if (other.coordinateEquals(x, y, width, height)) {
                other.setValue(value);
                return 0;
            }
            Collection<XYWHRectangleWValue<L>> collection = new LinkedList<>();
            collection.add(other);
            collection.add(new XYWHRectangleWValue<>(x, y, width, height, value));
            node.setData(collection);
            return 1;
        }
        Collection<XYWHRectangleWValue<L>> collection = (Collection<XYWHRectangleWValue<L>>) currentValue;
        for (XYWHRectangleWValue<L> other : collection) {
            if (other.coordinateEquals(x, y, width, height)) {
                other.setValue(value);
                return 0;
            }
        }
        collection.add(new XYWHRectangleWValue<>(x, y, width, height, value));
        return 1;
    }
}
