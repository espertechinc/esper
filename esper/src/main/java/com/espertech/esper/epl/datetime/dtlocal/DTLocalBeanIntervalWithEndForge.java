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
package com.espertech.esper.epl.datetime.dtlocal;

import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.event.EventPropertyGetterSPI;

public class DTLocalBeanIntervalWithEndForge implements DTLocalForge {
    protected final EventPropertyGetterSPI getterStartTimestamp;
    protected final Class getterStartReturnType;
    protected final EventPropertyGetterSPI getterEndTimestamp;
    protected final Class getterEndReturnType;
    protected final DTLocalForgeIntervalComp inner;

    public DTLocalBeanIntervalWithEndForge(EventPropertyGetterSPI getterStartTimestamp, Class getterStartReturnType, EventPropertyGetterSPI getterEndTimestamp, Class getterEndReturnType, DTLocalForgeIntervalComp inner) {
        this.getterStartTimestamp = getterStartTimestamp;
        this.getterStartReturnType = getterStartReturnType;
        this.getterEndTimestamp = getterEndTimestamp;
        this.getterEndReturnType = getterEndReturnType;
        this.inner = inner;
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalBeanIntervalWithEndEval(getterStartTimestamp, getterEndTimestamp, inner.makeEvaluatorComp());
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenParamSetExprPremade params, CodegenContext context) {
        return DTLocalBeanIntervalWithEndEval.codegen(this, inner, params, context);
    }
}
