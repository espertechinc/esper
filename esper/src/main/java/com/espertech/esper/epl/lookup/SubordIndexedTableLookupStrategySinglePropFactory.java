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

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingle;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingleUnique;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.event.EventBeanUtility;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordIndexedTableLookupStrategySinglePropFactory implements SubordTableLookupStrategyFactory {
    private final String property;

    /**
     * Stream numbers to get key values from.
     */
    protected final int keyStreamNum;

    /**
     * Getters to use to get key values.
     */
    protected final EventPropertyGetter propertyGetter;

    /**
     * Ctor.
     *
     * @param eventTypes    is the event types per stream
     * @param keyStreamNum  is the stream number per property
     * @param property      is the key properties
     * @param isNWOnTrigger for named window on-trigger
     */
    public SubordIndexedTableLookupStrategySinglePropFactory(boolean isNWOnTrigger, EventType[] eventTypes, int keyStreamNum, String property) {
        this.keyStreamNum = keyStreamNum + (isNWOnTrigger ? 1 : 0); // for on-trigger the key will be provided in a {1,2,...} stream and not {0,...}
        this.property = property;
        propertyGetter = EventBeanUtility.getAssertPropertyGetter(eventTypes, keyStreamNum, property);
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw) {
        if (eventTable[0] instanceof PropertyIndexedEventTableSingleUnique) {
            return new SubordIndexedTableLookupStrategySinglePropUnique(keyStreamNum, propertyGetter, (PropertyIndexedEventTableSingleUnique) eventTable[0],
                    new LookupStrategyDesc(LookupStrategyType.SINGLEPROPUNIQUE, new String[]{property}));
        }
        LookupStrategyDesc desc = new LookupStrategyDesc(LookupStrategyType.SINGLEPROPNONUNIQUE, new String[]{property});
        return new SubordIndexedTableLookupStrategySingleProp(keyStreamNum, propertyGetter, (PropertyIndexedEventTableSingle) eventTable[0], desc);
    }

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " property=" + property + " stream=" + keyStreamNum;
    }
}
