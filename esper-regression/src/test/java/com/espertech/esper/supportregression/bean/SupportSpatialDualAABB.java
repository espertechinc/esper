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
package com.espertech.esper.supportregression.bean;

import java.io.Serializable;

public class SupportSpatialDualAABB implements Serializable {
    private SupportSpatialAABB one;
    private SupportSpatialAABB two;

    public SupportSpatialDualAABB(SupportSpatialAABB one, SupportSpatialAABB two) {
        this.one = one;
        this.two = two;
    }

    public SupportSpatialAABB getOne() {
        return one;
    }

    public SupportSpatialAABB getTwo() {
        return two;
    }
}
