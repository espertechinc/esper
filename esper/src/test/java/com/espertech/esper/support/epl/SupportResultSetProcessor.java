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

package com.espertech.esper.support.epl;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.core.ResultSetProcessor;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.event.SupportEventTypeFactory;
import com.espertech.esper.view.Viewable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SupportResultSetProcessor implements ResultSetProcessor
{
    public ResultSetProcessor copy(AgentInstanceContext agentInstanceContext) {
        return null;
    }

    public EventType getResultEventType()
    {
        return SupportEventTypeFactory.createBeanType(SupportBean.class);
    }

    public UniformPair<EventBean[]> processViewResult(EventBean[] newData, EventBean[] oldData, boolean isSynthesize)
    {
        return new UniformPair<EventBean[]>(newData, oldData);
    }

    public UniformPair<EventBean[]> processJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isSynthesize)
    {
        return new UniformPair<EventBean[]>(newEvents.iterator().next().getArray(), oldEvents.iterator().next().getArray());
    }

    public Iterator<EventBean> getIterator(Viewable parent)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterator<EventBean> getIterator(Set<MultiKey<EventBean>> joinSet)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void clear()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasAggregation() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setAgentInstanceContext(AgentInstanceContext context) {
    }

    public void applyViewResult(EventBean[] newData, EventBean[] oldData) {
    }

    public void applyJoinResult(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents) {
    }

    public void processOutputLimitedLastNonBufferedView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
    }

    public void processOutputLimitedLastNonBufferedJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic) {
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastNonBufferedView(boolean isSynthesize) {
        return null;
    }

    public UniformPair<EventBean[]> continueOutputLimitedLastNonBufferedJoin(boolean isSynthesize) {
        return null;
    }
}
