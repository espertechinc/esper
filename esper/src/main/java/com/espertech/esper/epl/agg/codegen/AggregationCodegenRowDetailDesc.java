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
package com.espertech.esper.epl.agg.codegen;

import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPairForge;

public class AggregationCodegenRowDetailDesc {
    private final AggregationCodegenRowDetailStateDesc stateDesc;
    private final AggregationAccessorSlotPairForge[] accessAccessors;

    public AggregationCodegenRowDetailDesc(AggregationCodegenRowDetailStateDesc stateDesc, AggregationAccessorSlotPairForge[] accessAccessors) {
        this.stateDesc = stateDesc;
        this.accessAccessors = accessAccessors;
    }

    public AggregationCodegenRowDetailStateDesc getStateDesc() {
        return stateDesc;
    }

    public AggregationAccessorSlotPairForge[] getAccessAccessors() {
        return accessAccessors;
    }
}
