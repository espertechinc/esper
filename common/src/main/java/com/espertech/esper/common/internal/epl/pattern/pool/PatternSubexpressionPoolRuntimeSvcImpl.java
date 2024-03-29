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
package com.espertech.esper.common.internal.epl.pattern.pool;

import com.espertech.esper.common.client.hook.condition.ConditionPatternRuntimeSubexpressionMax;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.EvalNode;
import com.espertech.esper.common.internal.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class PatternSubexpressionPoolRuntimeSvcImpl implements PatternSubexpressionPoolRuntimeSvc {
    private static final Logger log = LoggerFactory.getLogger(PatternSubexpressionPoolRuntimeSvc.class);

    private final long maxPoolCountConfigured;
    private final boolean preventStart;
    private final AtomicLong poolCount;
    private final Set<StatementEntry> patternContexts;

    public PatternSubexpressionPoolRuntimeSvcImpl(long maxPoolCountConfigured, boolean preventStart) {
        this.maxPoolCountConfigured = maxPoolCountConfigured;
        this.preventStart = preventStart;
        this.poolCount = new AtomicLong();
        this.patternContexts = Collections.synchronizedSet(new HashSet<StatementEntry>());
    }

    public void addPatternContext(int statementId, String statementName, PatternSubexpressionPoolStmtHandler stmtCounts) {
        patternContexts.add(new StatementEntry(statementId, statementName, stmtCounts));
    }

    public void removeStatement(int statementId) {
        Set<StatementEntry> removed = new HashSet<StatementEntry>();
        for (StatementEntry context : patternContexts) {
            if (context.getStatementId() == statementId) {
                removed.add(context);
            }
        }
        patternContexts.removeAll(removed);
    }

    public boolean tryIncreaseCount(EvalNode evalNode, AgentInstanceContext agentInstanceContext) {

        // test pool max
        long newMax = poolCount.incrementAndGet();
        if (newMax > maxPoolCountConfigured && maxPoolCountConfigured >= 0) {
            Map<String, Long> counts = getCounts();
            agentInstanceContext.getStatementContext().getExceptionHandlingService().handleCondition(new ConditionPatternRuntimeSubexpressionMax(maxPoolCountConfigured, counts), agentInstanceContext.getStatementContext());
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled() && (ExecutionPathDebugLog.isTimerDebugEnabled))) {
                PatternSubexpressionPoolStmtHandler stmtHandler = agentInstanceContext.getStatementContext().getPatternSubexpressionPoolSvc().getStmtHandler();
                String stmtName = agentInstanceContext.getStatementContext().getStatementName();
                log.debug(".tryIncreaseCount For statement '" + stmtName + "' pool count overflow at " + newMax + " statement count was " + stmtHandler.getCount() + " preventStart=" + preventStart);
            }

            if (preventStart) {
                poolCount.decrementAndGet();
                return false;
            } else {
                return true;
            }
        }
        if ((ExecutionPathDebugLog.isDebugEnabled) && log.isDebugEnabled()) {
            PatternSubexpressionPoolStmtHandler stmtHandler = agentInstanceContext.getStatementContext().getPatternSubexpressionPoolSvc().getStmtHandler();
            String stmtName = agentInstanceContext.getStatementContext().getStatementName();
            log.debug(".tryIncreaseCount For statement '" + stmtName + "' pool count increases to " + newMax + " statement count was " + stmtHandler.getCount());
        }
        return true;
    }

    // Relevant for recovery of state
    public void forceIncreaseCount(EvalNode evalNode, AgentInstanceContext agentInstanceContext) {

        long newMax = poolCount.incrementAndGet();
        if ((ExecutionPathDebugLog.isDebugEnabled) && log.isDebugEnabled()) {
            PatternSubexpressionPoolStmtHandler stmtHandler = agentInstanceContext.getStatementContext().getPatternSubexpressionPoolSvc().getStmtHandler();
            String stmtName = agentInstanceContext.getStatementContext().getStatementName();
            log.debug(".forceIncreaseCount For statement '" + stmtName + "' pool count increases to " + newMax + " statement count was " + stmtHandler.getCount());
        }
    }

    public void decreaseCount(EvalNode evalNode, AgentInstanceContext agentInstanceContext) {
        long newMax = poolCount.decrementAndGet();
        if ((ExecutionPathDebugLog.isDebugEnabled) && log.isDebugEnabled()) {
            PatternSubexpressionPoolStmtHandler stmtHandler = agentInstanceContext.getStatementContext().getPatternSubexpressionPoolSvc().getStmtHandler();
            String stmtName = agentInstanceContext.getStatementContext().getStatementName();
            log.debug(".decreaseCount For statement '" + stmtName + "' pool count decreases to " + newMax + " statement count was " + stmtHandler.getCount());
        }
    }

    private Map<String, Long> getCounts() {
        Map<String, Long> counts = new HashMap<String, Long>();
        for (StatementEntry context : patternContexts) {
            Long count = counts.get(context.getStatementName());
            if (count == null) {
                count = 0L;
            }
            count += context.getStmtCounts().getCount();
            counts.put(context.getStatementName(), count);
        }
        return counts;
    }

    public static class StatementEntry {
        private final int statementId;
        private final String statementName;
        private final PatternSubexpressionPoolStmtHandler stmtCounts;

        public StatementEntry(int statementId, String statementName, PatternSubexpressionPoolStmtHandler stmtCounts) {
            this.statementId = statementId;
            this.statementName = statementName;
            this.stmtCounts = stmtCounts;
        }

        public int getStatementId() {
            return statementId;
        }

        public String getStatementName() {
            return statementName;
        }

        public PatternSubexpressionPoolStmtHandler getStmtCounts() {
            return stmtCounts;
        }
    }
}
