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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.plugin.*;

public class SupportAggMFEventsAsListStateForge implements PlugInAggregationMultiFunctionStateForge {

    public PlugInAggregationMultiFunctionStateFactory getStateFactory() {
        return new SupportAggMFEventsAsListStateFactory();
    }

    public void rowMemberCodegen(PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext context) {
        throw new IllegalStateException();
    }

    public void applyEnterCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        throw new IllegalStateException();
    }

    public void applyLeaveCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        throw new IllegalStateException();
    }

    public void clearCodegen(PlugInAggregationMultiFunctionStateForgeCodegenClearContext context) {
        throw new IllegalStateException();
    }
}
