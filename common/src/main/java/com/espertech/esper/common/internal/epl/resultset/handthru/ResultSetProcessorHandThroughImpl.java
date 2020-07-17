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
package com.espertech.esper.common.internal.epl.resultset.handthru;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.TransformEventIterator;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputHelperVisitor;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ResultSetProcessorHandThroughImpl implements ResultSetProcessor {
    final ResultSetProcessorHandThroughFactory factory;
    AgentInstanceContext agentInstanceContext;

    public ResultSetProcessorHandThroughImpl(ResultSetProcessorHandThroughFactory factory, AgentInstanceContext agentInstanceContext) {
        this.factory = factory;
        this.agentInstanceContext = agentInstanceContext;
    }

    public EventType getResultEventType() {
        return factory.getResultEventType();
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        EventBean[] selectOldEvents = null;
        if (factory.isRstream()) {
            selectOldEvents = ResultSetProcessorHandThroughUtil.getSelectEventsNoHavingHandThruView(factory.getSelectExprProcessor(), oldData, false, isSynthesize, agentInstanceContext);
        }
        EventBean[] selectNewEvents = ResultSetProcessorHandThroughUtil.getSelectEventsNoHavingHandThruView(factory.getSelectExprProcessor(), newData, true, isSynthesize, agentInstanceContext);
        return new UniformPair<>(selectNewEvents, selectOldEvents);
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, boolean isSynthesize) {
        EventBean[] selectOldEvents = null;
        if (factory.isRstream()) {
            selectOldEvents = ResultSetProcessorHandThroughUtil.getSelectEventsNoHavingHandThruJoin(factory.getSelectExprProcessor(), oldEvents, false, isSynthesize, agentInstanceContext);
        }
        EventBean[] selectNewEvents = ResultSetProcessorHandThroughUtil.getSelectEventsNoHavingHandThruJoin(factory.getSelectExprProcessor(), newEvents, true, isSynthesize, agentInstanceContext);
        return new UniformPair<>(selectNewEvents, selectOldEvents);
    }

    public Iterator<EventBean> getIterator(Viewable viewable) {
        return new TransformEventIterator(viewable.iterator(), new ResultSetProcessorHandtruTransform(this));
    }

    public Iterator<EventBean> getIterator(Set<MultiKeyArrayOfKeys<EventBean>> joinSet) {
        UniformPair<EventBean[]> result = this.processJoinResult(joinSet, Collections.emptySet(), true);
        return new ArrayEventIterator(result.getFirst());
    }

    public void clear() {
    }

    public void stop() {
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKeyArrayOfKeys<EventBean>>>> joinEventsSet, boolean generateSynthetic) {
        throw new UnsupportedOperationException();
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic) {
        throw new UnsupportedOperationException();
    }

    public void setAgentInstanceContext(AgentInstanceContext context) {
        agentInstanceContext = context;
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
        // not implemented
    }

    public void applyJoinResult(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents) {
        // not implemented
    }

    public void processOutputLimitedLastAllNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isSynthesize) {
        // not implemented
    }

    public void processOutputLimitedLastAllNonBufferedJoin(Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, boolean isGenerateSynthetic) {
        // not implemented
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedView(boolean isSynthesize) {
        throw new UnsupportedOperationException();
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastAllNonBufferedJoin(boolean isSynthesize) {
        throw new UnsupportedOperationException();
    }

    public void acceptHelperVisitor(ResultSetProcessorOutputHelperVisitor visitor) {
    }
}
