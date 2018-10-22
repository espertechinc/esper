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
package com.espertech.esper.common.internal.epl.historical.method.poll;

import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.script.core.ScriptEvaluator;

public class MethodTargetStrategyScript implements MethodTargetStrategy, MethodTargetStrategyFactory, StatementReadyCallback {

    private ScriptEvaluator scriptEvaluator;

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        // no action
    }

    public MethodTargetStrategy make(AgentInstanceContext agentInstanceContext) {
        return this;
    }

    public Object invoke(Object lookupValues, AgentInstanceContext agentInstanceContext) {
        return scriptEvaluator.evaluate(lookupValues, agentInstanceContext);
    }

    public String getPlan() {
        return this.getClass().getSimpleName();
    }

    public void setScriptEvaluator(ScriptEvaluator scriptEvaluator) {
        this.scriptEvaluator = scriptEvaluator;
    }
}
