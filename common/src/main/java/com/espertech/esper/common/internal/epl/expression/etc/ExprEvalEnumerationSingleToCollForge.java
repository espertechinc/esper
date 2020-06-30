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
package com.espertech.esper.common.internal.epl.expression.etc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.typable.SelectExprProcessorTypableForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEvalEnumerationSingleToCollForge implements ExprForge, SelectExprProcessorTypableForge {
    protected final ExprEnumerationForge enumerationForge;
    private final EventType targetType;

    public ExprEvalEnumerationSingleToCollForge(ExprEnumerationForge enumerationForge, EventType targetType) {
        this.enumerationForge = enumerationForge;
        this.targetType = targetType;
    }

    public ExprEvaluator getExprEvaluator() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPEARRAY, ExprEvalEnumerationSingleToCollForge.class, codegenClassScope);

        methodNode.getBlock()
                .declareVar(EventBean.EPTYPE, "event", enumerationForge.evaluateGetEventBeanCodegen(methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("event")
                .declareVar(EventBean.EPTYPEARRAY, "events", newArrayByLength(EventBean.EPTYPE, constant(1)))
                .assignArrayElement(ref("events"), constant(0), ref("event"))
                .methodReturn(ref("events"));
        return localMethod(methodNode);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public EPTypeClass getEvaluationType() {
        return EventBean.EPTYPEARRAY;
    }

    public EPTypeClass getUnderlyingEvaluationType() {
        return JavaClassHelper.getArrayType(targetType.getUnderlyingEPType());
    }

    public ExprNodeRenderable getForgeRenderable() {
        return enumerationForge.getForgeRenderable();
    }
}
