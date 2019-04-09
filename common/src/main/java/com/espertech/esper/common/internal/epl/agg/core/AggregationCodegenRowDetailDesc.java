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

import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;

public class AggregationCodegenRowDetailDesc {
    private final AggregationCodegenRowDetailStateDesc stateDesc;
    private final AggregationAccessorSlotPairForge[] accessAccessors;
    private final MultiKeyClassRef multiKeyClassRef;

    public AggregationCodegenRowDetailDesc(AggregationCodegenRowDetailStateDesc stateDesc, AggregationAccessorSlotPairForge[] accessAccessors, MultiKeyClassRef multiKeyClassRef) {
        this.stateDesc = stateDesc;
        this.accessAccessors = accessAccessors;
        this.multiKeyClassRef = multiKeyClassRef;
    }

    public AggregationCodegenRowDetailStateDesc getStateDesc() {
        return stateDesc;
    }

    public AggregationAccessorSlotPairForge[] getAccessAccessors() {
        return accessAccessors;
    }

    public MultiKeyClassRef getMultiKeyClassRef() {
        return multiKeyClassRef;
    }
}
