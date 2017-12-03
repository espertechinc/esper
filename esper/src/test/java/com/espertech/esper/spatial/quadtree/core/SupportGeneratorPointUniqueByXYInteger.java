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

public class SupportGeneratorPointUniqueByXYInteger implements SupportQuadTreeUtil.Generator {

    public final static SupportGeneratorPointUniqueByXYInteger INSTANCE = new SupportGeneratorPointUniqueByXYInteger();

    private SupportGeneratorPointUniqueByXYInteger() {
    }

    public boolean unique() {
        return true;
    }

    public List<SupportRectangleWithId> generate(Random random, int numPoints, double x, double y, double width, double height) {
        Map<XYPoint, SupportRectangleWithId> points = new HashMap<>();
        int pointNum = 0;
        while (points.size() < numPoints) {
            float fx = random.nextInt(numPoints);
            float fy = random.nextInt(numPoints);
            XYPoint p = new XYPoint(fx, fy);
            if (points.containsKey(p)) {
                continue;
            }
            points.put(p, new SupportRectangleWithId("P" + pointNum, fx, fy, 0, 0));
            pointNum++;
        }
        return new LinkedList<>(points.values());
    }
}
