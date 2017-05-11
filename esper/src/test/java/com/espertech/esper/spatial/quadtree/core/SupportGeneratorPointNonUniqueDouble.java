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

public class SupportGeneratorPointNonUniqueDouble implements SupportQuadTreeUtil.Generator {

    public final static SupportGeneratorPointNonUniqueDouble INSTANCE = new SupportGeneratorPointNonUniqueDouble();

    private SupportGeneratorPointNonUniqueDouble() {
    }

    public boolean unique() {
        return false;
    }

    public List<SupportRectangleWithId> generate(Random random, int numPoints, double x, double y, double width, double height) {
        List<SupportRectangleWithId> result = new ArrayList<>(numPoints);
        for (int i = 0; i < numPoints; i++) {
            double px = random.nextDouble() * width + x;
            double py = random.nextDouble() * height + y;
            result.add(new SupportRectangleWithId("P" + i, px, py, 0, 0));
        }
        return result;
    }
}
