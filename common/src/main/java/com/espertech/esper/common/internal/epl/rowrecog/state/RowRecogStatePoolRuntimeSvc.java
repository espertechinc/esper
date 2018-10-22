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
package com.espertech.esper.common.internal.epl.rowrecog.state;

import com.espertech.esper.common.client.hook.condition.ConditionMatchRecognizeStatesMax;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class RowRecogStatePoolRuntimeSvc {
    private volatile long maxPoolCountConfigured;
    private final boolean preventStart;
    private final AtomicLong poolCount;
    private final Set<StatementEntry> matchRecognizeContexts;

    public RowRecogStatePoolRuntimeSvc(long maxPoolCountConfigured, boolean preventStart) {
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

    public void addPatternContext(DeploymentIdNamePair statement, RowRecogStatePoolStmtHandler stmtCounts) {
        matchRecognizeContexts.add(new StatementEntry(statement, stmtCounts));
    }

    public void removeStatement(DeploymentIdNamePair statement) {
        // counts get reduced upon view stop
        Set<StatementEntry> removed = new HashSet<StatementEntry>();
        for (StatementEntry context : matchRecognizeContexts) {
            if (context.getStatement().equals(statement)) {
                removed.add(context);
            }
        }
        matchRecognizeContexts.removeAll(removed);
    }

    public boolean tryIncreaseCount(AgentInstanceContext agentInstanceContext) {

        // test pool max
        long newMax = poolCount.incrementAndGet();
        if (newMax > maxPoolCountConfigured && maxPoolCountConfigured >= 0) {
            Map<DeploymentIdNamePair, Long> counts = getCounts();
            agentInstanceContext.getStatementContext().getExceptionHandlingService().handleCondition(new ConditionMatchRecognizeStatesMax(maxPoolCountConfigured, counts), agentInstanceContext.getStatementContext());

            if (preventStart) {
                poolCount.decrementAndGet();
                return false;
            } else {
                return true;
            }
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
    }

    private Map<DeploymentIdNamePair, Long> getCounts() {
        Map<DeploymentIdNamePair, Long> counts = new HashMap<>();
        for (StatementEntry context : matchRecognizeContexts) {
            Long count = counts.get(context.getStatement());
            if (count == null) {
                count = 0L;
            }
            count += context.getStmtCounts().getCount();
            counts.put(context.getStatement(), count);
        }
        return counts;
    }

    public static class StatementEntry {
        private final DeploymentIdNamePair statement;
        private final RowRecogStatePoolStmtHandler stmtCounts;

        public StatementEntry(DeploymentIdNamePair statement, RowRecogStatePoolStmtHandler stmtCounts) {
            this.statement = statement;
            this.stmtCounts = stmtCounts;
        }

        public DeploymentIdNamePair getStatement() {
            return statement;
        }

        public RowRecogStatePoolStmtHandler getStmtCounts() {
            return stmtCounts;
        }
    }
}
