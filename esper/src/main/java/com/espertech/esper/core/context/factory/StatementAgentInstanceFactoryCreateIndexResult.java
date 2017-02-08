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
package com.espertech.esper.core.context.factory;

import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.prev.ExprPreviousEvalStrategy;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.Viewable;

import java.util.Collections;

public class StatementAgentInstanceFactoryCreateIndexResult extends StatementAgentInstanceFactoryResult {

    public StatementAgentInstanceFactoryCreateIndexResult(Viewable finalView, StopCallback stopCallback, AgentInstanceContext agentInstanceContext) {
        super(finalView, stopCallback, agentInstanceContext, null,
                Collections.<ExprSubselectNode, SubSelectStrategyHolder>emptyMap(),
                Collections.<ExprPriorNode, ExprPriorEvalStrategy>emptyMap(),
                Collections.<ExprPreviousNode, ExprPreviousEvalStrategy>emptyMap(),
                null,
                Collections.<ExprTableAccessNode, ExprTableAccessEvalStrategy>emptyMap(),
                Collections.<StatementAgentInstancePreload>emptyList());
    }
}
