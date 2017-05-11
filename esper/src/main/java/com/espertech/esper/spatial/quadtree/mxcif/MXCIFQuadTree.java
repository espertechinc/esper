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
package com.espertech.esper.spatial.quadtree.mxcif;

/**
 * <p>
 *     Quad tree.
 * </p>
 * <p>
 *     Nodes can either be leaf nodes or branch nodes. Both leaf nodes and branch nodes have data. "Data" is modelled as a generic type.
 * </p>
 * <p>
 *     Branch nodes have 4 regions or child nodes that subdivide the parent region in NW/NE/SW/SE.
 * </p>
 * <p>
 *     The tree is polymorphic: leaf nodes can become branches and branches can become leafs.
 * </p>
 * <p>
 *     Manipulation and querying of the quad tree is done through tool classes.
 *     As the tree can be polymorphic users should not hold on to the root node as it could change.
 * </p>
 */
public class MXCIFQuadTree<L> {
    private final int leafCapacity;
    private final int maxTreeHeight;
    private MXCIFQuadTreeNode<L> root;

    public MXCIFQuadTree(int leafCapacity, int maxTreeHeight, MXCIFQuadTreeNode<L> root) {
        this.leafCapacity = leafCapacity;
        this.maxTreeHeight = maxTreeHeight;
        this.root = root;
    }

    public int getLeafCapacity() {
        return leafCapacity;
    }

    public int getMaxTreeHeight() {
        return maxTreeHeight;
    }

    public MXCIFQuadTreeNode<L> getRoot() {
        return root;
    }

    public void setRoot(MXCIFQuadTreeNode<L> root) {
        this.root = root;
    }

    public void clear() {
        root = new MXCIFQuadTreeNodeLeaf<>(root.getBb(), root.getLevel(), null, 0);
    }
}
