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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.unindexed.UnindexedEventTable;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;

/**
 * Factory for lookup on an unindexed table returning the full table as matching events.
 */
public class SubordFullTableScanLookupStrategyFactory implements SubordTableLookupStrategyFactory {
    public final static EPTypeClass EPTYPE = new EPTypeClass(SubordFullTableScanLookupStrategyFactory.class);

    public final static SubordFullTableScanLookupStrategyFactory INSTANCE = new SubordFullTableScanLookupStrategyFactory();

    private SubordFullTableScanLookupStrategyFactory() {
    }

    public SubordFullTableScanLookupStrategy makeStrategy(EventTable[] eventTable, ExprEvaluatorContext exprEvaluatorContext, VirtualDWView vdw) {
        return new SubordFullTableScanLookupStrategy((UnindexedEventTable) eventTable[0]);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return LookupStrategyDesc.SCAN;
    }
}
