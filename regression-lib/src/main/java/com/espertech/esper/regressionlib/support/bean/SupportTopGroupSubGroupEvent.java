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
public class SupportTopGroupSubGroupEvent implements Serializable {
    private static final long serialVersionUID = -7285649790786268489L;
    private final int topgroup;
    private final int subgroup;
    private final String op;

    public SupportTopGroupSubGroupEvent(int topgroup, int subgroup) {
        this.topgroup = topgroup;
        this.subgroup = subgroup;
        this.op = null;
    }

    public SupportTopGroupSubGroupEvent(int topgroup, int subgroup, String op) {
        this.topgroup = topgroup;
        this.subgroup = subgroup;
        this.op = op;
    }

    public int getTopgroup() {
        return topgroup;
    }

    public int getSubgroup() {
        return subgroup;
    }

    public String getOp() {
        return op;
    }
}
