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

public class PointRegionQuadTreeNodeBranch<L> extends PointRegionQuadTreeNode<L> {
    private PointRegionQuadTreeNode<L> nw;
    private PointRegionQuadTreeNode<L> ne;
    private PointRegionQuadTreeNode<L> sw;
    private PointRegionQuadTreeNode<L> se;

    public PointRegionQuadTreeNodeBranch(BoundingBox bb, int level, PointRegionQuadTreeNode<L> nw, PointRegionQuadTreeNode<L> ne, PointRegionQuadTreeNode<L> sw, PointRegionQuadTreeNode<L> se) {
        super(bb, level);
        this.nw = nw;
        this.ne = ne;
        this.sw = sw;
        this.se = se;
    }

    public PointRegionQuadTreeNode<L> getNw() {
        return nw;
    }

    public void setNw(PointRegionQuadTreeNode<L> nw) {
        this.nw = nw;
    }

    public PointRegionQuadTreeNode<L> getNe() {
        return ne;
    }

    public void setNe(PointRegionQuadTreeNode<L> ne) {
        this.ne = ne;
    }

    public PointRegionQuadTreeNode<L> getSw() {
        return sw;
    }

    public void setSw(PointRegionQuadTreeNode<L> sw) {
        this.sw = sw;
    }

    public PointRegionQuadTreeNode<L> getSe() {
        return se;
    }

    public void setSe(PointRegionQuadTreeNode<L> se) {
        this.se = se;
    }
}
