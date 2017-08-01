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
package com.espertech.esper.epl.declexpr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprDeclaredForgeConstant implements ExprForge, ExprEvaluator {
    private final ExprDeclaredNodeImpl parent;
    private final Class returnType;
    private final ExpressionDeclItem prototype;
    private final Object value;
    private final boolean audit;
    private final String engineURI;
    private final String statementName;

    public ExprDeclaredForgeConstant(ExprDeclaredNodeImpl parent, Class returnType, ExpressionDeclItem prototype, Object value, boolean audit, String engineURI, String statementName) {
        this.parent = parent;
        this.returnType = returnType;
        this.prototype = prototype;
        this.value = value;
        this.audit = audit;
        this.engineURI = engineURI;
        this.statementName = statementName;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprDeclared(prototype);
            InstrumentationHelper.get().aExprDeclared(value);
        }
        return value;
    }

    public ExprEvaluator getExprEvaluator() {
        if (audit) {
            return (ExprEvaluator) ExprEvaluatorProxy.newInstance(engineURI, statementName, prototype.getName(), this);
        }
        return this;
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.NONE;
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        if (!audit) {
            return constant(value);
        }
        String method = context.addMethod(returnType, ExprDeclaredForgeConstant.class).add(params).begin()
                .ifCondition(staticMethod(AuditPath.class, "isInfoEnabled"))
                .expression(staticMethod(AuditPath.class, "auditLog", constant(engineURI), constant(statementName), enumValue(AuditEnum.class, "EXPRDEF"), op(constant(prototype.getName() + " result "), "+", constant(value))))
                .blockEnd()
                .methodReturn(constant(value));
        return localMethodBuild(method).passAll(params).call();
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return parent;
    }
}
