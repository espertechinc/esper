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
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingle;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

/**
 * Index lookup strategy that coerces the key values before performing a lookup.
 */
public class SubordIndexedTableLookupStrategySingleCoercingFactory extends SubordIndexedTableLookupStrategySingleExprFactory {
    private Class coercionType;

    public SubordIndexedTableLookupStrategySingleCoercingFactory(boolean isNWOnTrigger, int streamCountOuter, SubordPropHashKey hashKey, Class coercionType) {
        super(isNWOnTrigger, streamCountOuter, hashKey);
        this.coercionType = coercionType;
    }

    @Override
    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw) {
        if (isNWOnTrigger) {
            return new SubordIndexedTableLookupStrategySingleCoercingNW(evaluator, (PropertyIndexedEventTableSingle) eventTable[0], coercionType, strategyDesc);
        } else {
            return new SubordIndexedTableLookupStrategySingleCoercing(streamCountOuter, evaluator, (PropertyIndexedEventTableSingle) eventTable[0], coercionType, strategyDesc);
        }
    }
}
