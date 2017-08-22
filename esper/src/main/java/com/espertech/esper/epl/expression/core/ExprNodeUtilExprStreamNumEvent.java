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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;

import java.io.StringWriter;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.arrayAtIndex;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;

public class ExprNodeUtilExprStreamNumEvent implements ExprForge, ExprEvaluator, ExprNodeRenderable {
    private final int streamNum;

    public ExprNodeUtilExprStreamNumEvent(int streamNum) {
        this.streamNum = streamNum;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return eventsPerStream[streamNum];
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(codegenMethodScope);
        return arrayAtIndex(refEPS, constant(streamNum));
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.NONE;
    }

    public Class getEvaluationType() {
        return EventBean.class;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(ExprNodeUtilExprStreamNumEvent.class.getSimpleName());
    }
}
