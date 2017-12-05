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

import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.variable.OnSetVariableView;
import com.espertech.esper.epl.variable.OnSetVariableViewFactory;
import com.espertech.esper.epl.view.OutputProcessViewFactory;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.View;

import java.util.List;

public class StatementAgentInstanceFactoryOnTriggerSetVariable extends StatementAgentInstanceFactoryOnTriggerBase {
    private final OnSetVariableViewFactory onSetVariableViewFactory;
    private final ResultSetProcessorFactoryDesc outputResultSetProcessorPrototype;
    private final OutputProcessViewFactory outputProcessViewFactory;

    public StatementAgentInstanceFactoryOnTriggerSetVariable(StatementContext statementContext, StatementSpecCompiled statementSpec, EPServicesContext services, ViewableActivator activator, SubSelectStrategyCollection subSelectStrategyCollection, OnSetVariableViewFactory onSetVariableViewFactory, ResultSetProcessorFactoryDesc outputResultSetProcessorPrototype, OutputProcessViewFactory outputProcessViewFactory) {
        super(statementContext, statementSpec, services, activator, subSelectStrategyCollection);
        this.onSetVariableViewFactory = onSetVariableViewFactory;
        this.outputResultSetProcessorPrototype = outputResultSetProcessorPrototype;
        this.outputProcessViewFactory = outputProcessViewFactory;
    }

    public OnExprViewResult determineOnExprView(AgentInstanceContext agentInstanceContext, List<StopCallback> stopCallbacks, boolean isRecoveringReslient) {
        OnSetVariableView view = onSetVariableViewFactory.instantiate(agentInstanceContext);
        return new OnExprViewResult(view, null);
    }

    public View determineFinalOutputView(AgentInstanceContext agentInstanceContext, View onExprView) {
        ResultSetProcessor outputResultSetProcessor = outputResultSetProcessorPrototype.getResultSetProcessorFactory().instantiate(null, null, agentInstanceContext);
        View outputView = outputProcessViewFactory.makeView(outputResultSetProcessor, agentInstanceContext);
        onExprView.addView(outputView);
        return outputView;
    }
}
