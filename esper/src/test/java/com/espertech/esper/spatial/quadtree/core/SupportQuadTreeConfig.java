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

public class SupportQuadTreeConfig {
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final int leafCapacity;
    private final int maxTreeHeight;

    public SupportQuadTreeConfig(double x, double y, double width, double height, int leafCapacity, int maxTreeHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.leafCapacity = leafCapacity;
        this.maxTreeHeight = maxTreeHeight;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public int getLeafCapacity() {
        return leafCapacity;
    }

    public int getMaxTreeHeight() {
        return maxTreeHeight;
    }

    public double getMaxX() {
        return x+width;
    }

    public double getMaxY() {
        return y+height;
    }
}
