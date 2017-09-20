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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.ExpressionDeclItem;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.AuditPath;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.util.AuditPath.METHOD_AUDITLOG;

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

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (!audit) {
            return constant(value);
        }
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(returnType, ExprDeclaredForgeConstant.class, codegenClassScope);

        methodNode.getBlock()
                .ifCondition(staticMethod(AuditPath.class, "isInfoEnabled"))
                .staticMethod(AuditPath.class, METHOD_AUDITLOG, constant(engineURI), constant(statementName), enumValue(AuditEnum.class, "EXPRDEF"), op(constant(prototype.getName() + " result "), "+", constant(value)))
                .blockEnd()
                .methodReturn(constant(value));
        return localMethod(methodNode);
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return parent;
    }
}
