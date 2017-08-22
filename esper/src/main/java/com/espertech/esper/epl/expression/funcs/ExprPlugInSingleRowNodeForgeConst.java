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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.lang.reflect.Method;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.noop;

public class ExprPlugInSingleRowNodeForgeConst extends ExprPlugInSingleRowNodeForge implements ExprEvaluator {

    private final Object constantResult;
    private final Method method;

    public ExprPlugInSingleRowNodeForgeConst(ExprPlugInSingleRowNode parent, Object constantResult, Method method) {
        super(parent, true);
        this.constantResult = constantResult;
        this.method = method;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return method.getReturnType();
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprPlugInSingleRow(method);
            InstrumentationHelper.get().aExprPlugInSingleRow(constantResult);
        }
        return constantResult;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (getEvaluationType() == void.class) {
            return noop();
        }
        return constant(constantResult);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.NONE;
    }
}
