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

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.EvalNode;

public class PatternSubexpressionPoolRuntimeSvcNoOp implements PatternSubexpressionPoolRuntimeSvc {
    public final static PatternSubexpressionPoolRuntimeSvcNoOp INSTANCE = new PatternSubexpressionPoolRuntimeSvcNoOp();

    private PatternSubexpressionPoolRuntimeSvcNoOp() {
    }

    public void addPatternContext(int statementId, String statementName, PatternSubexpressionPoolStmtHandler stmtCounts) {
    }

    public void removeStatement(int statementId) {
    }

    public void decreaseCount(EvalNode evalNode, AgentInstanceContext agentInstanceContext) {
    }

    public boolean tryIncreaseCount(EvalNode evalNode, AgentInstanceContext agentInstanceContext) {
        return false;
    }

    public void forceIncreaseCount(EvalNode evalNode, AgentInstanceContext agentInstanceContext) {
    }
}
