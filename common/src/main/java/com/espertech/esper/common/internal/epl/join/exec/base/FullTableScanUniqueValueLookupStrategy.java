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
package com.espertech.esper.common.internal.epl.join.exec.base;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTableAsSet;
import com.espertech.esper.common.internal.epl.join.rep.Cursor;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyType;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.util.Set;

/**
 * Lookup on an unindexed table returning the full table as matching events.
 */
public class FullTableScanUniqueValueLookupStrategy implements JoinExecTableLookupStrategy {
    private EventTableAsSet eventIndex;

    /**
     * Ctor.
     *
     * @param eventIndex - table to use
     */
    public FullTableScanUniqueValueLookupStrategy(EventTableAsSet eventIndex) {
        this.eventIndex = eventIndex;
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext exprEvaluatorContext) {
        InstrumentationCommon instrumentationCommon = exprEvaluatorContext.getInstrumentationProvider();
        instrumentationCommon.qIndexJoinLookup(this, eventIndex);

        Set<EventBean> result = eventIndex.allValues();
        if (result.isEmpty()) {
            instrumentationCommon.aIndexJoinLookup(null, null);
            return null;
        }
        instrumentationCommon.aIndexJoinLookup(result, null);
        return result;
    }

    public LookupStrategyDesc getStrategyDesc() {
        return LookupStrategyDesc.SCAN;
    }

    public LookupStrategyType getLookupStrategyType() {
        return LookupStrategyType.FULLTABLESCAN;
    }
}
