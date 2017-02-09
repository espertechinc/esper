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
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

import java.util.List;

/**
 * Index lookup strategy that coerces the key values before performing a lookup.
 */
public class SubordIndexedTableLookupStrategyCoercingFactory extends SubordIndexedTableLookupStrategyExprFactory {
    private Class[] coercionTypes;

    public SubordIndexedTableLookupStrategyCoercingFactory(boolean isNWOnTrigger, int numStreamsOuter, List<SubordPropHashKey> hashKeys, Class[] coercionTypes) {
        super(isNWOnTrigger, numStreamsOuter, hashKeys);
        this.coercionTypes = coercionTypes;
    }

    @Override
    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw) {
        if (isNWOnTrigger) {
            return new SubordIndexedTableLookupStrategyCoercingNW(evaluators, (PropertyIndexedEventTable) eventTable[0], coercionTypes, strategyDesc);
        } else {
            return new SubordIndexedTableLookupStrategyCoercing(numStreamsOuter, evaluators, (PropertyIndexedEventTable) eventTable[0], coercionTypes, strategyDesc);
        }
    }

}
