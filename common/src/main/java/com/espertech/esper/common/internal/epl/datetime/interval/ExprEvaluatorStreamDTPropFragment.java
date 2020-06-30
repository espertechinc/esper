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
package com.espertech.esper.common.internal.epl.datetime.interval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEvaluatorStreamDTPropFragment implements ExprForge, ExprEvaluator, ExprNodeRenderable {

    private final int streamId;
    private final EventPropertyGetterSPI getterFragment;
    private final EventPropertyGetterSPI getterTimestamp;

    public ExprEvaluatorStreamDTPropFragment(int streamId, EventPropertyGetterSPI getterFragment, EventPropertyGetterSPI getterTimestamp) {
        this.streamId = streamId;
        this.getterFragment = getterFragment;
        this.getterTimestamp = getterTimestamp;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean theEvent = eventsPerStream[streamId];
        if (theEvent == null) {
            return null;
        }
        Object event = getterFragment.getFragment(theEvent);
        if (!(event instanceof EventBean)) {
            return null;
        }
        return getterTimestamp.get((EventBean) event);
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.LONGBOXED.getEPType(), ExprEvaluatorStreamDTPropFragment.class, codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);

        methodNode.getBlock()
                .declareVar(EventBean.EPTYPE, "theEvent", arrayAtIndex(refEPS, constant(streamId)))
                .ifRefNullReturnNull("theEvent")
                .declareVar(EPTypePremade.OBJECT.getEPType(), "event", getterFragment.eventBeanFragmentCodegen(ref("theEvent"), methodNode, codegenClassScope))
                .ifCondition(not(instanceOf(ref("event"), EventBean.EPTYPE)))
                .blockReturn(constantNull())
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(EPTypePremade.LONGBOXED.getEPType(), getterTimestamp.eventBeanGetCodegen(cast(EventBean.EPTYPE, ref("event")), methodNode, codegenClassScope)));
        return localMethod(methodNode);
    }

    public EPTypeClass getEvaluationType() {
        return EPTypePremade.LONGBOXED.getEPType();
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence, ExprNodeRenderableFlags flags) {
        writer.append(this.getClass().getSimpleName());
    }
}
