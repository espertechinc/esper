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
package com.espertech.esper.common.internal.context.activator;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.EvalNodeUtil;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.*;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.view.core.EventStream;
import com.espertech.esper.common.internal.view.core.ViewNoop;
import com.espertech.esper.common.internal.view.core.ZeroDepthStreamIterable;
import com.espertech.esper.common.internal.view.core.ZeroDepthStreamNoIterate;

import java.util.Map;

public class ViewableActivatorPattern implements ViewableActivator {

    protected PatternContext patternContext;
    protected EvalRootFactoryNode rootFactoryNode;
    protected EventType eventType;
    protected boolean hasConsumingFilter;
    protected boolean suppressSameEventMatches;
    protected boolean discardPartialsOnMatch;
    protected boolean isCanIterate;
    protected EventBeanTypedEventFactory eventBeanTypedEventFactory;

    public void setRootFactoryNode(EvalRootFactoryNode rootFactoryNode) {
        this.rootFactoryNode = rootFactoryNode;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setHasConsumingFilter(boolean hasConsumingFilter) {
        this.hasConsumingFilter = hasConsumingFilter;
    }

    public void setSuppressSameEventMatches(boolean suppressSameEventMatches) {
        this.suppressSameEventMatches = suppressSameEventMatches;
    }

    public void setDiscardPartialsOnMatch(boolean discardPartialsOnMatch) {
        this.discardPartialsOnMatch = discardPartialsOnMatch;
    }

    public void setCanIterate(boolean canIterate) {
        isCanIterate = canIterate;
    }

    public void setEventBeanTypedEventFactory(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public void setPatternContext(PatternContext patternContext) {
        this.patternContext = patternContext;
    }

    public ViewableActivationResult activate(AgentInstanceContext agentInstanceContext, boolean isSubselect, boolean isRecoveringResilient) {
        PatternAgentInstanceContext patternAgentInstanceContext = new PatternAgentInstanceContext(patternContext, agentInstanceContext, hasConsumingFilter, null);
        EvalRootNode rootNode = EvalNodeUtil.makeRootNodeFromFactory(rootFactoryNode, patternAgentInstanceContext);

        final EventStream sourceEventStream = isCanIterate ? new ZeroDepthStreamIterable(eventType) : new ZeroDepthStreamNoIterate(eventType);

        // we set a child now in case the start itself indicates results
        sourceEventStream.setChild(ViewNoop.INSTANCE);

        final PatternMatchCallback callback = new PatternMatchCallback() {
            public void matchFound(Map<String, Object> matchEvent, EventBean optionalTriggeringEvent) {
                EventBean compositeEvent = eventBeanTypedEventFactory.adapterForTypedMap(matchEvent, eventType);
                sourceEventStream.insert(compositeEvent);
            }
        };

        EvalRootState rootState = rootNode.start(callback, patternContext, isRecoveringResilient);

        return new ViewableActivationResult(sourceEventStream, services -> rootState.stop(), rootState, suppressSameEventMatches, discardPartialsOnMatch, rootState, null);
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean isHasConsumingFilter() {
        return hasConsumingFilter;
    }

    public boolean isSuppressSameEventMatches() {
        return suppressSameEventMatches;
    }

    public boolean isDiscardPartialsOnMatch() {
        return discardPartialsOnMatch;
    }

    public boolean isCanIterate() {
        return isCanIterate;
    }
}
