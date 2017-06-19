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

import com.espertech.esper.spatial.quadtree.core.QuadrantAppliesEnum;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNode;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class MXCIFQuadTreeRowIndexRemove {

    /**
     * Remove value.
     *
     * @param x      x
     * @param y      y
     * @param width  width
     * @param height height
     * @param value  value to remove
     * @param tree   quadtree
     */
    public static void remove(double x, double y, double width, double height, Object value, MXCIFQuadTree<Object> tree) {
        MXCIFQuadTreeNode<Object> root = tree.getRoot();
        MXCIFQuadTreeNode<Object> replacement = removeFromNode(x, y, width, height, value, root, tree);
        tree.setRoot(replacement);
    }

    private static MXCIFQuadTreeNode<Object> removeFromNode(double x, double y, double width, double height, Object value, MXCIFQuadTreeNode<Object> node, MXCIFQuadTree<Object> tree) {

        if (node instanceof MXCIFQuadTreeNodeLeaf) {
            MXCIFQuadTreeNodeLeaf<Object> leaf = (MXCIFQuadTreeNodeLeaf<Object>) node;
            boolean removed = removeFromPoints(x, y, width, height, value, leaf.getData());
            if (removed) {
                leaf.decCount();
                if (leaf.getCount() == 0) {
                    leaf.setData(null);
                }
            }
            return leaf;
        }

        MXCIFQuadTreeNodeBranch<Object> branch = (MXCIFQuadTreeNodeBranch<Object>) node;
        QuadrantAppliesEnum quadrant = node.getBb().getQuadrantApplies(x, y, width, height);
        if (quadrant == QuadrantAppliesEnum.NW) {
            branch.setNw(removeFromNode(x, y, width, height, value, branch.getNw(), tree));
        } else if (quadrant == QuadrantAppliesEnum.NE) {
            branch.setNe(removeFromNode(x, y, width, height, value, branch.getNe(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SW) {
            branch.setSw(removeFromNode(x, y, width, height, value, branch.getSw(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SE) {
            branch.setSe(removeFromNode(x, y, width, height, value, branch.getSe(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SOME) {
            boolean removed = removeFromPoints(x, y, width, height, value, branch.getData());
            if (removed) {
                branch.decCount();
                if (branch.getCount() == 0) {
                    branch.setData(null);
                }
            }
        }

        if (!(branch.getNw() instanceof MXCIFQuadTreeNodeLeaf) || !(branch.getNe() instanceof MXCIFQuadTreeNodeLeaf) || !(branch.getSw() instanceof MXCIFQuadTreeNodeLeaf) || !(branch.getSe() instanceof MXCIFQuadTreeNodeLeaf)) {
            return branch;
        }
        MXCIFQuadTreeNodeLeaf<Object> nwLeaf = (MXCIFQuadTreeNodeLeaf<Object>) branch.getNw();
        MXCIFQuadTreeNodeLeaf<Object> neLeaf = (MXCIFQuadTreeNodeLeaf<Object>) branch.getNe();
        MXCIFQuadTreeNodeLeaf<Object> swLeaf = (MXCIFQuadTreeNodeLeaf<Object>) branch.getSw();
        MXCIFQuadTreeNodeLeaf<Object> seLeaf = (MXCIFQuadTreeNodeLeaf<Object>) branch.getSe();
        int total = branch.getCount() + nwLeaf.getCount() + neLeaf.getCount() + swLeaf.getCount() + seLeaf.getCount();
        if (total >= tree.getLeafCapacity()) {
            return branch;
        }

        Collection<XYWHRectangleMultiType> collection = new LinkedList<>();
        int count = mergeChildNodes(collection, branch.getData());
        count += mergeChildNodes(collection, nwLeaf.getData());
        count += mergeChildNodes(collection, neLeaf.getData());
        count += mergeChildNodes(collection, swLeaf.getData());
        count += mergeChildNodes(collection, seLeaf.getData());
        return new MXCIFQuadTreeNodeLeaf<Object>(branch.getBb(), branch.getLevel(), collection, count);
    }

    private static boolean removeFromPoints(double x, double y, double width, double height, Object value, Object data) {
        if (data == null) {
            return false;
        }
        if (!(data instanceof Collection)) {
            XYWHRectangleMultiType rectangle = (XYWHRectangleMultiType) data;
            if (rectangle.coordinateEquals(x, y, width, height)) {
                boolean removed = rectangle.remove(value);
                if (removed) {
                    return true;
                }
            }
            return false;
        }
        Collection<XYWHRectangleMultiType> collection = (Collection<XYWHRectangleMultiType>) data;
        Iterator<XYWHRectangleMultiType> it = collection.iterator();
        for (; it.hasNext(); ) {
            XYWHRectangleMultiType rectangle = it.next();
            if (rectangle.coordinateEquals(x, y, width, height)) {
                boolean removed = rectangle.remove(value);
                if (removed) {
                    if (rectangle.isEmpty()) {
                        it.remove();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static int mergeChildNodes(Collection<XYWHRectangleMultiType> target, Object data) {
        if (data == null) {
            return 0;
        }
        if (data instanceof XYWHRectangleMultiType) {
            XYWHRectangleMultiType r = (XYWHRectangleMultiType) data;
            target.add(r);
            return r.count();
        }
        Collection<XYWHRectangleMultiType> coll = (Collection<XYWHRectangleMultiType>) data;
        int total = 0;
        for (XYWHRectangleMultiType r : coll) {
            target.add(r);
            total += r.count();
        }
        return total;
    }
}
