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
package com.espertech.esper.common.internal.epl.agg.groupby;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.agg.core.AggSvcGroupByReclaimAgedEvalFunc;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.variable.core.Variable;
import com.espertech.esper.common.internal.epl.variable.core.VariableReader;

public class AggSvcGroupByReclaimAgedEvalFuncFactoryVariable implements AggSvcGroupByReclaimAgedEvalFuncFactory {
    public final static EPTypeClass EPTYPE = new EPTypeClass(AggSvcGroupByReclaimAgedEvalFuncFactoryVariable.class);

    private final Variable variable;

    public AggSvcGroupByReclaimAgedEvalFuncFactoryVariable(Variable variable) {
        this.variable = variable;
    }

    public AggSvcGroupByReclaimAgedEvalFunc make(ExprEvaluatorContext exprEvaluatorContext) {
        VariableReader reader = exprEvaluatorContext.getVariableManagementService().getReader(variable.getDeploymentId(), variable.getMetaData().getVariableName(), exprEvaluatorContext.getAgentInstanceId());
        return new AggSvcGroupByReclaimAgedEvalFuncVariable(reader);
    }
}
