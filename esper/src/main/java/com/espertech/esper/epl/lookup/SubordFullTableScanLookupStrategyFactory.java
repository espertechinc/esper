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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.UnindexedEventTable;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

/**
 * Factory for lookup on an unindexed table returning the full table as matching events.
 */
public class SubordFullTableScanLookupStrategyFactory implements SubordTableLookupStrategyFactory {

    public SubordFullTableScanLookupStrategyFactory() {
    }

    public SubordFullTableScanLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw) {
        return new SubordFullTableScanLookupStrategy((UnindexedEventTable) eventTable[0]);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
