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
package com.espertech.esper.spatial.quadtree.user;

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableUnique;
import com.espertech.esper.spatial.quadtree.core.*;

import java.util.Collection;
import java.util.LinkedList;

public class QuadTreeToolAdd {

    /**
     * Add value.
     *
     * @param x     x
     * @param y     y
     * @param value value to add
     * @param tree  quadtree
     * @param unique true for unique
     * @param indexName index name
     * @return true for added, false for not-responsible for this point
     */
    public static boolean add(double x, double y, Object value, QuadTree<Object> tree, boolean unique, String indexName) {
        QuadTreeNode<Object> root = tree.getRoot();
        if (!root.getBb().containsPoint(x, y)) {
            return false;
        }
        QuadTreeNode<Object> replacement = addToNode(x, y, value, root, tree, unique, indexName);
        tree.setRoot(replacement);
        return true;
    }

    private static QuadTreeNode<Object> addToNode(double x, double y, Object value, QuadTreeNode<Object> node, QuadTree<Object> tree, boolean unique, String indexName) {
        if (node instanceof QuadTreeNodeLeaf) {
            QuadTreeNodeLeaf<Object> leaf = (QuadTreeNodeLeaf<Object>) node;
            if (leaf.getCount() < tree.getLeafCapacity() || node.getLevel() >= tree.getMaxTreeHeight()) {
                // can be multiple as value can be a collection
                int numAdded = addToLeaf(leaf, x, y, value, unique, indexName);
                leaf.incCount(numAdded);

                if (leaf.getCount() <= tree.getLeafCapacity() || node.getLevel() >= tree.getMaxTreeHeight()) {
                    return leaf;
                }
            }

            node = subdivide(leaf, tree, unique, indexName);
        }

        QuadTreeNodeBranch<Object> branch = (QuadTreeNodeBranch) node;
        addToBranch(branch, x, y, value, tree, unique, indexName);
        return node;
    }

    private static void addToBranch(QuadTreeNodeBranch<Object> branch, double x, double y, Object value, QuadTree<Object> tree, boolean unique, String indexName) {
        QuadrantEnum quadrant = branch.getBb().getQuadrant(x, y);
        if (quadrant == QuadrantEnum.NW) {
            branch.setNw(addToNode(x, y, value, branch.getNw(), tree, unique, indexName));
        } else if (quadrant == QuadrantEnum.NE) {
            branch.setNe(addToNode(x, y, value, branch.getNe(), tree, unique, indexName));
        } else if (quadrant == QuadrantEnum.SW) {
            branch.setSw(addToNode(x, y, value, branch.getSw(), tree, unique, indexName));
        } else {
            branch.setSe(addToNode(x, y, value, branch.getSe(), tree, unique, indexName));
        }
    }

    private static QuadTreeNode<Object> subdivide(QuadTreeNodeLeaf<Object> leaf, QuadTree<Object> tree, boolean unique, String indexName) {
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
        if (points instanceof XYPointMultiType) {
            XYPointMultiType point = (XYPointMultiType) points;
            subdividePoint(point, branch, tree, unique, indexName);
        } else {
            Collection<XYPointMultiType> collection = (Collection<XYPointMultiType>) points;
            for (XYPointMultiType point : collection) {
                subdividePoint(point, branch, tree, unique, indexName);
            }
        }
        return branch;
    }

    private static void subdividePoint(XYPointMultiType point, QuadTreeNodeBranch<Object> branch, QuadTree<Object> tree, boolean unique, String indexName) {
        double x = point.getX();
        double y = point.getY();
        QuadrantEnum quadrant = branch.getBb().getQuadrant(x, y);
        if (quadrant == QuadrantEnum.NW) {
            branch.setNw(addToNode(x, y, point, branch.getNw(), tree, unique, indexName));
        } else if (quadrant == QuadrantEnum.NE) {
            branch.setNe(addToNode(x, y, point, branch.getNe(), tree, unique, indexName));
        } else if (quadrant == QuadrantEnum.SW) {
            branch.setSw(addToNode(x, y, point, branch.getSw(), tree, unique, indexName));
        } else {
            branch.setSe(addToNode(x, y, point, branch.getSe(), tree, unique, indexName));
        }
    }

    public static int addToLeaf(QuadTreeNodeLeaf<Object> leaf, double x, double y, Object value, boolean unique, String indexName) {
        Object currentValue = leaf.getPoints();

        // value can be multitype itself since we may subdivide-add and don't want to allocate a new object
        if (value instanceof XYPointMultiType) {
            XYPointMultiType point = (XYPointMultiType) value;
            if (point.getX() != x && point.getY() != y) {
                throw new IllegalStateException();
            }
            if (currentValue == null) {
                leaf.setPoints(point);
                return point.count();
            }
            if (currentValue instanceof XYPointMultiType) {
                XYPointMultiType other = (XYPointMultiType) currentValue;
                if (other.getX() == x && other.getY() == y) {
                    if (unique) {
                        throw handleUniqueViolation(indexName, other.getX(), other.getY());
                    }
                    other.addMultiType(point);
                    return point.count();
                }
                Collection<XYPointMultiType> collection = new LinkedList<>();
                collection.add(other);
                collection.add(point);
                leaf.setPoints(collection);
                return point.count();
            }
            Collection<XYPointMultiType> collection = (Collection<XYPointMultiType>) currentValue;
            for (XYPointMultiType other : collection) {
                if (other.getX() == x && other.getY() == y) {
                    if (unique) {
                        throw handleUniqueViolation(indexName, other.getX(), other.getY());
                    }
                    other.addMultiType(point);
                    return point.count();
                }
            }
            collection.add(point);
            return point.count();
        }

        if (currentValue == null) {
            XYPointMultiType point = new XYPointMultiType(x, y, value);
            leaf.setPoints(point);
            return 1;
        }
        if (currentValue instanceof XYPointMultiType) {
            XYPointMultiType other = (XYPointMultiType) currentValue;
            if (other.getX() == x && other.getY() == y) {
                if (unique) {
                    throw handleUniqueViolation(indexName, other.getX(), other.getY());
                }
                other.addSingleValue(value);
                return 1;
            }
            Collection<XYPointMultiType> collection = new LinkedList<>();
            collection.add(other);
            collection.add(new XYPointMultiType(x, y, value));
            leaf.setPoints(collection);
            return 1;
        }
        Collection<XYPointMultiType> collection = (Collection<XYPointMultiType>) currentValue;
        for (XYPointMultiType other : collection) {
            if (other.getX() == x && other.getY() == y) {
                if (unique) {
                    throw handleUniqueViolation(indexName, other.getX(), other.getY());
                }
                other.addSingleValue(value);
                return 1;
            }
        }
        collection.add(new XYPointMultiType(x, y, value));
        return 1;
    }

    private static EPException handleUniqueViolation(String indexName, double x, double y) {
        return PropertyIndexedEventTableUnique.handleUniqueIndexViolation(indexName, "(" + x + "," + y + ")");
    }
}
