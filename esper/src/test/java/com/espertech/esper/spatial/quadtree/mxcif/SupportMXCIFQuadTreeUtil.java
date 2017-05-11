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

import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil;

public class SupportMXCIFQuadTreeUtil {

    public final static SupportQuadTreeUtil.Factory<MXCIFQuadTree<Object>> MXCIF_FACTORY = config -> MXCIFQuadTreeFactory.make(config.getX(), config.getY(), config.getWidth(), config.getHeight(), config.getLeafCapacity(), config.getMaxTreeHeight());

    public static MXCIFQuadTreeNodeLeaf<Object> navigateLeaf(MXCIFQuadTree<Object> tree, String directions) {
        return (MXCIFQuadTreeNodeLeaf<Object>) navigate(tree, directions);
    }

    public static MXCIFQuadTreeNodeLeaf<Object> navigateLeaf(MXCIFQuadTreeNode<Object> node, String directions) {
        return (MXCIFQuadTreeNodeLeaf<Object>) navigate(node, directions);
    }

    public static MXCIFQuadTreeNodeBranch<Object> navigateBranch(MXCIFQuadTree<Object> tree, String directions) {
        return (MXCIFQuadTreeNodeBranch<Object>) navigate(tree, directions);
    }

    public static MXCIFQuadTreeNode<Object> navigate(MXCIFQuadTree<Object> tree, String directions) {
        return navigate(tree.getRoot(), directions);
    }

    public static MXCIFQuadTreeNode<Object> navigate(MXCIFQuadTreeNode<Object> current, String directions) {
        if (directions.isEmpty()) {
            return current;
        }
        String[] split = directions.split(",");
        for (int i = 0; i < split.length; i++) {
            MXCIFQuadTreeNodeBranch<Object> branch = (MXCIFQuadTreeNodeBranch<Object>) current;
            if (split[i].equals("nw")) {
                current = branch.getNw();
            } else if (split[i].equals("ne")) {
                current = branch.getNe();
            } else if (split[i].equals("sw")) {
                current = branch.getSw();
            } else if (split[i].equals("se")) {
                current = branch.getSe();
            } else {
                throw new IllegalArgumentException("Invalid direction " + split[i]);
            }
        }
        return current;
    }
}
