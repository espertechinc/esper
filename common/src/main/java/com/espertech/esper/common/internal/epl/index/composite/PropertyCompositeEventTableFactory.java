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
package com.espertech.esper.common.internal.epl.index.composite;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganizationType;
import com.espertech.esper.common.internal.epl.join.exec.composite.CompositeIndexEnterRemove;
import com.espertech.esper.common.internal.epl.join.exec.composite.CompositeIndexEnterRemoveKeyed;
import com.espertech.esper.common.internal.epl.join.exec.composite.CompositeIndexEnterRemoveRange;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * For use when the index comprises of either two or more ranges or a unique key in combination with a range.
 * Organizes into a TreeMap&lt;key, TreeMap&lt;key2, Set&lt;EventBean&gt;&gt;, for short. The top level can also be just Map&lt;HashableMultiKey, TreeMap...&gt;.
 * Expected at least either (A) one key and one range or (B) zero keys and 2 ranges.
 * <p>
 * An alternative implementatation could have been based on "TreeMap&lt;ComparableMultiKey, Set&lt;EventBean&gt;&gt;&gt;", however the following implication arrive
 * - not applicable for range-only lookups (since there the key can be the value itself
 * - not applicable for multiple nested range as ordering not nested
 * - each add/remove and lookup would also need to construct a key object.
 */
public class PropertyCompositeEventTableFactory implements EventTableFactory {
    protected final int streamNum;
    protected final String[] optionalKeyedProps;
    protected final Class[] optKeyCoercedTypes;
    protected final EventPropertyValueGetter hashGetter;
    protected final MultiKeyFromObjectArray transformFireAndForget;
    protected final String[] rangeProps;
    protected final Class[] optRangeCoercedTypes;
    protected final EventPropertyValueGetter[] rangeGetters;
    protected final CompositeIndexEnterRemove chain;

    public PropertyCompositeEventTableFactory(int streamNum, String[] optionalKeyedProps, Class[] optKeyCoercedTypes, EventPropertyValueGetter hashGetter, MultiKeyFromObjectArray transformFireAndForget, String[] rangeProps, Class[] optRangeCoercedTypes, EventPropertyValueGetter[] rangeGetters) {
        this.streamNum = streamNum;
        this.optionalKeyedProps = optionalKeyedProps;
        this.optKeyCoercedTypes = optKeyCoercedTypes;
        this.hashGetter = hashGetter;
        this.transformFireAndForget = transformFireAndForget;
        this.rangeProps = rangeProps;
        this.optRangeCoercedTypes = optRangeCoercedTypes;
        this.rangeGetters = rangeGetters;

        // construct chain
        List<CompositeIndexEnterRemove> enterRemoves = new ArrayList<CompositeIndexEnterRemove>();
        if (optionalKeyedProps != null && optionalKeyedProps.length > 0) {
            enterRemoves.add(new CompositeIndexEnterRemoveKeyed(hashGetter));
        }
        for (EventPropertyValueGetter rangeGetter : rangeGetters) {
            enterRemoves.add(new CompositeIndexEnterRemoveRange(rangeGetter));
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

    public EventTable[] makeEventTables(AgentInstanceContext agentInstanceContext, Integer subqueryNumber) {
        return new EventTable[]{new PropertyCompositeEventTableImpl(this)};
    }

    public Class getEventTableClass() {
        return PropertyCompositeEventTable.class;
    }

    public CompositeIndexEnterRemove getChain() {
        return chain;
    }

    public String toQueryPlan() {
        return this.getClass().getName() +
                " streamNum=" + streamNum +
                " keys=" + Arrays.toString(optionalKeyedProps) +
                " ranges=" + Arrays.toString(rangeProps);
    }

    protected EventTableOrganization getOrganization() {
        return new EventTableOrganization(null, false, optKeyCoercedTypes != null || optRangeCoercedTypes != null, streamNum, combinedPropertyLists(optionalKeyedProps, rangeProps), EventTableOrganizationType.COMPOSITE);
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
