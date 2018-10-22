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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.onset;

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryUtil;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.StatementAgentInstanceFactoryOnTriggerBase;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnExprBaseViewResult;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewSimpleWProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.variable.core.VariableReadWritePackage;
import com.espertech.esper.common.internal.view.core.View;

import java.util.List;

public class StatementAgentInstanceFactoryOnTriggerSet extends StatementAgentInstanceFactoryOnTriggerBase {

    private VariableReadWritePackage variableReadWrite;
    private ResultSetProcessorFactoryProvider resultSetProcessorFactoryProvider;

    public void setVariableReadWrite(VariableReadWritePackage variableReadWrite) {
        this.variableReadWrite = variableReadWrite;
    }

    public void setResultSetProcessorFactoryProvider(ResultSetProcessorFactoryProvider resultSetProcessorFactoryProvider) {
        this.resultSetProcessorFactoryProvider = resultSetProcessorFactoryProvider;
    }

    public VariableReadWritePackage getVariableReadWrite() {
        return variableReadWrite;
    }

    public InfraOnExprBaseViewResult determineOnExprView(AgentInstanceContext agentInstanceContext, List<AgentInstanceStopCallback> stopCallbacks, boolean isRecoveringReslient) {
        OnSetVariableView view = new OnSetVariableView(this, agentInstanceContext);
        return new InfraOnExprBaseViewResult(view, null);
    }

    public View determineFinalOutputView(AgentInstanceContext agentInstanceContext, View onExprView) {
        // create result-processing
        Pair<ResultSetProcessor, AggregationService> pair = StatementAgentInstanceFactoryUtil.startResultSetAndAggregation(resultSetProcessorFactoryProvider, agentInstanceContext, false, null);
        OutputProcessViewSimpleWProcessor out = new OutputProcessViewSimpleWProcessor(agentInstanceContext, pair.getFirst());
        out.setParent(onExprView);
        onExprView.setChild(out);

        return out;
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }
}
