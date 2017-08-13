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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprDotMethodForgeNoDuckEvalUnderlying extends ExprDotMethodForgeNoDuckEvalPlain {
    private static final Logger log = LoggerFactory.getLogger(ExprDotMethodForgeNoDuckEvalUnderlying.class);

    public ExprDotMethodForgeNoDuckEvalUnderlying(ExprDotMethodForgeNoDuck forge, ExprEvaluator[] parameters) {
        super(forge, parameters);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }
        if (!(target instanceof EventBean)) {
            log.warn("Expected EventBean return value but received '" + target.getClass().getName() + "' for statement " + forge.getStatementName());
            return null;
        }
        EventBean bean = (EventBean) target;
        return super.evaluate(bean.getUnderlying(), eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegenUnderlying(ExprDotMethodForgeNoDuck forge, CodegenExpression inner, Class innerType, CodegenContext context, CodegenParamSetExprPremade params) {
        Class underlyingType = forge.getMethod().getDeclaringClass();
        CodegenMethodId method = context.addMethod(JavaClassHelper.getBoxedType(forge.getMethod().getReturnType()), ExprDotMethodForgeNoDuckEvalUnderlying.class).add(EventBean.class, "target").add(params).begin()
                .ifRefNullReturnNull("target")
                .declareVar(underlyingType, "underlying", cast(underlyingType, exprDotMethod(ref("target"), "getUnderlying")))
                .methodReturn(ExprDotMethodForgeNoDuckEvalPlain.codegenPlain(forge, ref("underlying"), innerType, context, params));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }
}
