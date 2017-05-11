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

public class MXCIFQuadTreeNodeBranch<L> extends MXCIFQuadTreeNode<L> {
    private MXCIFQuadTreeNode<L> nw;
    private MXCIFQuadTreeNode<L> ne;
    private MXCIFQuadTreeNode<L> sw;
    private MXCIFQuadTreeNode<L> se;

    public MXCIFQuadTreeNodeBranch(BoundingBox bb, int level, L data, int dataCount, MXCIFQuadTreeNode<L> nw, MXCIFQuadTreeNode<L> ne, MXCIFQuadTreeNode<L> sw, MXCIFQuadTreeNode<L> se) {
        super(bb, level, data, dataCount);
        this.nw = nw;
        this.ne = ne;
        this.sw = sw;
        this.se = se;
    }

    public MXCIFQuadTreeNode<L> getNw() {
        return nw;
    }

    public void setNw(MXCIFQuadTreeNode<L> nw) {
        this.nw = nw;
    }

    public MXCIFQuadTreeNode<L> getNe() {
        return ne;
    }

    public void setNe(MXCIFQuadTreeNode<L> ne) {
        this.ne = ne;
    }

    public MXCIFQuadTreeNode<L> getSw() {
        return sw;
    }

    public void setSw(MXCIFQuadTreeNode<L> sw) {
        this.sw = sw;
    }

    public MXCIFQuadTreeNode<L> getSe() {
        return se;
    }

    public void setSe(MXCIFQuadTreeNode<L> se) {
        this.se = se;
    }
}
