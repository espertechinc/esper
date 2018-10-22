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
package com.espertech.esper.common.internal.epl.resultset.select.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.event.core.DecoratingEventBean;
import com.espertech.esper.common.internal.event.variant.VariantEventType;
import com.espertech.esper.common.internal.event.variant.VariantEventTypeUtil;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class SelectEvalInsertWildcardSSWrapperRevision extends SelectEvalBaseMap {

    private final VariantEventType variantEventType;

    public SelectEvalInsertWildcardSSWrapperRevision(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, VariantEventType variantEventType) {
        super(selectExprForgeContext, resultEventType);
        this.variantEventType = variantEventType;
    }

    protected CodegenExpression processSpecificCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenExpression props, CodegenMethod methodNode, SelectExprProcessorCodegenSymbol selectEnv, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField type = VariantEventTypeUtil.getField(variantEventType, codegenClassScope);
        CodegenExpression refEPS = exprSymbol.getAddEPS(methodNode);
        return staticMethod(SelectEvalInsertWildcardSSWrapperRevision.class, "selectExprInsertWildcardSSWrapRevision", refEPS, evaluators == null ? constant(0) : constant(evaluators.length), props, type);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param eventsPerStream  events
     * @param numEvaluators    num evals
     * @param props            props
     * @param variantEventType variant
     * @return bean
     */
    public static EventBean selectExprInsertWildcardSSWrapRevision(EventBean[] eventsPerStream, int numEvaluators, Map<String, Object> props, VariantEventType variantEventType) {
        DecoratingEventBean wrapper = (DecoratingEventBean) eventsPerStream[0];
        if (wrapper != null) {
            Map<String, Object> map = wrapper.getDecoratingProperties();
            if ((numEvaluators == 0) && (!map.isEmpty())) {
                // no action
            } else {
                props.putAll(map);
            }
        }

        EventBean theEvent = eventsPerStream[0];
        return variantEventType.getValueAddEventBean(theEvent);
    }
}
