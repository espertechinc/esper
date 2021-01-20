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

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportSpatialDualAABB implements Serializable {
    private static final long serialVersionUID = -351066111237079392L;
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
