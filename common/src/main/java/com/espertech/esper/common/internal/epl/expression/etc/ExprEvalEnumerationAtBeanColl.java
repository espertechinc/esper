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
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.typable.SelectExprProcessorTypableForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEvalEnumerationAtBeanColl implements ExprForge, SelectExprProcessorTypableForge {
    protected final ExprEnumerationForge enumerationForge;
    private final EventType eventTypeColl;

    public ExprEvalEnumerationAtBeanColl(ExprEnumerationForge enumerationForge, EventType eventTypeColl) {
        this.enumerationForge = enumerationForge;
        this.eventTypeColl = eventTypeColl;
    }

    public ExprEvaluator getExprEvaluator() {
        throw new IllegalStateException("Evaluator not available");
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPEARRAY, this.getClass(), codegenClassScope);
        methodNode.getBlock()
                .declareVar(EPTypePremade.OBJECT.getEPType(), "result", enumerationForge.evaluateGetROCollectionEventsCodegen(methodNode, exprSymbol, codegenClassScope))
                .ifCondition(and(notEqualsNull(ref("result")), instanceOf(ref("result"), EPTypePremade.COLLECTION.getEPType())))
                .declareVar(EPTypeClassParameterized.from(Collection.class, EventBean.class), "events", cast(EPTypePremade.COLLECTION.getEPType(), ref("result")))
                .blockReturn(cast(EventBean.EPTYPEARRAY, exprDotMethod(ref("events"), "toArray", newArrayByLength(EventBean.EPTYPE, exprDotMethod(ref("events"), "size")))))
                .methodReturn(cast(EventBean.EPTYPEARRAY, ref("result")));
        return localMethod(methodNode);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public EPTypeClass getEvaluationType() {
        return EventBean.EPTYPEARRAY;
    }

    public EPTypeClass getUnderlyingEvaluationType() {
        return JavaClassHelper.getArrayType(eventTypeColl.getUnderlyingEPType());
    }

    public ExprNodeRenderable getForgeRenderable() {
        return enumerationForge.getForgeRenderable();
    }
}
