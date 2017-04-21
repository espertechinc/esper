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
package com.espertech.esper.spatial.quadtree.core;

public class QuadTreeFactory {
    public final static int DEFAULT_LEAF_CAPACITY = 4;
    public final static int DEFAULT_MAX_TREE_HEIGHT = 20;

    public static <L> QuadTree<L> make(double x, double y, double width, double height, int leafCapacity, int maxTreeHeight) {
        BoundingBox bb = new BoundingBox(x, y, x + width, y + height);
        QuadTreeNodeLeaf<L> leaf = new QuadTreeNodeLeaf<>(bb, 1, null, 0);
        return new QuadTree<L>(leafCapacity, maxTreeHeight, leaf);
    }

    public static <L> QuadTree<L> make(double x, double y, double width, double height) {
        return make(x, y, width, height, DEFAULT_LEAF_CAPACITY, DEFAULT_MAX_TREE_HEIGHT);
    }
}
