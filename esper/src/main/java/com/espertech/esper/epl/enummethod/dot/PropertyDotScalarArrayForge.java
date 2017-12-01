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
package com.espertech.esper.epl.enummethod.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotScalarArrayForge implements ExprEnumerationEval, ExprEnumerationForge, ExprEnumerationGivenEvent, ExprNodeRenderable {

    private static final Logger log = LoggerFactory.getLogger(PropertyDotScalarArrayForge.class);

    private final String propertyName;
    private final int streamId;
    private final EventPropertyGetterSPI getter;
    private final Class componentType;
    private final Class getterReturnType;

    public PropertyDotScalarArrayForge(String propertyName, int streamId, EventPropertyGetterSPI getter, Class componentType, Class getterReturnType) {
        this.propertyName = propertyName;
        this.streamId = streamId;
        this.getter = getter;
        this.componentType = componentType;
        this.getterReturnType = getterReturnType;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean eventInQuestion = eventsPerStream[streamId];
        return evaluateEventGetROCollectionScalar(eventInQuestion, context);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(codegenMethodScope);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(codegenMethodScope);
        return codegenEvaluateEventGetROCollectionScalar(arrayAtIndex(refEPS, constant(streamId)), refExprEvalCtx, codegenMethodScope, codegenClassScope);
    }

    public Collection evaluateEventGetROCollectionScalar(EventBean event, ExprEvaluatorContext context) {
        if (event == null) {
            return null;
        }
        return evaluateGetInternal(event);
    }

    public CodegenExpression codegenEvaluateEventGetROCollectionScalar(CodegenExpression event, CodegenExpression evalctx, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethodNode method = codegenMethodScope.makeChild(Collection.class, PropertyDotScalarArrayForge.class, codegenClassScope).addParam(EventBean.class, "event").addParam(ExprEvaluatorContext.class, "context").getBlock()
                .ifRefNullReturnNull("event")
                .methodReturn(codegenEvaluateGetInternal(ref("event"), codegenMethodScope, codegenClassScope));
        return localMethodBuild(method).pass(event).pass(evalctx).call();
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return componentType;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) {
        return null;
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public Collection<EventBean> evaluateEventGetROCollectionEvents(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public EventBean evaluateEventGetEventBean(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    private Collection evaluateGetInternal(EventBean event) {
        Object value = getter.get(event);
        if (value == null) {
            return null;
        }
        if (!(value.getClass().isArray())) {
            log.warn("Expected array-type input from property '" + propertyName + "' but received " + value.getClass());
            return null;
        }
        if (componentType.isPrimitive()) {
            return new ArrayWrappingCollection(value);
        }
        return Arrays.asList((Object[]) value);
    }

    private CodegenExpression codegenEvaluateGetInternal(CodegenExpression event, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(Collection.class, PropertyDotScalarArrayForge.class, codegenClassScope).addParam(EventBean.class, "event").getBlock()
                .declareVar(getterReturnType, "value", CodegenLegoCast.castSafeFromObjectType(getterReturnType, getter.eventBeanGetCodegen(ref("event"), codegenMethodScope, codegenClassScope)))
                .ifRefNullReturnNull("value");
        CodegenMethodNode method;
        if (componentType.isPrimitive()) {
            method = block.methodReturn(newInstance(ArrayWrappingCollection.class, ref("value")));
        } else {
            method = block.methodReturn(staticMethod(Arrays.class, "asList", ref("value")));
        }
        return localMethodBuild(method).pass(event).call();
    }
}
