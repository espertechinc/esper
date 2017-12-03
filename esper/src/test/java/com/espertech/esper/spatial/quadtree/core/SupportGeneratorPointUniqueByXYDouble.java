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
import com.espertech.esper.type.XYPoint;

import java.util.*;

public class SupportGeneratorPointUniqueByXYDouble implements SupportQuadTreeUtil.Generator {

    public final static SupportGeneratorPointUniqueByXYDouble INSTANCE = new SupportGeneratorPointUniqueByXYDouble();

    private SupportGeneratorPointUniqueByXYDouble() {
    }

    public boolean unique() {
        return true;
    }

    public List<SupportRectangleWithId> generate(Random random, int numPoints, double x, double y, double width, double height) {
        Map<XYPoint, SupportRectangleWithId> points = new HashMap<>();
        int pointNum = 0;
        while(points.size() < numPoints) {
            double px = x + width * random.nextDouble();
            double py = y + height * random.nextDouble();
            XYPoint point = new XYPoint(px, py);
            if (points.containsKey(point)) {
                continue;
            }
            points.put(point, new SupportRectangleWithId("P" + pointNum, px, py, 0, 0));
            pointNum++;
        }
        return new LinkedList<>(points.values());
    }
}
