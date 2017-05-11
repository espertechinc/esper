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

import com.espertech.esper.spatial.quadtree.mxcif.SupportRectangleWithId;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SupportGeneratorRectangleNonUniqueIntersecting implements SupportQuadTreeUtil.Generator {

    public final static SupportGeneratorRectangleNonUniqueIntersecting INSTANCE = new SupportGeneratorRectangleNonUniqueIntersecting();

    private SupportGeneratorRectangleNonUniqueIntersecting() {
    }

    public boolean unique() {
        return false;
    }

    public List<SupportRectangleWithId> generate(Random random, int numPoints, double x, double y, double width, double height) {
        List<SupportRectangleWithId> points = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            double rx;
            double ry;
            double rwidth;
            double rheight;
            while(true) {
                rx = random.nextDouble() * width + x;
                ry = random.nextDouble() * height + y;
                rwidth = random.nextDouble() * 10d;
                rheight = random.nextDouble() * 10d;
                if (BoundingBox.intersectsBoxIncludingEnd(x, y, x+width, y+height, rx, ry, rwidth, rheight)) {
                    break;
                }
            }
            points.add(new SupportRectangleWithId("P" + i, rx, ry, rwidth, rheight));
        }
        return points;
    }
}
