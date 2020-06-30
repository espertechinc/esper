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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEvalStreamInsertNamedWindow implements ExprForge, ExprEvaluator, ExprNodeRenderable {
    private final int streamNum;
    private final EventType namedWindowAsType;
    private final EPTypeClass returnType;

    public ExprEvalStreamInsertNamedWindow(int streamNum, EventType namedWindowAsType, EPTypeClass returnType) {
        this.streamNum = streamNum;
        this.namedWindowAsType = namedWindowAsType;
        this.returnType = returnType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException("Cannot evaluate at runtime");
    }


    public int getStreamNum() {
        return streamNum;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField eventSvc = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField namedWindowType = codegenClassScope.addFieldUnshared(true, EventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(namedWindowAsType, EPStatementInitServices.REF));
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, ExprEvalStreamInsertNamedWindow.class, codegenClassScope);

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);

        String method = EventTypeUtility.getAdapterForMethodName(namedWindowAsType);
        methodNode.getBlock()
                .declareVar(EventBean.EPTYPE, "event", arrayAtIndex(refEPS, constant(streamNum)))
                .ifRefNullReturnNull("event")
                .methodReturn(exprDotMethod(eventSvc, method, cast(namedWindowAsType.getUnderlyingEPType(), exprDotUnderlying(ref("event"))), namedWindowType));
        return localMethod(methodNode);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public EPTypeClass getEvaluationType() {
        return returnType;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence, ExprNodeRenderableFlags flags) {
        writer.append(this.getClass().getSimpleName());
    }
}
