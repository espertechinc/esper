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

public class SupportTopGroupSubGroupEvent implements Serializable {
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
