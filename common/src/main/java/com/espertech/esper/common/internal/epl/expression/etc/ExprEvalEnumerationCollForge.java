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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.typable.SelectExprProcessorTypableForge;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEvalEnumerationCollForge implements ExprForge, SelectExprProcessorTypableForge {
    protected final ExprEnumerationForge enumerationForge;
    private final EventType targetType;
    private final boolean firstRowOnly;

    public ExprEvalEnumerationCollForge(ExprEnumerationForge enumerationForge, EventType targetType, boolean firstRowOnly) {
        this.enumerationForge = enumerationForge;
        this.targetType = targetType;
        this.firstRowOnly = firstRowOnly;
    }

    public ExprEvaluator getExprEvaluator() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (firstRowOnly) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, ExprEvalEnumerationCollForge.class, codegenClassScope);
            methodNode.getBlock()
                    .declareVar(Collection.class, EventBean.class, "events", enumerationForge.evaluateGetROCollectionEventsCodegen(methodNode, exprSymbol, codegenClassScope))
                    .ifRefNullReturnNull("events")
                    .ifCondition(equalsIdentity(exprDotMethod(ref("events"), "size"), constant(0)))
                    .blockReturn(constantNull())
                    .methodReturn(staticMethod(EventBeanUtility.class, "getNonemptyFirstEvent", ref("events")));
            return localMethod(methodNode);
        }

        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean[].class, ExprEvalEnumerationCollForge.class, codegenClassScope);
        methodNode.getBlock()
                .declareVar(Collection.class, EventBean.class, "events", enumerationForge.evaluateGetROCollectionEventsCodegen(methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("events")
                .methodReturn(cast(EventBean[].class, exprDotMethod(ref("events"), "toArray", newArrayByLength(EventBean.class, exprDotMethod(ref("events"), "size")))));
        return localMethod(methodNode);
    }

    public Class getUnderlyingEvaluationType() {
        if (firstRowOnly) {
            return targetType.getUnderlyingType();
        }
        return JavaClassHelper.getArrayType(targetType.getUnderlyingType());
    }

    public Class getEvaluationType() {
        if (firstRowOnly) {
            return EventBean.class;
        }
        return EventBean[].class;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return enumerationForge.getForgeRenderable();
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
