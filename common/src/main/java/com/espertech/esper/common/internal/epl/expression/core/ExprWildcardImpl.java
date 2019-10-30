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
package com.espertech.esper.common.internal.epl.expression.core;


import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.type.WildcardParameter;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.enumValue;

/**
 * Expression for use within crontab to specify a wildcard.
 */
public class ExprWildcardImpl extends ExprNodeBase implements ExprForge, ExprEvaluator, ExprWildcard {

    private EventType eventType;

    public ExprWildcardImpl() {
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("*");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public boolean isConstantResult() {
        return true;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return node instanceof ExprWildcardImpl;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (validationContext.getStreamTypeService().getEventTypes().length > 0) {
            eventType = validationContext.getStreamTypeService().getEventTypes()[0];
        }
        return null;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return WildcardParameter.INSTANCE;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return enumValue(WildcardParameter.class, "INSTANCE");
    }

    public Class getEvaluationType() {
        return WildcardParameter.class;
    }

    public ExprEnumerationForgeDesc getEnumerationForge(StreamTypeService streamTypeService, ContextCompileTimeDescriptor contextDescriptor) {
        if (eventType == null) {
            return null;
        }
        if (streamTypeService.getEventTypes().length > 1) {
            return null;
        }
        return new ExprEnumerationForgeDesc(new ExprStreamUnderlyingNodeEnumerationForge("*", 0, eventType),
            streamTypeService.getIStreamOnly()[0],
            0);
    }
}
