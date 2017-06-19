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

import com.espertech.esper.spatial.quadtree.core.QuadrantAppliesEnum;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNode;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeBranch;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTreeNodeLeaf;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import static com.espertech.esper.spatial.quadtree.mxcifrowindex.MXCIFQuadTreeFilterIndexCheckBB.checkBB;

public class MXCIFQuadTreeFilterIndexDelete {
    public static void delete(double x, double y, double width, double height, MXCIFQuadTree<Object> tree) {
        MXCIFQuadTreeNode<Object> root = tree.getRoot();
        checkBB(root.getBb(), x, y, width, height);
        MXCIFQuadTreeNode<Object> replacement = deleteFromNode(x, y, width, height, root, tree);
        tree.setRoot(replacement);
    }

    private static <L> MXCIFQuadTreeNode<Object> deleteFromNode(double x, double y, double width, double height, MXCIFQuadTreeNode<Object> node, MXCIFQuadTree<Object> tree) {

        if (node instanceof MXCIFQuadTreeNodeLeaf) {
            MXCIFQuadTreeNodeLeaf<Object> leaf = (MXCIFQuadTreeNodeLeaf<Object>) node;
            boolean removed = deleteFromData(x, y, width, height, leaf.getData());
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
            branch.setNw(deleteFromNode(x, y, width, height, branch.getNw(), tree));
        } else if (quadrant == QuadrantAppliesEnum.NE) {
            branch.setNe(deleteFromNode(x, y, width, height, branch.getNe(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SW) {
            branch.setSw(deleteFromNode(x, y, width, height, branch.getSw(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SE) {
            branch.setSe(deleteFromNode(x, y, width, height, branch.getSe(), tree));
        } else if (quadrant == QuadrantAppliesEnum.SOME) {
            boolean removed = deleteFromData(x, y, width, height, branch.getData());
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
        int total = nwLeaf.getCount() + neLeaf.getCount() + swLeaf.getCount() + seLeaf.getCount() + branch.getCount();
        if (total >= tree.getLeafCapacity()) {
            return branch;
        }

        Collection<XYWHRectangleWValue<L>> collection = new LinkedList<>();
        int count = mergeChildNodes(collection, branch.getData());
        count += mergeChildNodes(collection, nwLeaf.getData());
        count += mergeChildNodes(collection, neLeaf.getData());
        count += mergeChildNodes(collection, swLeaf.getData());
        count += mergeChildNodes(collection, seLeaf.getData());
        return new MXCIFQuadTreeNodeLeaf<Object>(branch.getBb(), branch.getLevel(), collection, count);
    }

    private static <L> boolean deleteFromData(double x, double y, double width, double height, Object data) {
        if (data == null) {
            return false;
        }
        if (!(data instanceof Collection)) {
            XYWHRectangleWValue<L> rectangle = (XYWHRectangleWValue<L>) data;
            return rectangle.coordinateEquals(x, y, width, height);
        }
        Collection<XYWHRectangleWValue<L>> collection = (Collection<XYWHRectangleWValue<L>>) data;
        Iterator<XYWHRectangleWValue<L>> it = collection.iterator();
        for (; it.hasNext(); ) {
            XYWHRectangleWValue<L> rectangles = it.next();
            if (rectangles.coordinateEquals(x, y, width, height)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    private static <L> int mergeChildNodes(Collection<XYWHRectangleWValue<L>> target, Object data) {
        if (data == null) {
            return 0;
        }
        if (data instanceof XYWHRectangleWValue) {
            XYWHRectangleWValue<L> p = (XYWHRectangleWValue<L>) data;
            target.add(p);
            return 1;
        }
        Collection<XYWHRectangleWValue<L>> coll = (Collection<XYWHRectangleWValue<L>>) data;
        target.addAll(coll);
        return coll.size();
    }
}
