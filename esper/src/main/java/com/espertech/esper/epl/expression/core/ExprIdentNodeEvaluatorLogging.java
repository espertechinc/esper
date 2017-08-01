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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.util.AuditPath;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprIdentNodeEvaluatorLogging extends ExprIdentNodeEvaluatorImpl {
    private final String engineURI;
    private final String propertyName;
    private final String statementName;

    public ExprIdentNodeEvaluatorLogging(int streamNum, EventPropertyGetterSPI propertyGetter, Class propertyType, ExprIdentNode identNode, String propertyName, String statementName, String engineURI) {
        super(streamNum, propertyGetter, propertyType, identNode);
        this.propertyName = propertyName;
        this.statementName = statementName;
        this.engineURI = engineURI;
    }

    @Override
    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object result = super.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (AuditPath.isInfoEnabled()) {
            AuditPath.auditLog(engineURI, statementName, AuditEnum.PROPERTY, propertyName + " value " + result);
        }
        return result;
    }

    @Override
    public CodegenExpression codegen(CodegenParamSetExprPremade params, CodegenContext context) {
        if (returnType == null) {
            return constantNull();
        }
        String method = context.addMethod(returnType, this.getClass()).add(params).begin()
                .declareVar(returnType, "result", super.codegen(params, context))
                .ifCondition(staticMethod(AuditPath.class, "isInfoEnabled"))
                .expression(staticMethod(AuditPath.class, "auditLog", constant(engineURI), constant(statementName), enumValue(AuditEnum.class, "PROPERTY"), op(constant(propertyName + " value "), "+", ref("result"))))
                .blockEnd()
                .methodReturn(ref("result"));
        return localMethodBuild(method).passAll(params).call();
    }
}
