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
package com.espertech.esper.epl.datetime.interval;

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

public class ExprEvaluatorStreamDTPropFragment implements ExprForge, ExprEvaluator, ExprNodeRenderable {

    private final int streamId;
    private final EventPropertyGetterSPI getterFragment;
    private final EventPropertyGetterSPI getterTimestamp;

    public ExprEvaluatorStreamDTPropFragment(int streamId, EventPropertyGetterSPI getterFragment, EventPropertyGetterSPI getterTimestamp) {
        this.streamId = streamId;
        this.getterFragment = getterFragment;
        this.getterTimestamp = getterTimestamp;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean theEvent = eventsPerStream[streamId];
        if (theEvent == null) {
            return null;
        }
        Object event = getterFragment.getFragment(theEvent);
        if (!(event instanceof EventBean)) {
            return null;
        }
        return getterTimestamp.get((EventBean) event);
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Long.class, ExprEvaluatorStreamDTPropFragment.class, codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);

        methodNode.getBlock()
                .declareVar(EventBean.class, "theEvent", arrayAtIndex(refEPS, constant(streamId)))
                .ifRefNullReturnNull("theEvent")
                .declareVar(Object.class, "event", getterFragment.eventBeanFragmentCodegen(ref("theEvent"), methodNode, codegenClassScope))
                .ifCondition(not(instanceOf(ref("event"), EventBean.class)))
                .blockReturn(constantNull())
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(Long.class, getterTimestamp.eventBeanGetCodegen(cast(EventBean.class, ref("event")), methodNode, codegenClassScope)));
        return localMethod(methodNode);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SINGLE;
    }

    public Class getEvaluationType() {
        return Long.class;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
