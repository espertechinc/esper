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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.core.WrapperEventType;
import com.espertech.esper.common.internal.event.variant.VariantEventType;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprIdentNodeEvaluatorImpl implements ExprIdentNodeEvaluator {
    private final int streamNum;
    private final EventPropertyGetterSPI propertyGetter;
    protected final Class returnType;
    private final ExprIdentNode identNode;
    private final EventTypeSPI eventType;
    private boolean optionalEvent;
    private boolean audit;

    public ExprIdentNodeEvaluatorImpl(int streamNum, EventPropertyGetterSPI propertyGetter, Class returnType, ExprIdentNode identNode, EventTypeSPI eventType, boolean optionalEvent, boolean audit) {
        this.streamNum = streamNum;
        this.propertyGetter = propertyGetter;
        this.returnType = returnType;
        this.identNode = identNode;
        this.eventType = eventType;
        this.optionalEvent = optionalEvent;
        this.audit = audit;
    }

    public void setOptionalEvent(boolean optionalEvent) {
        this.optionalEvent = optionalEvent;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean event = eventsPerStream[streamNum];
        if (event == null) {
            return null;
        }
        return propertyGetter.get(event);
    }

    public Class getCodegenReturnType(Class requiredType) {
        return requiredType == Object.class ? Object.class : returnType;
    }

    public CodegenExpression codegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {

        if (!audit) {
            return codegenGet(requiredType, parent, symbols, classScope);
        }

        Class targetType = getCodegenReturnType(requiredType);
        CodegenMethod method = parent.makeChild(targetType, this.getClass(), classScope);
        method.getBlock()
                .declareVar(targetType, "value", codegenGet(requiredType, method, symbols, classScope))
                .expression(exprDotMethodChain(symbols.getAddExprEvalCtx(method)).add("getAuditProvider").add("property", constant(identNode.getResolvedPropertyName()), ref("value"), symbols.getAddExprEvalCtx(method)))
                .methodReturn(ref("value"));
        return localMethod(method);
    }

    private CodegenExpression codegenGet(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {

        if (returnType == null) {
            return constantNull();
        }

        Class castTargetType = getCodegenReturnType(requiredType);
        boolean useUnderlying = exprSymbol.isAllowUnderlyingReferences() && !identNode.getResolvedPropertyName().contains("?") && !(eventType instanceof WrapperEventType) && !(eventType instanceof VariantEventType);
        if (useUnderlying && !optionalEvent) {
            CodegenExpressionRef underlying = exprSymbol.getAddRequiredUnderlying(codegenMethodScope, streamNum, eventType, false);
            return CodegenLegoCast.castSafeFromObjectType(castTargetType, propertyGetter.underlyingGetCodegen(underlying, codegenMethodScope, codegenClassScope));
        }

        CodegenMethod method = codegenMethodScope.makeChild(castTargetType, this.getClass(), codegenClassScope);
        CodegenBlock block = method.getBlock();

        if (useUnderlying) {
            CodegenExpressionRef underlying = exprSymbol.getAddRequiredUnderlying(method, streamNum, eventType, true);
            block.ifNullReturnNull(underlying)
                    .methodReturn(CodegenLegoCast.castSafeFromObjectType(castTargetType, propertyGetter.underlyingGetCodegen(underlying, method, codegenClassScope)));
        } else {
            CodegenExpressionRef refEPS = exprSymbol.getAddEPS(method);
            method.getBlock().declareVar(EventBean.class, "event", arrayAtIndex(refEPS, constant(streamNum)));
            if (optionalEvent) {
                block.ifRefNullReturnNull("event");
            }
            block.methodReturn(CodegenLegoCast.castSafeFromObjectType(castTargetType, propertyGetter.eventBeanGetCodegen(ref("event"), method, codegenClassScope)));

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

    public EventTypeSPI getEventType() {
        return eventType;
    }
}
