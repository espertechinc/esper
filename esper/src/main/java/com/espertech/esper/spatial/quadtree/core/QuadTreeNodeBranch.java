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

public class QuadTreeNodeBranch<L> extends QuadTreeNode<L> {
    private QuadTreeNode<L> nw;
    private QuadTreeNode<L> ne;
    private QuadTreeNode<L> sw;
    private QuadTreeNode<L> se;

    public QuadTreeNodeBranch(BoundingBox bb, int level, QuadTreeNode<L> nw, QuadTreeNode<L> ne, QuadTreeNode<L> sw, QuadTreeNode<L> se) {
        super(bb, level);
        this.nw = nw;
        this.ne = ne;
        this.sw = sw;
        this.se = se;
    }

    public QuadTreeNode<L> getNw() {
        return nw;
    }

    public void setNw(QuadTreeNode<L> nw) {
        this.nw = nw;
    }

    public QuadTreeNode<L> getNe() {
        return ne;
    }

    public void setNe(QuadTreeNode<L> ne) {
        this.ne = ne;
    }

    public QuadTreeNode<L> getSw() {
        return sw;
    }

    public void setSw(QuadTreeNode<L> sw) {
        this.sw = sw;
    }

    public QuadTreeNode<L> getSe() {
        return se;
    }

    public void setSe(QuadTreeNode<L> se) {
        this.se = se;
    }
}
