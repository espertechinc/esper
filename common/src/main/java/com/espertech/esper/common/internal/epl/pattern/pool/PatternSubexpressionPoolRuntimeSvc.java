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

public interface PatternSubexpressionPoolRuntimeSvc {
    void addPatternContext(int statementId, String statementName, PatternSubexpressionPoolStmtHandler stmtCounts);

    void removeStatement(int statementId);

    void decreaseCount(EvalNode evalNode, AgentInstanceContext agentInstanceContext);

    boolean tryIncreaseCount(EvalNode evalNode, AgentInstanceContext agentInstanceContext);

    void forceIncreaseCount(EvalNode evalNode, AgentInstanceContext agentInstanceContext);
}
