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
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotScalarIterable implements ExprEnumerationForge, ExprEnumerationEval, ExprEnumerationGivenEvent, ExprNodeRenderable {
    private static final Logger log = LoggerFactory.getLogger(PropertyDotScalarIterable.class);

    private final String propertyName;
    private final int streamId;
    private final EventPropertyGetterSPI getter;
    private final Class componentType;
    private final Class getterReturnType;

    public PropertyDotScalarIterable(String propertyName, int streamId, EventPropertyGetterSPI getter, Class componentType, Class getterReturnType) {
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
        return evaluateInternal(eventsPerStream[streamId]);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(codegenMethodScope);
        return codegenEvaluateInternal(arrayAtIndex(refEPS, constant(streamId)), codegenMethodScope, codegenClassScope);
    }

    public Collection evaluateEventGetROCollectionScalar(EventBean event, ExprEvaluatorContext context) {
        return evaluateInternal(event);
    }

    private Collection evaluateInternal(EventBean event) {
        Object result = getter.get(event);
        if (result == null) {
            return null;
        }
        if (result instanceof Collection) {
            return (Collection) result;
        }
        if (!(result instanceof Iterable)) {
            log.warn("Expected iterable-type input from property '" + propertyName + "' but received " + result.getClass());
            return null;
        }
        return CollectionUtil.iterableToCollection((Iterable) result);
    }

    private CodegenExpression codegenEvaluateInternal(CodegenExpression event, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (JavaClassHelper.isImplementsInterface(getterReturnType, Collection.class)) {
            return getter.eventBeanGetCodegen(event, codegenMethodScope, codegenClassScope);
        }
        CodegenMethodNode method = codegenMethodScope.makeChild(Collection.class, PropertyDotScalarIterable.class, codegenClassScope).addParam(EventBean.class, "event").getBlock()
                .declareVar(getterReturnType, "result", CodegenLegoCast.castSafeFromObjectType(Iterable.class, getter.eventBeanGetCodegen(ref("event"), codegenMethodScope, codegenClassScope)))
                .ifRefNullReturnNull("result")
                .methodReturn(staticMethod(CollectionUtil.class, "iterableToCollection", ref("result")));
        return localMethodBuild(method).pass(event).call();
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) {
        return null;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return componentType;
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

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
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
}
