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
package com.espertech.esper.common.internal.epl.resultset.select.typable;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorTypableMapEval implements ExprEvaluator {
    private final SelectExprProcessorTypableMapForge forge;

    public SelectExprProcessorTypableMapEval(SelectExprProcessorTypableMapForge forge) {
        this.forge = forge;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException("Evaluate not supported");
    }

    public static CodegenExpression codegen(SelectExprProcessorTypableMapForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression mapType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(forge.getMapType(), EPStatementInitServices.REF));
        CodegenExpression beanFactory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);

        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, SelectExprProcessorTypableMapEval.class, codegenClassScope);

        methodNode.getBlock()
                .declareVar(Map.class, "values", forge.innerForge.evaluateCodegen(Map.class, methodNode, exprSymbol, codegenClassScope))
                .declareVarNoInit(Map.class, "map")
                .ifRefNull("values")
                .assignRef("values", staticMethod(Collections.class, "emptyMap"))
                .blockEnd()
                .methodReturn(exprDotMethod(beanFactory, "adapterForTypedMap", ref("values"), mapType));
        return localMethod(methodNode);
    }

}
