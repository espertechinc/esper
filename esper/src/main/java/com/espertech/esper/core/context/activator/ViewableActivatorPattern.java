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
package com.espertech.esper.core.context.activator;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.pattern.*;
import com.espertech.esper.view.EventStream;
import com.espertech.esper.view.ZeroDepthStreamIterable;
import com.espertech.esper.view.ZeroDepthStreamNoIterate;

import java.util.Map;

public class ViewableActivatorPattern implements ViewableActivator {

    private final PatternContext patternContext;
    private final EvalRootFactoryNode rootFactoryNode;
    private final EventType eventType;
    private final boolean hasConsumingFilter;
    private final boolean suppressSameEventMatches;
    private final boolean discardPartialsOnMatch;
    private final boolean isCanIterate;

    protected ViewableActivatorPattern(PatternContext patternContext, EvalRootFactoryNode rootFactoryNode, EventType eventType, boolean hasConsumingFilter, boolean suppressSameEventMatches, boolean discardPartialsOnMatch, boolean isCanIterate) {
        this.patternContext = patternContext;
        this.rootFactoryNode = rootFactoryNode;
        this.eventType = eventType;
        this.hasConsumingFilter = hasConsumingFilter;
        this.suppressSameEventMatches = suppressSameEventMatches;
        this.discardPartialsOnMatch = discardPartialsOnMatch;
        this.isCanIterate = isCanIterate;
    }

    public ViewableActivationResult activate(AgentInstanceContext agentInstanceContext, boolean isSubselect, boolean isRecoveringResilient) {
        PatternAgentInstanceContext patternAgentInstanceContext = agentInstanceContext.getStatementContext().getPatternContextFactory().createPatternAgentContext(patternContext, agentInstanceContext, hasConsumingFilter, null);
        EvalRootNode rootNode = EvalNodeUtil.makeRootNodeFromFactory(rootFactoryNode, patternAgentInstanceContext);

        final EventStream sourceEventStream = isCanIterate ? new ZeroDepthStreamIterable(eventType) : new ZeroDepthStreamNoIterate(eventType);
        final StatementContext statementContext = patternContext.getStatementContext();
        final PatternMatchCallback callback = new PatternMatchCallback() {
            public void matchFound(Map<String, Object> matchEvent, EventBean optionalTriggeringEvent) {
                EventBean compositeEvent = statementContext.getEventAdapterService().adapterForTypedMap(matchEvent, eventType);
                sourceEventStream.insert(compositeEvent);
            }
        };

        EvalRootState rootState = rootNode.start(callback, patternContext, isRecoveringResilient);
        return new ViewableActivationResult(sourceEventStream, rootState, null, rootState, rootState, suppressSameEventMatches, discardPartialsOnMatch, null);
    }

    public EvalRootFactoryNode getRootFactoryNode() {
        return rootFactoryNode;
    }

    public PatternContext getPatternContext() {
        return patternContext;
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
