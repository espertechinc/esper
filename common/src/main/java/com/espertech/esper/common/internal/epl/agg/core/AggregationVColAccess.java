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
package com.espertech.esper.common.internal.epl.agg.core;

class AggregationVColAccess {
    private final int vcol;
    private final AggregationAccessorForge accessorForge;
    private final int stateNumber;
    private final AggregationStateFactoryForge stateForge;

    public AggregationVColAccess(int vcol, AggregationAccessorForge accessorForge, int stateNumber, AggregationStateFactoryForge stateForge) {
        this.vcol = vcol;
        this.accessorForge = accessorForge;
        this.stateNumber = stateNumber;
        this.stateForge = stateForge;
    }

    public int getVcol() {
        return vcol;
    }

    public AggregationAccessorForge getAccessorForge() {
        return accessorForge;
    }

    public int getStateNumber() {
        return stateNumber;
    }

    public AggregationStateFactoryForge getStateForge() {
        return stateForge;
    }
}
