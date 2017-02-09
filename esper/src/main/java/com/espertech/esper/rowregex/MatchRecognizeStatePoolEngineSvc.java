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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.hook.ConditionMatchRecognizeStatesMax;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MatchRecognizeStatePoolEngineSvc {
    private static final Logger log = LoggerFactory.getLogger(MatchRecognizeStatePoolEngineSvc.class);

    private volatile long maxPoolCountConfigured;
    private final boolean preventStart;
    private final AtomicLong poolCount;
    private final Set<StatementEntry> matchRecognizeContexts;

    public MatchRecognizeStatePoolEngineSvc(long maxPoolCountConfigured, boolean preventStart) {
        this.maxPoolCountConfigured = maxPoolCountConfigured;
        this.preventStart = preventStart;
        this.poolCount = new AtomicLong();
        this.matchRecognizeContexts = Collections.synchronizedSet(new HashSet<StatementEntry>());
    }

    public void setMatchRecognizeMaxStates(Long maxStates) {
        if (maxStates == null) {
            maxPoolCountConfigured = -1;
        } else {
            maxPoolCountConfigured = maxStates;
        }
    }

    public void addPatternContext(String statementName, MatchRecognizeStatePoolStmtHandler stmtCounts) {
        matchRecognizeContexts.add(new StatementEntry(statementName, stmtCounts));
    }

    public void removeStatement(String name) {
        // counts get reduced upon view stop
        Set<StatementEntry> removed = new HashSet<StatementEntry>();
        for (StatementEntry context : matchRecognizeContexts) {
            if (context.getStatementName().equals(name)) {
                removed.add(context);
            }
        }
        matchRecognizeContexts.removeAll(removed);
    }

    public boolean tryIncreaseCount(AgentInstanceContext agentInstanceContext) {

        // test pool max
        long newMax = poolCount.incrementAndGet();
        if (newMax > maxPoolCountConfigured && maxPoolCountConfigured >= 0) {
            Map<String, Long> counts = getCounts();
            agentInstanceContext.getStatementContext().getExceptionHandlingService().handleCondition(new ConditionMatchRecognizeStatesMax(maxPoolCountConfigured, counts), agentInstanceContext.getStatementContext().getEpStatementHandle());
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled() && (ExecutionPathDebugLog.isTimerDebugEnabled))) {
                MatchRecognizeStatePoolStmtHandler stmtHandler = agentInstanceContext.getStatementContext().getMatchRecognizeStatePoolStmtSvc().getStmtHandler();
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
            MatchRecognizeStatePoolStmtHandler stmtHandler = agentInstanceContext.getStatementContext().getMatchRecognizeStatePoolStmtSvc().getStmtHandler();
            String stmtName = agentInstanceContext.getStatementContext().getStatementName();
            log.debug(".tryIncreaseCount For statement '" + stmtName + "' pool count increases to " + newMax + " statement count was " + stmtHandler.getCount());
        }
        return true;
    }

    public void decreaseCount(AgentInstanceContext agentInstanceContext) {
        decreaseCount(agentInstanceContext, 1);
    }

    public void decreaseCount(AgentInstanceContext agentInstanceContext, int numRemoved) {
        long newMax = poolCount.addAndGet(-1 * numRemoved);
        if (newMax < 0) {
            poolCount.set(0);
        }
        logDecrease(agentInstanceContext, newMax);
    }

    private void logDecrease(AgentInstanceContext agentInstanceContext, long newMax) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && log.isDebugEnabled()) {
            MatchRecognizeStatePoolStmtHandler stmtHandler = agentInstanceContext.getStatementContext().getMatchRecognizeStatePoolStmtSvc().getStmtHandler();
            String stmtName = agentInstanceContext.getStatementContext().getStatementName();
            log.debug(".decreaseCount For statement '" + stmtName + "' pool count decreases to " + newMax + " statement count was " + stmtHandler.getCount());
        }
    }

    private Map<String, Long> getCounts() {
        Map<String, Long> counts = new HashMap<String, Long>();
        for (StatementEntry context : matchRecognizeContexts) {
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
        private final MatchRecognizeStatePoolStmtHandler stmtCounts;

        public StatementEntry(String statementName, MatchRecognizeStatePoolStmtHandler stmtCounts) {
            this.statementName = statementName;
            this.stmtCounts = stmtCounts;
        }

        public String getStatementName() {
            return statementName;
        }

        public MatchRecognizeStatePoolStmtHandler getStmtCounts() {
            return stmtCounts;
        }
    }
}
