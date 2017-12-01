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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotScalarCollection implements ExprEnumerationEval, ExprEnumerationForge, ExprEnumerationGivenEvent, ExprNodeRenderable {

    private static final Logger log = LoggerFactory.getLogger(PropertyDotScalarCollection.class);

    private final String propertyName;
    private final int streamId;
    private final EventPropertyGetterSPI getter;
    private final Class componentType;

    public PropertyDotScalarCollection(String propertyName, int streamId, EventPropertyGetterSPI getter, Class componentType) {
        this.propertyName = propertyName;
        this.streamId = streamId;
        this.getter = getter;
        this.componentType = componentType;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return evaluateInternal(eventsPerStream[streamId]);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Collection.class, PropertyDotScalarCollection.class, codegenClassScope);

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        methodNode.getBlock().methodReturn(codegenEvaluateInternal(arrayAtIndex(refEPS, constant(streamId)), codegenClassScope, methodNode));
        return localMethod(methodNode);
    }

    public Collection<EventBean> evaluateEventGetROCollectionEvents(EventBean event, ExprEvaluatorContext context) {
        return evaluateInternal(event);
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return componentType;
    }

    public Collection evaluateEventGetROCollectionScalar(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public EventBean evaluateEventGetEventBean(EventBean event, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
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

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }

    private Collection<EventBean> evaluateInternal(EventBean event) {
        Object result = getter.get(event);
        if (result == null) {
            return null;
        }
        if (!(result instanceof Collection)) {
            log.warn("Expected collection-type input from property '" + propertyName + "' but received " + result.getClass());
            return null;
        }
        return (Collection) getter.get(event);
    }

    private CodegenExpression codegenEvaluateInternal(CodegenExpression event, CodegenClassScope codegenClassScope, CodegenMethodScope codegenMethodScope) {
        CodegenMethodNode method = codegenMethodScope.makeChild(Collection.class, PropertyDotScalarCollection.class, codegenClassScope).addParam(EventBean.class, "event").getBlock()
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(Collection.class, getter.eventBeanGetCodegen(ref("event"), codegenMethodScope, codegenClassScope)));
        return localMethodBuild(method).pass(event).call();
    }
}
