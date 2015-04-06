/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexEnterRemove;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

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
public class PropertyCompositeEventTable implements EventTable
{
    private final CompositeIndexEnterRemove chain;
    private final Class[] optKeyCoercedTypes;
    private final Class[] optRangeCoercedTypes;
    private final EventTableOrganization organization;

    /**
     * Index table (sorted and/or keyed, always nested).
     */
    protected final Map<Object, Object> index;

    public PropertyCompositeEventTable(boolean isHashKeyed, CompositeIndexEnterRemove chain, Class[] optKeyCoercedTypes, Class[] optRangeCoercedTypes, EventTableOrganization organization)
    {
        this.chain = chain;
        this.optKeyCoercedTypes = optKeyCoercedTypes;
        this.optRangeCoercedTypes = optRangeCoercedTypes;
        this.organization = organization;

        if (isHashKeyed) {
            index = new HashMap<Object, Object>();
        }
        else {
            index = new TreeMap<Object, Object>();
        }
    }

    public Map<Object, Object> getIndex() {
        return index;
    }

    public void addRemove(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qIndexAddRemove(this, newData, oldData);}
        if (newData != null) {
            for (EventBean theEvent : newData) {
                add(theEvent);
            }
        }
        if (oldData != null) {
            for (EventBean theEvent : oldData) {
                remove(theEvent);
            }
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aIndexAddRemove();}
    }

    /**
     * Add an array of events. Same event instance is not added twice. Event properties should be immutable.
     * Allow null passed instead of an empty array.
     * @param events to add
     * @throws IllegalArgumentException if the event was already existed in the index
     */
    public void add(EventBean[] events)
    {
        if (events != null) {

            if (InstrumentationHelper.ENABLED && events.length > 0) {
                InstrumentationHelper.get().qIndexAdd(this, events);
                for (EventBean theEvent : events) {
                    add(theEvent);
                }
                InstrumentationHelper.get().aIndexAdd();
                return;
            }

            for (EventBean theEvent : events) {
                add(theEvent);
            }
        }
    }

    /**
     * Remove events.
     * @param events to be removed, can be null instead of an empty array.
     * @throws IllegalArgumentException when the event could not be removed as its not in the index
     */
    public void remove(EventBean[] events)
    {
        if (events != null) {

            if (InstrumentationHelper.ENABLED && events.length > 0) {
                InstrumentationHelper.get().qIndexRemove(this, events);
                for (EventBean theEvent : events) {
                    remove(theEvent);
                }
                InstrumentationHelper.get().aIndexRemove();
                return;
            }

            for (EventBean theEvent : events) {
                remove(theEvent);
            }
        }
    }

    public void add(EventBean theEvent)
    {
        chain.enter(theEvent, index);
    }

    public void remove(EventBean theEvent)
    {
        chain.remove(theEvent, index);
    }

    public boolean isEmpty()
    {
        return index.isEmpty();
    }

    public Iterator<EventBean> iterator()
    {
        HashSet<EventBean> result = new LinkedHashSet<EventBean>();
        chain.getAll(result, index);
        return result.iterator();
    }

    public void clear()
    {
        index.clear();
    }

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan()
    {
        return this.getClass().getName();
    }

    public Class[] getOptRangeCoercedTypes() {
        return optRangeCoercedTypes;
    }

    public Class[] getOptKeyCoercedTypes() {
        return optKeyCoercedTypes;
    }

    public Integer getNumberOfEvents() {
        return null;
    }

    public int getNumKeys() {
        return index.size();
    }

    public EventTableOrganization getOrganization() {
        return organization;
    }

    private static Log log = LogFactory.getLog(PropertyCompositeEventTable.class);
}
