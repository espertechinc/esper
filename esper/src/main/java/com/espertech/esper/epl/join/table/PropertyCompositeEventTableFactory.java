/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexEnterRemove;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexEnterRemoveKeyed;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexEnterRemoveRange;
import com.espertech.esper.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * For use when the index comprises of either two or more ranges or a unique key in combination with a range.
 * Organizes into a TreeMap<key, TreeMap<key2, Set<EventBean>>, for short. The top level can also be just Map<MultiKeyUntyped, TreeMap...>.
 * Expected at least either (A) one key and one range or (B) zero keys and 2 ranges.
 * <p>
 * An alternative implementatation could have been based on "TreeMap<ComparableMultiKey, Set<EventBean>>>", however the following implication arrive
 * - not applicable for range-only lookups (since there the key can be the value itself
 * - not applicable for multiple nested range as ordering not nested
 * - each add/remove and lookup would also need to construct a key object.
 */
public class PropertyCompositeEventTableFactory implements EventTableFactory
{
    private final int streamNum;
    private final String[] optionalKeyedProps;
    private final String[] rangeProps;
    private final CompositeIndexEnterRemove chain;
    private final Class[] optKeyCoercedTypes;
    private final Class[] optRangeCoercedTypes;

    /**
     * Ctor.
     * @param streamNum - the stream number that is indexed
     * @param eventType - types of events indexed
     * @param optRangeCoercedTypes - property types
     */
    public PropertyCompositeEventTableFactory(int streamNum, EventType eventType, String[] optionalKeyedProps, Class[] optKeyCoercedTypes, String[] rangeProps, Class[] optRangeCoercedTypes)
    {
        this.streamNum = streamNum;
        this.rangeProps = rangeProps;
        this.optionalKeyedProps = optionalKeyedProps;
        this.optKeyCoercedTypes = optKeyCoercedTypes;
        this.optRangeCoercedTypes = optRangeCoercedTypes;

        // construct chain
        List<CompositeIndexEnterRemove> enterRemoves = new ArrayList<CompositeIndexEnterRemove>();
        if (optionalKeyedProps != null && optionalKeyedProps.length > 0) {
            enterRemoves.add(new CompositeIndexEnterRemoveKeyed(eventType, optionalKeyedProps, optKeyCoercedTypes));
        }
        int count = 0;
        for (String rangeProp : rangeProps) {
            Class coercionType = optRangeCoercedTypes == null ? null : optRangeCoercedTypes[count];
            enterRemoves.add(new CompositeIndexEnterRemoveRange(eventType, rangeProp, coercionType));
            count++;
        }

        // Hook up as chain for remove
        CompositeIndexEnterRemove last = null;
        for (CompositeIndexEnterRemove action : enterRemoves) {
            if (last != null) {
                last.setNext(action);
            }
            last = action;
        }
        chain = enterRemoves.get(0);
    }

    public EventTable[] makeEventTables(EventTableFactoryTableIdent tableIdent) {
        EventTableOrganization organization = new EventTableOrganization(null, false, optKeyCoercedTypes != null || optRangeCoercedTypes != null, streamNum, combinedPropertyLists(optionalKeyedProps, rangeProps), EventTableOrganization.EventTableOrganizationType.COMPOSITE);
        return new EventTable[] {new PropertyCompositeEventTable((optionalKeyedProps != null && optionalKeyedProps.length > 0), chain, optKeyCoercedTypes, optRangeCoercedTypes, organization)};
    }

    public Class getEventTableClass() {
        return PropertyCompositeEventTable.class;
    }

    public String toQueryPlan()
    {
        return this.getClass().getName() +
                " streamNum=" + streamNum +
                " keys=" + Arrays.toString(optionalKeyedProps) +
                " ranges=" + Arrays.toString(rangeProps);
    }

    private String[] combinedPropertyLists(String[] optionalKeyedProps, String[] rangeProps) {
        if (optionalKeyedProps == null) {
            return rangeProps;
        }
        if (rangeProps == null) {
            return optionalKeyedProps;
        }
        return (String[]) CollectionUtil.addArrays(optionalKeyedProps, rangeProps);
    }
}
