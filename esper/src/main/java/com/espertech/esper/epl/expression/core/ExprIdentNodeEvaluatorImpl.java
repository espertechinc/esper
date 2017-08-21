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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.blocks.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.WrapperEventType;
import com.espertech.esper.event.vaevent.RevisionEventType;
import com.espertech.esper.event.vaevent.VariantEventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprIdentNodeEvaluatorImpl implements ExprIdentNodeEvaluator {
    private final int streamNum;
    private final EventPropertyGetterSPI propertyGetter;
    protected final Class returnType;
    private final ExprIdentNode identNode;
    private final EventType eventType;

    public ExprIdentNodeEvaluatorImpl(int streamNum, EventPropertyGetterSPI propertyGetter, Class returnType, ExprIdentNode identNode, EventType eventType) {
        this.streamNum = streamNum;
        this.propertyGetter = propertyGetter;
        this.returnType = returnType;
        this.identNode = identNode;
        this.eventType = eventType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprIdent(identNode.getFullUnresolvedName());
        }
        EventBean event = eventsPerStream[streamNum];
        if (event == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprIdent(null);
            }
            return null;
        }
        if (InstrumentationHelper.ENABLED) {
            Object result = propertyGetter.get(event);
            InstrumentationHelper.get().aExprIdent(result);
            return result;
        }

        return propertyGetter.get(event);
    }

    public CodegenExpression codegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (returnType == null) {
            return constantNull();
        }
        CodegenMethodNode method = codegenMethodScope.makeChild(returnType, this.getClass());

        if (exprSymbol.isAllowUnderlyingReferences() && !identNode.getResolvedPropertyName().contains("?") && !(eventType instanceof WrapperEventType) && !(eventType instanceof VariantEventType) && !(eventType instanceof RevisionEventType)) {
            CodegenExpressionRef underlying = exprSymbol.getAddRequiredUnderlying(method, streamNum, eventType);
            method.getBlock().ifRefNullReturnNull(underlying)
                    .methodReturn(CodegenLegoCast.castSafeFromObjectType(returnType, propertyGetter.underlyingGetCodegen(underlying, method, codegenClassScope)));
        } else {
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(method);
            method.getBlock().declareVar(EventBean.class, "event", arrayAtIndex(refEPS, constant(streamNum)))
                    .ifRefNullReturnNull("event")
                    .methodReturn(CodegenLegoCast.castSafeFromObjectType(returnType, propertyGetter.eventBeanGetCodegen(ref("event"), method, codegenClassScope)));

        }
        return localMethod(method);
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public EventPropertyGetterSPI getGetter() {
        return propertyGetter;
    }

    /**
     * Returns true if the property exists, or false if not.
     *
     * @param eventsPerStream each stream's events
     * @param isNewData       if the stream represents insert or remove stream
     * @return true if the property exists, false if not
     */
    public boolean evaluatePropertyExists(EventBean[] eventsPerStream, boolean isNewData) {
        EventBean theEvent = eventsPerStream[streamNum];
        if (theEvent == null) {
            return false;
        }
        return propertyGetter.isExistsProperty(theEvent);
    }

    public int getStreamNum() {
        return streamNum;
    }

    public boolean isContextEvaluated() {
        return false;
    }

}
