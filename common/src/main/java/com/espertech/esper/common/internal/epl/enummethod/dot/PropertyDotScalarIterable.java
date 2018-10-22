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
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotScalarIterable implements ExprEnumerationForge, ExprEnumerationEval, ExprEnumerationGivenEvent, ExprEnumerationGivenEventForge, ExprNodeRenderable {
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

    public CodegenExpression evaluateEventGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprEnumerationGivenEventSymbol symbols, CodegenClassScope codegenClassScope) {
        if (JavaClassHelper.isImplementsInterface(getterReturnType, Collection.class)) {
            return getter.eventBeanGetCodegen(symbols.getAddEvent(codegenMethodScope), codegenMethodScope, codegenClassScope);
        }
        CodegenMethod method = codegenMethodScope.makeChild(Collection.class, PropertyDotScalarIterable.class, codegenClassScope);
        method.getBlock().declareVar(getterReturnType, "result", CodegenLegoCast.castSafeFromObjectType(Iterable.class, getter.eventBeanGetCodegen(symbols.getAddEvent(method), codegenMethodScope, codegenClassScope)))
                .ifRefNullReturnNull("result")
                .methodReturn(staticMethod(CollectionUtil.class, "iterableToCollection", ref("result")));
        return localMethod(method);
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
        CodegenMethod method = codegenMethodScope.makeChild(Collection.class, PropertyDotScalarIterable.class, codegenClassScope).addParam(EventBean.class, "event").getBlock()
                .declareVar(getterReturnType, "result", CodegenLegoCast.castSafeFromObjectType(Iterable.class, getter.eventBeanGetCodegen(ref("event"), codegenMethodScope, codegenClassScope)))
                .ifRefNullReturnNull("result")
                .methodReturn(staticMethod(CollectionUtil.class, "iterableToCollection", ref("result")));
        return localMethodBuild(method).pass(event).call();
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        return null;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return componentType;
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
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

    public CodegenExpression evaluateEventGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprEnumerationGivenEventSymbol symbols, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }

    public CodegenExpression evaluateEventGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprEnumerationGivenEventSymbol symbols, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
