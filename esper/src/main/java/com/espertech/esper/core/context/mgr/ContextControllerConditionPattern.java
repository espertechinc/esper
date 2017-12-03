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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.spec.ContextDetailConditionPattern;
import com.espertech.esper.epl.spec.PatternStreamSpecCompiled;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.filterspec.MatchedEventMapImpl;
import com.espertech.esper.filterspec.MatchedEventMapMeta;
import com.espertech.esper.pattern.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ContextControllerConditionPattern implements ContextControllerCondition, PatternMatchCallback {

    private final EPServicesContext servicesContext;
    private final AgentInstanceContext agentInstanceContext;
    private final ContextDetailConditionPattern endpointPatternSpec;
    private final ContextControllerConditionCallback callback;
    private final ContextInternalFilterAddendum filterAddendum;
    private final boolean isStartEndpoint;
    private final ContextStatePathKey contextStatePathKey;

    protected EvalRootState patternStopCallback;

    public ContextControllerConditionPattern(EPServicesContext servicesContext, AgentInstanceContext agentInstanceContext, ContextDetailConditionPattern endpointPatternSpec, ContextControllerConditionCallback callback, ContextInternalFilterAddendum filterAddendum, boolean startEndpoint, ContextStatePathKey contextStatePathKey) {
        this.servicesContext = servicesContext;
        this.agentInstanceContext = agentInstanceContext;
        this.endpointPatternSpec = endpointPatternSpec;
        this.callback = callback;
        this.filterAddendum = filterAddendum;
        this.isStartEndpoint = startEndpoint;
        this.contextStatePathKey = contextStatePathKey;
    }

    public void activate(EventBean optionalTriggeringEvent, MatchedEventMap priorMatches, long timeOffset, boolean isRecoveringReslient) {
        if (patternStopCallback != null) {
            patternStopCallback.stop();
        }

        PatternStreamSpecCompiled patternStreamSpec = endpointPatternSpec.getPatternCompiled();
        StatementContext stmtContext = agentInstanceContext.getStatementContext();

        EvalRootFactoryNode rootFactoryNode = servicesContext.getPatternNodeFactory().makeRootNode(patternStreamSpec.getEvalFactoryNode());
        int streamNum = isStartEndpoint ? contextStatePathKey.getSubPath() : -1 * contextStatePathKey.getSubPath();
        boolean allowResilient = contextStatePathKey.getLevel() == 1;
        PatternContext patternContext = stmtContext.getPatternContextFactory().createContext(stmtContext, streamNum, rootFactoryNode, new MatchedEventMapMeta(patternStreamSpec.getAllTags(), !patternStreamSpec.getArrayEventTypes().isEmpty()), allowResilient);

        PatternAgentInstanceContext patternAgentInstanceContext = stmtContext.getPatternContextFactory().createPatternAgentContext(patternContext, agentInstanceContext, false, filterAddendum == null ? null : filterAddendum.getFilterAddendum());
        EvalRootNode rootNode = EvalNodeUtil.makeRootNodeFromFactory(rootFactoryNode, patternAgentInstanceContext);

        if (priorMatches == null) {
            priorMatches = new MatchedEventMapImpl(patternContext.getMatchedEventMapMeta());
        }

        // capture any callbacks that may occur right after start
        ConditionPatternMatchCallback callback = new ConditionPatternMatchCallback(this);
        patternStopCallback = rootNode.start(callback, patternContext, priorMatches, isRecoveringReslient);
        callback.forwardCalls = true;

        if (agentInstanceContext.getStatementContext().getStatementExtensionServicesContext() != null && agentInstanceContext.getStatementContext().getStatementExtensionServicesContext().getStmtResources() != null) {
            agentInstanceContext.getStatementContext().getStatementExtensionServicesContext().getStmtResources().startContextPattern(patternStopCallback, isStartEndpoint, contextStatePathKey);
        }

        if (callback.isInvoked) {
            matchFound(Collections.<String, Object>emptyMap(), optionalTriggeringEvent);
        }
    }

    public void matchFound(Map<String, Object> matchEvent, EventBean optionalTriggeringEventPattern) {
        Map<String, Object> matchEventInclusive = null;
        if (endpointPatternSpec.isInclusive()) {
            if (matchEvent.size() < 2) {
                matchEventInclusive = matchEvent;
            } else {
                // need to reorder according to tag order
                LinkedHashMap<String, Object> ordered = new LinkedHashMap<String, Object>();
                for (String key : endpointPatternSpec.getPatternCompiled().getTaggedEventTypes().keySet()) {
                    ordered.put(key, matchEvent.get(key));
                }
                for (String key : endpointPatternSpec.getPatternCompiled().getArrayEventTypes().keySet()) {
                    ordered.put(key, matchEvent.get(key));
                }
                matchEventInclusive = ordered;
            }
        }
        callback.rangeNotification(matchEvent, this, null, matchEventInclusive, optionalTriggeringEventPattern, filterAddendum);
    }

    public void deactivate() {
        if (patternStopCallback != null) {
            patternStopCallback.stop();
            patternStopCallback = null;
            if (agentInstanceContext.getStatementContext().getStatementExtensionServicesContext() != null && agentInstanceContext.getStatementContext().getStatementExtensionServicesContext().getStmtResources() != null) {
                agentInstanceContext.getStatementContext().getStatementExtensionServicesContext().getStmtResources().stopContextPattern(isStartEndpoint, contextStatePathKey);
            }
        }
    }

    public boolean isRunning() {
        return patternStopCallback != null;
    }

    public Long getExpectedEndTime() {
        return null;
    }

    public boolean isImmediate() {
        return endpointPatternSpec.isImmediate();
    }

    public static class ConditionPatternMatchCallback implements PatternMatchCallback {
        private final ContextControllerConditionPattern condition;

        private boolean isInvoked;
        private boolean forwardCalls;

        public ConditionPatternMatchCallback(ContextControllerConditionPattern condition) {
            this.condition = condition;
        }

        public void matchFound(Map<String, Object> matchEvent, EventBean optionalTriggeringEvent) {
            isInvoked = true;
            if (forwardCalls) {
                condition.matchFound(matchEvent, optionalTriggeringEvent);
            }
        }

        public boolean isInvoked() {
            return isInvoked;
        }
    }
}
