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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;

public class SupportEventRectangleWithOffset implements Serializable {
    private final String id;
    private final Double xOffset;
    private final Double yOffset;
    private final Double x;
    private final Double y;
    private final Double width;
    private final Double height;

    public SupportEventRectangleWithOffset(String id, Double xOffset, Double yOffset, Double x, Double y, Double width, Double height) {
        this.id = id;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getId() {
        return id;
    }

    public Double getxOffset() {
        return xOffset;
    }

    public Double getyOffset() {
        return yOffset;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Double getWidth() {
        return width;
    }

    public Double getHeight() {
        return height;
    }
}
