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
package com.espertech.esper.spatial.quadtree.pointregion;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.core.SupportQuadTreeUtil;
import com.espertech.esper.spatial.quadtree.mxcif.SupportRectangleWithId;

import java.util.*;

public class SupportPointRegionQuadTreeUtil {

    public final static SupportQuadTreeUtil.Factory<PointRegionQuadTree<Object>> POINTREGION_FACTORY = (config) -> PointRegionQuadTreeFactory.make(config.getX(), config.getY(), config.getWidth(), config.getHeight(), config.getLeafCapacity(), config.getMaxTreeHeight());

    public static String printPoint(double x, double y) {
        return "(" + x + "," + y + ")";
    }

    public static PointRegionQuadTreeNodeLeaf<Object> navigateLeaf(PointRegionQuadTree<Object> tree, String directions) {
        return (PointRegionQuadTreeNodeLeaf<Object>) navigate(tree, directions);
    }

    public static PointRegionQuadTreeNodeLeaf<Object> navigateLeaf(PointRegionQuadTreeNode<Object> node, String directions) {
        return (PointRegionQuadTreeNodeLeaf<Object>) navigate(node, directions);
    }

    public static PointRegionQuadTreeNodeBranch<Object> navigateBranch(PointRegionQuadTree<Object> tree, String directions) {
        return (PointRegionQuadTreeNodeBranch<Object>) navigate(tree, directions);
    }

    public static PointRegionQuadTreeNode<Object> navigate(PointRegionQuadTree<Object> tree, String directions) {
        return navigate(tree.getRoot(), directions);
    }

    public static PointRegionQuadTreeNode<Object> navigate(PointRegionQuadTreeNode<Object> current, String directions) {
        if (directions.isEmpty()) {
            return current;
        }
        String[] split = directions.split(",");
        for (int i = 0; i < split.length; i++) {
            PointRegionQuadTreeNodeBranch<Object> branch = (PointRegionQuadTreeNodeBranch<Object>) current;
            if (split[i].equals("nw")) {
                current = branch.getNw();
            }
            else if (split[i].equals("ne")) {
                current = branch.getNe();
            }
            else if (split[i].equals("sw")) {
                current = branch.getSw();
            }
            else if (split[i].equals("se")) {
                current = branch.getSe();
            }
            else {
                throw new IllegalArgumentException("Invalid direction " + split[i]);
            }
        }
        return current;
    }
}
