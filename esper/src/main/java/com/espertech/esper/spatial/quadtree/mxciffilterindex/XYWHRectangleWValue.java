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
package com.espertech.esper.spatial.quadtree.mxciffilterindex;

import com.espertech.esper.type.XYWHRectangle;

public class XYWHRectangleWValue<L> extends XYWHRectangle {
    private L value;

    public XYWHRectangleWValue(double x, double y, double w, double h, L value) {
        super(x, y, w, h);
        this.value = value;
    }

    public L getValue() {
        return value;
    }

    public void setValue(L value) {
        this.value = value;
    }
}
