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

package com.espertech.esper.pattern.pool;

import com.espertech.esper.client.hook.ConditionPatternEngineSubexpressionMax;
import com.espertech.esper.pattern.EvalFollowedByNode;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class PatternSubexpressionPoolEngineSvc {
    private static final Log log = LogFactory.getLog(PatternSubexpressionPoolEngineSvc.class);

    private volatile long maxPoolCountConfigured;
    private final boolean preventStart;
    private final AtomicLong poolCount;
    private final Set<StatementEntry> patternContexts;

    public PatternSubexpressionPoolEngineSvc(long maxPoolCountConfigured, boolean preventStart) {
        this.maxPoolCountConfigured = maxPoolCountConfigured;
        this.preventStart = preventStart;
        this.poolCount = new AtomicLong();
        this.patternContexts = Collections.synchronizedSet(new HashSet<StatementEntry>());
    }

    public void setPatternMaxSubexpressions(Long maxSubexpressions) {
        if (maxSubexpressions == null) {
            maxPoolCountConfigured = -1;
        }
        else {
            maxPoolCountConfigured = maxSubexpressions;
        }
    }

    public void addPatternContext(String statementName, PatternSubexpressionPoolStmtHandler stmtCounts) {
        patternContexts.add(new StatementEntry(statementName, stmtCounts));
    }

    public void removeStatement(String name) {
        Set<StatementEntry> removed = new HashSet<StatementEntry>();
        for (StatementEntry context : patternContexts) {
            if (context.getStatementName().equals(name)) {
                removed.add(context);
            }
        }
        patternContexts.removeAll(removed);
    }

    public boolean tryIncreaseCount(EvalFollowedByNode evalFollowedByNode) {

        // test pool max
        long newMax = poolCount.incrementAndGet();
        if (newMax > maxPoolCountConfigured && maxPoolCountConfigured >= 0) {
            Map<String, Long> counts = getCounts();
            evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getExceptionHandlingService().handleCondition(new ConditionPatternEngineSubexpressionMax(maxPoolCountConfigured, counts), evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getEpStatementHandle());
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled() && (ExecutionPathDebugLog.isTimerDebugEnabled)))
            {
                PatternSubexpressionPoolStmtHandler stmtHandler = evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getPatternSubexpressionPoolSvc().getStmtHandler();
                String stmtName = evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getStatementName();
                log.debug(".tryIncreaseCount For statement '" + stmtName + "' pool count overflow at " + newMax + " statement count was " + stmtHandler.getCount() + " preventStart=" + preventStart);
            }

            if (preventStart) {
                poolCount.decrementAndGet();
                return false;
            }
            else {
                return true;
            }
        }
        if ((ExecutionPathDebugLog.isDebugEnabled) && log.isDebugEnabled())
        {
            PatternSubexpressionPoolStmtHandler stmtHandler = evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getPatternSubexpressionPoolSvc().getStmtHandler();
            String stmtName = evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getStatementName();
            log.debug(".tryIncreaseCount For statement '" + stmtName + "' pool count increases to " + newMax + " statement count was " + stmtHandler.getCount());
        }
        return true;
    }

    // Relevant for recovery of state
    public void forceIncreaseCount(EvalFollowedByNode evalFollowedByNode) {

        long newMax = poolCount.incrementAndGet();
        if ((ExecutionPathDebugLog.isDebugEnabled) && log.isDebugEnabled())
        {
            PatternSubexpressionPoolStmtHandler stmtHandler = evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getPatternSubexpressionPoolSvc().getStmtHandler();
            String stmtName = evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getStatementName();
            log.debug(".forceIncreaseCount For statement '" + stmtName + "' pool count increases to " + newMax + " statement count was " + stmtHandler.getCount());
        }
    }

    public void decreaseCount(EvalFollowedByNode evalFollowedByNode) {
        long newMax = poolCount.decrementAndGet();
        if ((ExecutionPathDebugLog.isDebugEnabled) && log.isDebugEnabled())
        {
            PatternSubexpressionPoolStmtHandler stmtHandler = evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getPatternSubexpressionPoolSvc().getStmtHandler();
            String stmtName = evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getStatementName();
            log.debug(".decreaseCount For statement '" + stmtName + "' pool count decreases to " + newMax + " statement count was " + stmtHandler.getCount());
        }
    }

    private Map<String,Long> getCounts() {
        Map<String,Long> counts = new HashMap<String,Long>();
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
        private final String statementName;
        private final PatternSubexpressionPoolStmtHandler stmtCounts;

        public StatementEntry(String statementName, PatternSubexpressionPoolStmtHandler stmtCounts) {
            this.statementName = statementName;
            this.stmtCounts = stmtCounts;
        }

        public String getStatementName() {
            return statementName;
        }

        public PatternSubexpressionPoolStmtHandler getStmtCounts() {
            return stmtCounts;
        }
    }
}
