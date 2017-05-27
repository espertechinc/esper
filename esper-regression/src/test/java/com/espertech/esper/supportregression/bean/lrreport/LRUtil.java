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
package com.espertech.esper.supportregression.bean.lrreport;

public class LRUtil {
    public static double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public static boolean inrect(Rectangle r, Location l) {
        int minX = getMin(r.getX1(), r.getX2());
        int minY = getMin(r.getY1(), r.getY2());
        int maxX = getMax(r.getX1(), r.getX2());
        int maxY = getMax(r.getY1(), r.getY2());
        if (l.getX() >= minX &&
                l.getX() <= maxX &&
                l.getY() >= minY &&
                l.getY() <= maxY) {
            return true;
        }
        return false;
    }

    private static int getMin(int numOne, int numTwo) {
        return numOne < numTwo ? numOne : numTwo;
    }

    private static int getMax(int numOne, int numTwo) {
        return numOne > numTwo ? numOne : numTwo;
    }
}
