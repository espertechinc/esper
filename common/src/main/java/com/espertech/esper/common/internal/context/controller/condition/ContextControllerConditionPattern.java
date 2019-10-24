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
package com.espertech.esper.common.internal.context.controller.condition;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.compile.stage2.EvalNodeUtil;
import com.espertech.esper.common.internal.context.controller.core.ContextController;
import com.espertech.esper.common.internal.context.mgr.ContextManagerUtil;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootState;
import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.PatternMatchCallback;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filterspec.MatchedEventMapImpl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class ContextControllerConditionPattern implements ContextControllerConditionNonHA, PatternMatchCallback {

    private final IntSeqKey conditionPath;
    private final Object[] partitionKeys;
    private final ContextConditionDescriptorPattern pattern;
    private final ContextControllerConditionCallback callback;
    private final ContextController controller;

    protected EvalRootState patternStopCallback;

    public ContextControllerConditionPattern(IntSeqKey conditionPath, Object[] partitionKeys, ContextConditionDescriptorPattern pattern, ContextControllerConditionCallback callback, ContextController controller) {
        this.conditionPath = conditionPath;
        this.partitionKeys = partitionKeys;
        this.pattern = pattern;
        this.callback = callback;
        this.controller = controller;
    }

    public boolean activate(EventBean optionalTriggeringEvent, ContextControllerEndConditionMatchEventProvider endConditionMatchEventProvider, Map<String, Object> optionalTriggeringPattern) {
        if (patternStopCallback != null) {
            patternStopCallback.stop();
        }

        AgentInstanceContext agentInstanceContext = controller.getRealization().getAgentInstanceContextCreate();
        Function<FilterSpecActivatable, FilterValueSetParam[][]> contextAddendumFunction = filter ->
                ContextManagerUtil.computeAddendumNonStmt(partitionKeys, filter, controller.getRealization());
        PatternAgentInstanceContext patternAgentInstanceContext = new PatternAgentInstanceContext(pattern.getPatternContext(), agentInstanceContext, false, contextAddendumFunction);
        EvalRootNode rootNode = EvalNodeUtil.makeRootNodeFromFactory(pattern.getPattern(), patternAgentInstanceContext);

        MatchedEventMapImpl matchedEventMap = new MatchedEventMapImpl(pattern.getPatternContext().getMatchedEventMapMeta());
        if (optionalTriggeringEvent != null && endConditionMatchEventProvider != null) {
            endConditionMatchEventProvider.populateEndConditionFromTrigger(matchedEventMap, optionalTriggeringEvent);
        }
        if (optionalTriggeringPattern != null && endConditionMatchEventProvider != null) {
            endConditionMatchEventProvider.populateEndConditionFromTrigger(matchedEventMap, optionalTriggeringPattern);
        }

        // capture any callbacks that may occur right after start
        ConditionPatternMatchCallback callback = new ConditionPatternMatchCallback(this);
        patternStopCallback = rootNode.start(callback, pattern.getPatternContext(), matchedEventMap, false);
        callback.forwardCalls = true;

        if (callback.isInvoked) {
            matchFound(Collections.<String, Object>emptyMap(), optionalTriggeringEvent);
        }
        return false;
    }

    public void deactivate() {
        if (patternStopCallback == null) {
            return;
        }
        patternStopCallback.stop();
        patternStopCallback = null;
    }

    public void matchFound(Map<String, Object> matchEvent, EventBean optionalTriggeringEvent) {
        Map<String, Object> matchEventInclusive = null;
        if (pattern.isInclusive()) {
            if (matchEvent.size() < 2) {
                matchEventInclusive = matchEvent;
            } else {
                // need to reorder according to tag order
                LinkedHashMap<String, Object> ordered = new LinkedHashMap<>();
                for (String key : pattern.getTaggedEvents()) {
                    ordered.put(key, matchEvent.get(key));
                }
                for (String key : pattern.getArrayEvents()) {
                    ordered.put(key, matchEvent.get(key));
                }
                matchEventInclusive = ordered;
            }
        }
        callback.rangeNotification(conditionPath, this, null, matchEvent, optionalTriggeringEvent, matchEventInclusive);
    }

    public boolean isImmediate() {
        return pattern.isImmediate();
    }

    public boolean isRunning() {
        return patternStopCallback != null;
    }

    public Long getExpectedEndTime() {
        return null;
    }

    public ContextConditionDescriptor getDescriptor() {
        return pattern;
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
