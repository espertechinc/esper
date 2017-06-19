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
package com.espertech.esper.spatial.quadtree.mxcifrowindex;

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableUnique;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.core.QuadrantAppliesEnum;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNode;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;

import java.util.Collection;
import java.util.LinkedList;

public class MXCIFQuadTreeRowIndexAdd {

    /**
     * Add value.
     *
     * @param x         x
     * @param y         y
     * @param width     width
     * @param height    height
     * @param value     value to add
     * @param tree      quadtree
     * @param unique    true for unique
     * @param indexName index name
     * @return true for added, false for not-responsible for this point
     */
    public static boolean add(double x, double y, double width, double height, Object value, MXCIFQuadTree<Object> tree, boolean unique, String indexName) {
        MXCIFQuadTreeNode<Object> root = tree.getRoot();
        if (!root.getBb().intersectsBoxIncludingEnd(x, y, width, height)) {
            return false;
        }
        MXCIFQuadTreeNode<Object> replacement = addToNode(x, y, width, height, value, root, tree, unique, indexName);
        tree.setRoot(replacement);
        return true;
    }

    private static MXCIFQuadTreeNode<Object> addToNode(double x, double y, double width, double height, Object value, MXCIFQuadTreeNode<Object> node, MXCIFQuadTree<Object> tree, boolean unique, String indexName) {
        if (node instanceof MXCIFQuadTreeNodeLeaf) {
            MXCIFQuadTreeNodeLeaf<Object> leaf = (MXCIFQuadTreeNodeLeaf<Object>) node;

            if (leaf.getCount() < tree.getLeafCapacity() || node.getLevel() >= tree.getMaxTreeHeight()) {
                // can be multiple as value can be a collection
                int numAdded = addToData(leaf, x, y, width, height, value, unique, indexName);
                leaf.incCount(numAdded);

                if (leaf.getCount() <= tree.getLeafCapacity() || node.getLevel() >= tree.getMaxTreeHeight()) {
                    return leaf;
                }
            }

            node = subdivide(leaf, tree, unique, indexName);
        }

        MXCIFQuadTreeNodeBranch<Object> branch = (MXCIFQuadTreeNodeBranch) node;
        addToBranch(branch, x, y, width, height, value, tree, unique, indexName);
        return node;
    }

    private static void addToBranch(MXCIFQuadTreeNodeBranch<Object> branch, double x, double y, double width, double height, Object value, MXCIFQuadTree<Object> tree, boolean unique, String indexName) {
        QuadrantAppliesEnum quadrant = branch.getBb().getQuadrantApplies(x, y, width, height);
        if (quadrant == QuadrantAppliesEnum.NW) {
            branch.setNw(addToNode(x, y, width, height, value, branch.getNw(), tree, unique, indexName));
        } else if (quadrant == QuadrantAppliesEnum.NE) {
            branch.setNe(addToNode(x, y, width, height, value, branch.getNe(), tree, unique, indexName));
        } else if (quadrant == QuadrantAppliesEnum.SW) {
            branch.setSw(addToNode(x, y, width, height, value, branch.getSw(), tree, unique, indexName));
        } else if (quadrant == QuadrantAppliesEnum.SE) {
            branch.setSe(addToNode(x, y, width, height, value, branch.getSe(), tree, unique, indexName));
        } else if (quadrant == QuadrantAppliesEnum.SOME) {
            int numAdded = addToData(branch, x, y, width, height, value, unique, indexName);
            branch.incCount(numAdded);
        } else {
            throw new IllegalStateException("Applies to none");
        }
    }

    private static MXCIFQuadTreeNode<Object> subdivide(MXCIFQuadTreeNodeLeaf<Object> leaf, MXCIFQuadTree<Object> tree, boolean unique, String indexName) {
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
        MXCIFQuadTreeNodeBranch<Object> branch = new MXCIFQuadTreeNodeBranch<Object>(leaf.getBb(), leaf.getLevel(), null, 0, nw, ne, sw, se);

        Object data = leaf.getData();
        if (data instanceof XYWHRectangleMultiType) {
            XYWHRectangleMultiType rectangle = (XYWHRectangleMultiType) data;
            subdivide(rectangle, branch, tree, unique, indexName);
        } else {
            Collection<XYWHRectangleMultiType> collection = (Collection<XYWHRectangleMultiType>) data;
            for (XYWHRectangleMultiType rectangle : collection) {
                subdivide(rectangle, branch, tree, unique, indexName);
            }
        }
        return branch;
    }

    private static void subdivide(XYWHRectangleMultiType rectangle, MXCIFQuadTreeNodeBranch<Object> branch, MXCIFQuadTree<Object> tree, boolean unique, String indexName) {
        double x = rectangle.getX();
        double y = rectangle.getY();
        double w = rectangle.getW();
        double h = rectangle.getH();
        QuadrantAppliesEnum quadrant = branch.getBb().getQuadrantApplies(x, y, w, h);
        if (quadrant == QuadrantAppliesEnum.NW) {
            branch.setNw(addToNode(x, y, w, h, rectangle, branch.getNw(), tree, unique, indexName));
        } else if (quadrant == QuadrantAppliesEnum.NE) {
            branch.setNe(addToNode(x, y, w, h, rectangle, branch.getNe(), tree, unique, indexName));
        } else if (quadrant == QuadrantAppliesEnum.SW) {
            branch.setSw(addToNode(x, y, w, h, rectangle, branch.getSw(), tree, unique, indexName));
        } else if (quadrant == QuadrantAppliesEnum.SE) {
            branch.setSe(addToNode(x, y, w, h, rectangle, branch.getSe(), tree, unique, indexName));
        } else if (quadrant == QuadrantAppliesEnum.SOME) {
            int numAdded = addToData(branch, x, y, w, h, rectangle, unique, indexName);
            branch.incCount(numAdded);
        } else {
            throw new IllegalStateException("No intersection");
        }
    }

    public static int addToData(MXCIFQuadTreeNode<Object> node, double x, double y, double width, double height, Object value, boolean unique, String indexName) {
        Object currentValue = node.getData();

        // value can be multitype itself since we may subdivide-add and don't want to allocate a new object
        if (value instanceof XYWHRectangleMultiType) {
            XYWHRectangleMultiType rectangle = (XYWHRectangleMultiType) value;
            if (!rectangle.coordinateEquals(x, y, width, height)) {
                throw new IllegalStateException();
            }
            if (currentValue == null) {
                node.setData(rectangle);
                return rectangle.count();
            }
            if (currentValue instanceof XYWHRectangleMultiType) {
                XYWHRectangleMultiType other = (XYWHRectangleMultiType) currentValue;
                if (other.coordinateEquals(x, y, width, height)) {
                    if (unique) {
                        throw handleUniqueViolation(indexName, other);
                    }
                    other.addMultiType(rectangle);
                    return rectangle.count();
                }
                Collection<XYWHRectangleMultiType> collection = new LinkedList<>();
                collection.add(other);
                collection.add(rectangle);
                node.setData(collection);
                return rectangle.count();
            }
            Collection<XYWHRectangleMultiType> collection = (Collection<XYWHRectangleMultiType>) currentValue;
            for (XYWHRectangleMultiType other : collection) {
                if (other.coordinateEquals(x, y, width, height)) {
                    if (unique) {
                        throw handleUniqueViolation(indexName, other);
                    }
                    other.addMultiType(rectangle);
                    return rectangle.count();
                }
            }
            collection.add(rectangle);
            return rectangle.count();
        }

        if (currentValue == null) {
            XYWHRectangleMultiType point = new XYWHRectangleMultiType(x, y, width, height, value);
            node.setData(point);
            return 1;
        }
        if (currentValue instanceof XYWHRectangleMultiType) {
            XYWHRectangleMultiType other = (XYWHRectangleMultiType) currentValue;
            if (other.coordinateEquals(x, y, width, height)) {
                if (unique) {
                    throw handleUniqueViolation(indexName, other);
                }
                other.addSingleValue(value);
                return 1;
            }
            Collection<XYWHRectangleMultiType> collection = new LinkedList<>();
            collection.add(other);
            collection.add(new XYWHRectangleMultiType(x, y, width, height, value));
            node.setData(collection);
            return 1;
        }
        Collection<XYWHRectangleMultiType> collection = (Collection<XYWHRectangleMultiType>) currentValue;
        for (XYWHRectangleMultiType other : collection) {
            if (other.coordinateEquals(x, y, width, height)) {
                if (unique) {
                    throw handleUniqueViolation(indexName, other);
                }
                other.addSingleValue(value);
                return 1;
            }
        }
        collection.add(new XYWHRectangleMultiType(x, y, width, height, value));
        return 1;
    }

    private static EPException handleUniqueViolation(String indexName, XYWHRectangleMultiType other) {
        return PropertyIndexedEventTableUnique.handleUniqueIndexViolation(indexName, "(" + other.getX() + "," + other.getY() + "," + other.getW() + "," + other.getH() + ")");
    }
}
