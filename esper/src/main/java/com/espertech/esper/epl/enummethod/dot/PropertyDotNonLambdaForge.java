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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventPropertyGetterSPI;

import java.io.StringWriter;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotNonLambdaForge implements ExprForge, ExprEvaluator, ExprNodeRenderable {

    private final int streamId;
    private final EventPropertyGetterSPI getter;
    private final Class returnType;

    public PropertyDotNonLambdaForge(int streamId, EventPropertyGetterSPI getter, Class returnType) {
        this.streamId = streamId;
        this.getter = getter;
        this.returnType = returnType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean event = eventsPerStream[streamId];
        if (event == null) {
            return null;
        }
        return getter.get(event);
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(getEvaluationType(), PropertyDotNonLambdaForge.class, codegenClassScope);

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        methodNode.getBlock()
                .declareVar(EventBean.class, "event", arrayAtIndex(refEPS, constant(streamId)))
                .ifRefNullReturnNull("event")
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(returnType, getter.eventBeanGetCodegen(ref("event"), methodNode, codegenClassScope)));
        return localMethod(methodNode);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SINGLE;
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
