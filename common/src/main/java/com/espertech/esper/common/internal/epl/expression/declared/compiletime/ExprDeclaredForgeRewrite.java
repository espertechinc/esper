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
package com.espertech.esper.common.internal.epl.expression.declared.compiletime;


import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEnumerationForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDeclaredForgeRewrite extends ExprDeclaredForgeBase {
    private final ExprEnumerationForge[] eventEnumerationForges;

    public ExprDeclaredForgeRewrite(ExprDeclaredNodeImpl parent, ExprForge innerForge, boolean isCache, ExprEnumerationForge[] eventEnumerationForges, boolean audit, String statementName) {
        super(parent, innerForge, isCache, audit, statementName);
        this.eventEnumerationForges = eventEnumerationForges;
    }

    public EventBean[] getEventsPerStreamRewritten(EventBean[] eps, boolean isNewData, ExprEvaluatorContext context) {

        // rewrite streams
        EventBean[] events = new EventBean[eventEnumerationForges.length];
        for (int i = 0; i < eventEnumerationForges.length; i++) {
            events[i] = eventEnumerationForges[i].getExprEvaluatorEnumeration().evaluateGetEventBean(eps, isNewData, context);
        }

        return events;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    protected CodegenExpression codegenEventsPerStreamRewritten(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(EventBean[].class, ExprDeclaredForgeRewrite.class, codegenClassScope);
        method.getBlock().declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, constant(eventEnumerationForges.length)));
        for (int i = 0; i < eventEnumerationForges.length; i++) {
            method.getBlock().assignArrayElement("events", constant(i), eventEnumerationForges[i].evaluateGetEventBeanCodegen(method, exprSymbol, codegenClassScope));
        }
        method.getBlock().methodReturn(ref("events"));
        return localMethod(method);
    }
}