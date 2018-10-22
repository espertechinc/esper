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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.core.WrapperEventType;

import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectEvalInsertWildcardWrapperNested extends SelectEvalBaseMap implements SelectExprProcessorForge {

    private final WrapperEventType innerWrapperType;

    public SelectEvalInsertWildcardWrapperNested(SelectExprForgeContext selectExprForgeContext, EventType resultEventType, WrapperEventType innerWrapperType) {
        super(selectExprForgeContext, resultEventType);
        this.innerWrapperType = innerWrapperType;
    }

    protected CodegenExpression processSpecificCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenExpression props, CodegenMethod methodNode, SelectExprProcessorCodegenSymbol selectEnv, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField innerType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(innerWrapperType, EPStatementInitServices.REF));
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        return staticMethod(this.getClass(), "wildcardNestedWrapper", arrayAtIndex(refEPS, constant(0)), innerType, resultEventType, eventBeanFactory, props);
    }

    public static EventBean wildcardNestedWrapper(EventBean event, EventType innerWrapperType, EventType outerWrapperType, EventBeanTypedEventFactory factory, Map<String, Object> props) {
        EventBean inner = factory.adapterForTypedWrapper(event, Collections.emptyMap(), innerWrapperType);
        return factory.adapterForTypedWrapper(inner, props, outerWrapperType);
    }
}
