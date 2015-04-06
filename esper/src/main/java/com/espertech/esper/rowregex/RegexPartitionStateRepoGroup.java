/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Partition-by implementation for partition state.
 */
public class RegexPartitionStateRepoGroup implements RegexPartitionStateRepo
{
    /**
     * Empty state collection initial threshold.
     */
    public final static int INITIAL_COLLECTION_MIN = 100;

    private final RegexPartitionStateRepoGroupMeta meta;
    private final RegexPartitionStateRandomAccessGetter getter;
    private final Map<Object, RegexPartitionState> states;

    private int currentCollectionSize = INITIAL_COLLECTION_MIN;

    /**
     * Ctor.
     * @param getter for "prev" function access
     * @param meta general metadata for grouping
     */
    public RegexPartitionStateRepoGroup(RegexPartitionStateRandomAccessGetter getter,
                                        RegexPartitionStateRepoGroupMeta meta)
    {
        this.getter = getter;
        this.meta = meta;
        this.states = new HashMap<Object, RegexPartitionState>();
    }

    public void removeState(Object partitionKey) {
        states.remove(partitionKey);
    }

    public RegexPartitionStateRepo copyForIterate()
    {
        RegexPartitionStateRepoGroup copy = new RegexPartitionStateRepoGroup(getter, meta);
        for (Map.Entry<Object, RegexPartitionState> entry : states.entrySet())
        {
            copy.states.put(entry.getKey(), new RegexPartitionState(entry.getValue().getRandomAccess(), entry.getKey(), meta.isHasInterval()));
        }
        return copy;
    }

    public void removeOld(EventBean[] oldData, boolean isEmpty, boolean[] found)
    {
        if (isEmpty)
        {
            if (getter == null)
            {
                // no "prev" used, clear all state
                states.clear();
            }
            else
            {
                for (Map.Entry<Object, RegexPartitionState> entry : states.entrySet())
                {
                    entry.getValue().getCurrentStates().clear();
                }
            }

            // clear "prev" state
            if (getter != null)
            {
                // we will need to remove event-by-event
                for (int i = 0; i < oldData.length; i++)
                {
                    RegexPartitionState partitionState = getState(oldData[i], true);
                    if (partitionState == null)
                    {
                        continue;
                    }
                    partitionState.removeEventFromPrev(oldData);
                }
            }

            return;
        }

        // we will need to remove event-by-event
        for (int i = 0; i < oldData.length; i++)
        {
            RegexPartitionState partitionState = getState(oldData[i], true);
            if (partitionState == null)
            {
                continue;
            }

            if (found[i])
            {
                boolean cleared = partitionState.removeEventFromState(oldData[i]);
                if (cleared)
                {
                    if (getter == null)
                    {
                        states.remove(partitionState.getOptionalKeys());
                    }
                }
            }

            partitionState.removeEventFromPrev(oldData[i]);
        }
    }
   
    public RegexPartitionState getState(Object key)
    {
        return states.get(key);
    }

    public RegexPartitionState getState(EventBean theEvent, boolean isCollect)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qRegExPartition(meta.getPartitionExpressionNodes()); }

        // collect unused states
        if ((isCollect) && (states.size() >= currentCollectionSize))
        {
            List<Object> removeList = new ArrayList<Object>();
            for (Map.Entry<Object, RegexPartitionState> entry : states.entrySet())
            {
                if ((entry.getValue().getCurrentStates().isEmpty()) &&
                    (entry.getValue().getRandomAccess() == null || entry.getValue().getRandomAccess().isEmpty()))
                {
                    removeList.add(entry.getKey());
                }
            }

            for (Object removeKey : removeList)
            {
                states.remove(removeKey);
            }

            if (removeList.size() < (currentCollectionSize / 5))
            {
                currentCollectionSize *= 2;
            }
        }

        Object key = getKeys(theEvent);
        
        RegexPartitionState state = states.get(key);
        if (state != null)
        {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegExPartition(true, state); }
            return state;
        }

        state = new RegexPartitionState(getter, new ArrayList<RegexNFAStateEntry>(), key, meta.isHasInterval());
        states.put(key, state);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aRegExPartition(false, state); }
        return state;
    }

    public void accept(EventRowRegexNFAViewServiceVisitor visitor) {
        visitor.visitPartitioned(states);
    }

    public boolean isPartitioned() {
        return true;
    }

    private Object getKeys(EventBean theEvent)
    {
        EventBean[] eventsPerStream = meta.getEventsPerStream();
        eventsPerStream[0] = theEvent;

        ExprEvaluator[] partitionExpressions = meta.getPartitionExpressions();
        if (partitionExpressions.length == 1) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprValue(meta.getPartitionExpressionNodes()[0], eventsPerStream);
                Object value = partitionExpressions[0].evaluate(eventsPerStream, true, meta.getExprEvaluatorContext());
                InstrumentationHelper.get().aExprValue(value);
                return value;
            }
            else {
                return partitionExpressions[0].evaluate(eventsPerStream, true, meta.getExprEvaluatorContext());
            }
        }

        Object[] keys = new Object[partitionExpressions.length];
        int count = 0;
        ExprEvaluatorContext exprEvaluatorContext = meta.getExprEvaluatorContext();
        for (ExprEvaluator node : partitionExpressions) {
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qExprValue(meta.getPartitionExpressionNodes()[count], eventsPerStream); }
            keys[count] = node.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aExprValue(keys[count]); }
            count++;
        }
        return new MultiKeyUntyped(keys);
    }
}