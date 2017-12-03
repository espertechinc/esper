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
import com.espertech.esper.type.XYWHRectangle;

import java.util.*;

public class SupportGeneratorRectangleUniqueByXYWH implements SupportQuadTreeUtil.Generator {

    public final static SupportGeneratorRectangleUniqueByXYWH INSTANCE = new SupportGeneratorRectangleUniqueByXYWH();

    private SupportGeneratorRectangleUniqueByXYWH() {
    }

    public boolean unique() {
        return true;
    }

    public List<SupportRectangleWithId> generate(Random random, int numPoints, double x, double y, double width, double height) {
        Map<XYWHRectangle, SupportRectangleWithId> rectangles = new HashMap<>();
        int pointNum = 0;
        while(rectangles.size() < numPoints) {
            double rx;
            double ry;
            double rwidth;
            double rheight;
            while(true) {
                rx = x + width * random.nextDouble() - 5;
                ry = y + height * random.nextDouble() - 5;
                rwidth = width * random.nextDouble();
                rheight = height * random.nextDouble();
                if (BoundingBox.intersectsBoxIncludingEnd(x, y, x+width, y+height, rx, ry, rwidth, rheight)) {
                    break;
                }
            }
            XYWHRectangle rectangle = new XYWHRectangle(rx, ry, rwidth, rheight);
            if (rectangles.containsKey(rectangle)) {
                continue;
            }
            rectangles.put(rectangle, new SupportRectangleWithId("P" + pointNum, rectangle.getX(), rectangle.getY(), rectangle.getW(), rectangle.getH()));
            pointNum++;
        }
        return new LinkedList<>(rectangles.values());
    }
}
