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
package com.espertech.esper.common.internal.epl.resultset.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.util.StopCallback;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Processor for result sets coming from 2 sources. First, out of a simple view (no join).
 * And second, out of a join of event streams. The processor must apply the select-clause, grou-by-clause and having-clauses
 * as supplied. It must state what the event type of the result rows is.
 */
public interface ResultSetProcessor extends StopCallback {
    /**
     * Returns the event type of processed results.
     *
     * @return event type of the resulting events posted by the processor.
     */
    public EventType getResultEventType();

    /**
     * For use by views posting their result, process the event rows that are entered and removed (new and old events).
     * Processes according to select-clauses, group-by clauses and having-clauses and returns new events and
     * old events as specified.
     *
     * @param newData      - new events posted by view
     * @param oldData      - old events posted by view
     * @param isSynthesize - set to true to indicate that synthetic events are required for an iterator result set
     * @return pair of new events and old events
     */
    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize);

    /**
     * For use by joins posting their result, process the event rows that are entered and removed (new and old events).
     * Processes according to select-clauses, group-by clauses and having-clauses and returns new events and
     * old events as specified.
     *
     * @param newEvents    - new events posted by join
     * @param oldEvents    - old events posted by join
     * @param isSynthesize - set to true to indicate that synthetic events are required for an iterator result set
     * @return pair of new events and old events
     */
    public UniformPair<EventBean[]> processJoinResult(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, boolean isSynthesize);

    /**
     * Returns the iterator implementing the group-by and aggregation and order-by logic
     * specific to each case of use of these construct.
     *
     * @param parent is the parent view iterator
     * @return event iterator
     */
    public Iterator<EventBean> getIterator(Viewable parent);

    /**
     * Returns the iterator for iterating over a join-result.
     *
     * @param joinSet is the join result set
     * @return iterator over join results
     */
    public Iterator<EventBean> getIterator(Set<MultiKeyArrayOfKeys<EventBean>> joinSet);

    /**
     * Clear out current state.
     */
    public void clear();

    /**
     * Processes batched events in case of output-rate limiting.
     *
     * @param joinEventsSet     the join results
     * @param generateSynthetic flag to indicate whether synthetic events must be generated
     * @return results for dispatch
     */
    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>>> joinEventsSet, boolean generateSynthetic);

    /**
     * Processes batched events in case of output-rate limiting.
     *
     * @param viewEventsList    the view results
     * @param generateSynthetic flag to indicate whether synthetic events must be generated
     * @return results for dispatch
     */
    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic);

    public void setAgentInstanceContext(AgentInstanceContext context);

    public void applyViewResult(EventBean[] newData, EventBean[] oldData);

    public void applyJoinResult(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents);

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic);

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, boolean isGenerateSynthetic);

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize);

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedJoin(boolean isSynthesize);

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor);
}
