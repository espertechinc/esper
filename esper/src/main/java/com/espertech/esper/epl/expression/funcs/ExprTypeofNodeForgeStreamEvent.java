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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.event.vaevent.VariantEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the TYPEOF(a) function is an expression tree.
 */
public class ExprTypeofNodeForgeStreamEvent extends ExprTypeofNodeForge implements ExprEvaluator {

    private final ExprTypeofNode parent;
    private final int streamNum;

    public ExprTypeofNodeForgeStreamEvent(ExprTypeofNode parent, int streamNum) {
        this.parent = parent;
        this.streamNum = streamNum;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprTypeof();
        }
        EventBean event = eventsPerStream[streamNum];
        if (event == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprTypeof(null);
            }
            return null;
        }
        if (event instanceof VariantEvent) {
            String typeName = ((VariantEvent) event).getUnderlyingEventBean().getEventType().getName();
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprTypeof(typeName);
            }
            return typeName;
        }
        if (InstrumentationHelper.ENABLED) {
            String typeName = event.getEventType().getName();
            InstrumentationHelper.get().aExprTypeof(typeName);
            return typeName;
        }
        return event.getEventType().getName();
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(String.class, ExprTypeofNodeForgeStreamEvent.class).add(params).begin()
                .declareVar(EventBean.class, "event", arrayAtIndex(params.passEPS(), constant(streamNum)))
                .ifRefNullReturnNull("event")
                .ifInstanceOf("event", VariantEvent.class)
                .blockReturn(exprDotMethodChain(cast(VariantEvent.class, ref("event"))).add("getUnderlyingEventBean").add("getEventType").add("getName"))
                .methodReturn(exprDotMethodChain(ref("event")).add("getEventType").add("getName"));
        return localMethodBuild(method).passAll(params).call();
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SINGLE;
    }

    public ExprNode getForgeRenderable() {
        return parent;
    }
}
