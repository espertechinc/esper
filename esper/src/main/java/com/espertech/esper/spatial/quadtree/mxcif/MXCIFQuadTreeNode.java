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

import com.espertech.esper.spatial.quadtree.core.BoundingBox;

public abstract class MXCIFQuadTreeNode<L> {
    private final BoundingBox bb;
    private final int level;
    private L data;
    private int count;

    public MXCIFQuadTreeNode(BoundingBox bb, int level, L data, int count) {
        this.bb = bb;
        this.level = level;
        this.data = data;
        this.count = count;
    }

    public BoundingBox getBb() {
        return bb;
    }

    public int getLevel() {
        return level;
    }

    public L getData() {
        return data;
    }

    public int getCount() {
        return count;
    }

    public void setData(L data) {
        this.data = data;
    }

    public void incCount(int numAdded) {
        count += numAdded;
    }

    public void decCount() {
        count--;
    };

    public void setCount(int count) {
        this.count = count;
    }
}
