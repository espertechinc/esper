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
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForgeComplexityEnum;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprTypeofNodeForgeFragmentType extends ExprTypeofNodeForge implements ExprEvaluator {

    private final ExprTypeofNode parent;
    private final int streamId;
    private final EventPropertyGetterSPI getter;
    private final String fragmentType;

    public ExprTypeofNodeForgeFragmentType(ExprTypeofNode parent, int streamId, EventPropertyGetterSPI getter, String fragmentType) {
        this.parent = parent;
        this.streamId = streamId;
        this.getter = getter;
        this.fragmentType = fragmentType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprTypeof();
        }
        EventBean event = eventsPerStream[streamId];
        if (event == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprTypeof(null);
            }
            return null;
        }
        Object fragment = getter.getFragment(event);
        if (fragment == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprTypeof(null);
            }
            return null;
        }
        if (fragment instanceof EventBean) {
            EventBean bean = (EventBean) fragment;
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprTypeof(bean.getEventType().getName());
            }
            return bean.getEventType().getName();
        }
        if (fragment.getClass().isArray()) {
            String type = fragmentType + "[]";
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprTypeof(type);
            }
            return type;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprTypeof(null);
        }
        return null;
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        String method = context.addMethod(String.class, ExprTypeofNodeForgeFragmentType.class).add(params).begin()
                .declareVar(EventBean.class, "event", arrayAtIndex(params.passEPS(), constant(streamId)))
                .ifRefNullReturnNull("event")
                .declareVar(Object.class, "fragment", getter.eventBeanFragmentCodegen(ref("event"), context))
                .ifRefNullReturnNull("fragment")
                .ifInstanceOf("fragment", EventBean.class)
                .blockReturn(exprDotMethodChain(cast(EventBean.class, ref("fragment"))).add("getEventType").add("getName"))
                .ifCondition(exprDotMethodChain(ref("fragment")).add("getClass").add("isArray"))
                .blockReturn(constant(fragmentType + "[]"))
                .methodReturn(constantNull());
        return localMethodBuild(method).passAll(params).call();
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.SINGLE;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return parent;
    }
}
