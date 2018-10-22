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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeRenderable;

import java.util.Map;

public class SelectExprProcessorTypableMapForge implements SelectExprProcessorTypableForge {
    protected final EventType mapType;
    protected final ExprForge innerForge;

    public SelectExprProcessorTypableMapForge(EventType mapType, ExprForge innerForge) {
        this.mapType = mapType;
        this.innerForge = innerForge;
    }

    public ExprEvaluator getExprEvaluator() {
        return new SelectExprProcessorTypableMapEval(this);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return SelectExprProcessorTypableMapEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Class getUnderlyingEvaluationType() {
        return Map.class;
    }

    public Class getEvaluationType() {
        return EventBean.class;
    }

    public ExprForge getInnerForge() {
        return innerForge;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return innerForge.getForgeRenderable();
    }

    public EventType getMapType() {
        return mapType;
    }
}
