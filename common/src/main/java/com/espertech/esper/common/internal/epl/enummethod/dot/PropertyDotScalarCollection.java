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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotScalarCollection implements ExprEnumerationEval, ExprEnumerationForge, ExprEnumerationGivenEvent, ExprEnumerationGivenEventForge, ExprNodeRenderable {

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

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return evaluateInternal(eventsPerStream[streamId]);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(Collection.class, PropertyDotScalarCollection.class, codegenClassScope);
        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        methodNode.getBlock().methodReturn(codegenEvaluateInternal(arrayAtIndex(refEPS, constant(streamId)), codegenClassScope, methodNode));
        return localMethod(methodNode);
    }

    public CodegenExpression evaluateEventGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprEnumerationGivenEventSymbol symbols, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(Collection.class, PropertyDotScalarCollection.class, codegenClassScope);
        methodNode.getBlock().methodReturn(CodegenLegoCast.castSafeFromObjectType(Collection.class, getter.eventBeanGetCodegen(symbols.getAddEvent(methodNode), codegenMethodScope, codegenClassScope)));
        return localMethod(methodNode);
    }

    public Collection<EventBean> evaluateEventGetROCollectionEvents(EventBean event, ExprEvaluatorContext context) {
        return evaluateInternal(event);
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return componentType;
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        return null;
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
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

    public CodegenExpression evaluateEventGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprEnumerationGivenEventSymbol symbols, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression evaluateEventGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprEnumerationGivenEventSymbol symbols, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
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
        CodegenMethod method = codegenMethodScope.makeChild(Collection.class, PropertyDotScalarCollection.class, codegenClassScope).addParam(EventBean.class, "event").getBlock()
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(Collection.class, getter.eventBeanGetCodegen(ref("event"), codegenMethodScope, codegenClassScope)));
        return localMethodBuild(method).pass(event).call();
    }
}
