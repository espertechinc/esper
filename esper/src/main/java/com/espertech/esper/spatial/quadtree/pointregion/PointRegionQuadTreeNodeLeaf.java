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

import com.espertech.esper.spatial.quadtree.core.BoundingBox;

public class PointRegionQuadTreeNodeLeaf<L> extends PointRegionQuadTreeNode<L> {
    private L points;
    private int count;

    public PointRegionQuadTreeNodeLeaf(BoundingBox bb, int level, L points, int count) {
        super(bb, level);
        this.points = points;
        this.count = count;
    }

    public L getPoints() {
        return points;
    }

    public void setPoints(L points) {
        this.points = points;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incCount(int numAdded) {
        count += numAdded;
    }

    public void decCount() {
        count--;
    }
}
