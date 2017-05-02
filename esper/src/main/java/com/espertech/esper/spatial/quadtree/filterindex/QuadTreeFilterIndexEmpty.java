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

import com.espertech.esper.spatial.quadtree.core.QuadTree;
import com.espertech.esper.spatial.quadtree.core.QuadTreeNode;
import com.espertech.esper.spatial.quadtree.core.QuadTreeNodeLeaf;

public class QuadTreeFilterIndexEmpty {
    public static boolean isEmpty(QuadTree<Object> quadTree) {
        return isEmpty(quadTree.getRoot());
    }

    public static boolean isEmpty(QuadTreeNode<Object> node) {
        if (node instanceof QuadTreeNodeLeaf) {
            QuadTreeNodeLeaf<Object> leaf = (QuadTreeNodeLeaf<Object>) node;
            return leaf.getPoints() == null;
        }
        return false;
    }
}
